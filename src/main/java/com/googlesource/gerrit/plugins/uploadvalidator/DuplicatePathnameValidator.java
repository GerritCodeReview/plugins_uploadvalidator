// Copyright (C) 2016 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.uploadvalidator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.ProjectConfigEntry;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;

import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DuplicatePathnameValidator implements CommitValidationListener {

  public static AbstractModule module() {
    return new AbstractModule() {
      private List<String> getAvailableLocales() {
        return Lists.transform(Arrays.asList(Locale.getAvailableLocales()),
            new Function<Locale, String>() {
          @Override
          public String apply(Locale input) {
            return input.toString();
          }
        });
      }

      @Override
      protected void configure() {
        DynamicSet.bind(binder(), CommitValidationListener.class)
            .to(DuplicatePathnameValidator.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_REJECT_DUPLICATE_PATHNAMES))
            .toInstance(new ProjectConfigEntry("Reject Duplicate Pathnames",
                null, ProjectConfigEntry.Type.BOOLEAN, null, false,
                "Pushes of commits that contain duplicate pathnames, or that "
                    + "contain duplicates of existing pathnames will be "
                    + "rejected. Pathnames y and z are considered to be "
                    + "duplicatesif they are equal, case-insensitive."));
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_REJECT_DUPLICATE_PATHNAMES_LOCALE))
            .toInstance(new ProjectConfigEntry(
                "Reject Duplicate Pathnames Locale", "en",
                ProjectConfigEntry.Type.STRING, getAvailableLocales(), false,
                "To avoid problems with different locales by comparing pathnames "
                    + "it is possible to use a specific locale. The default is "
                    + "English (en)."));
      }
    };
  }

  public static String KEY_REJECT_DUPLICATE_PATHNAMES =
      "rejectDuplicatePathnames";
  public static String KEY_REJECT_DUPLICATE_PATHNAMES_LOCALE =
      "rejectDuplicatePathnamesLocale";

  @VisibleForTesting
  static boolean isActive(PluginConfig cfg) {
    return cfg.getBoolean(KEY_REJECT_DUPLICATE_PATHNAMES, false);
  }

  @VisibleForTesting
  static Locale getLocale(PluginConfig cfg) {
    return Locale.forLanguageTag(
        cfg.getString(KEY_REJECT_DUPLICATE_PATHNAMES_LOCALE, "en"));
  }

  @VisibleForTesting
  static Set<String> allParentFolders(Collection<String> paths) {
    Set<String> folders = Sets.newHashSet();
    for (String cp : paths) {
      int n = cp.indexOf('/');
      while (n > -1) {
        folders.add(cp.substring(0, n));
        n = cp.indexOf('/', n + 1);
      }
    }
    return folders;
  }

  @VisibleForTesting
  static CommitValidationMessage conflict(String f1, String f2) {
    return new CommitValidationMessage(f1 + ": pathname collides with " + f2,
        true);
  }

  private static boolean isDeleted(TreeWalk tw) {
    return FileMode.MISSING.equals(tw.getRawMode(0));
  }

  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final GitRepositoryManager repoManager;

  private Locale locale;

  @VisibleForTesting
  void setLocale(Locale locale) {
    this.locale = locale;
  }

  @Inject
  DuplicatePathnameValidator(@PluginName String pluginName,
      PluginConfigFactory cfgFactory, GitRepositoryManager repoManager) {
    this.pluginName = pluginName;
    this.cfgFactory = cfgFactory;
    this.repoManager = repoManager;
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(
      CommitReceivedEvent receiveEvent) throws CommitValidationException {
    try {
      PluginConfig cfg = cfgFactory
          .getFromProjectConfig(receiveEvent.project.getNameKey(), pluginName);
      if (!isActive(cfg)) {
        return Collections.emptyList();
      }
      locale = getLocale(cfg);
      try (Repository repo =
          repoManager.openRepository(receiveEvent.project.getNameKey())) {
        List<CommitValidationMessage> messages =
            performValidation(repo, receiveEvent.commit);
        if (!messages.isEmpty()) {
          throw new CommitValidationException("contains duplicate pathnames",
              messages);
        }
      }
    } catch (NoSuchProjectException | IOException e) {
      throw new CommitValidationException(
          "failed to check on duplicate pathnames", e);
    }
    return Collections.emptyList();
  }

  @VisibleForTesting
  List<CommitValidationMessage> performValidation(Repository repo, RevCommit c)
      throws IOException {
    List<CommitValidationMessage> messages = new LinkedList<>();

    Set<String> pathnames = CommitUtils.getChangedPaths(repo, c);
    checkForDuplicatesWithinThisSet(pathnames, messages);
    if (!messages.isEmpty() || c.getParentCount() == 0) {
      return messages;
    }

    try (TreeWalk tw = new TreeWalk(repo)) {
      tw.setRecursive(false);
      tw.addTree(c.getTree());
      checkForDuplicatesAgainstTheWholeTree(tw, pathnames, messages);
    }
    return messages;
  }

  @VisibleForTesting
  void checkForDuplicatesAgainstTheWholeTree(TreeWalk tw,
      Collection<String> changed, List<CommitValidationMessage> messages)
          throws IOException {
    List<String> changedPaths = Lists.newArrayList(changed);
    Set<String> folders = allParentFolders(changed);
    while (tw.next()) {
      if (isDeleted(tw)) {
        // If this object is deleted in current commit, it is not necessary to
        // check it
        continue;
      }
      Iterator<String> iterator = changedPaths.iterator();
      String currentPath = tw.getPathString();
      while (iterator.hasNext()) {
        String changedPath = iterator.next();
        if (currentPath.length() <= changedPath.length()) {
          String changedPathprefix =
              changedPath.substring(0, currentPath.length());
          if (equalsIgnoreCase(currentPath, changedPathprefix)
              && !changedPath.equals(currentPath)) {
            if (tw.isSubtree()) {
              if (folders.contains(currentPath)) {
                tw.enterSubtree();
                break;
              } else {
                // Folder conflict
                iterator.remove();
                messages.add(conflict(changedPathprefix, currentPath));
                break;
              }
            } else if (changedPath.length() == currentPath.length()) {
              // If there are no duplicates in the tree, then there exist at
              // most
              // one duplicate for each changedPath. If we found one, this
              // changedPath is not longer relevant
              // The length of the paths must be equal, otherwise it cannot be a
              // duplicate
              iterator.remove();
              messages.add(conflict(changedPath, currentPath));
              break;
            }
          }
        }
      }
    }
  }

  private void checkForDuplicatesWithinThisSet(Set<String> files,
      List<CommitValidationMessage> messages) {
    Set<String> filesAndFolders = Sets.newHashSet(files);
    filesAndFolders.addAll(allParentFolders(files));
    Map<String, String> seen = new HashMap<>();
    for (String file : filesAndFolders) {
      String lc = file.toLowerCase(locale);
      String duplicate = seen.get(lc);
      if (duplicate != null) {
        messages.add(conflict(duplicate, file));
      } else {
        seen.put(lc, file);
      }
    }
  }

  private boolean equalsIgnoreCase(String s1, String s2) {
    return s1.toLowerCase(locale).equals(s2.toLowerCase(locale));
  }
}
