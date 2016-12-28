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
import com.google.gerrit.extensions.api.projects.ProjectConfigEntryType;
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
import java.util.HashSet;
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
                null, ProjectConfigEntryType.BOOLEAN, null, false,
                "Pushes of commits that contain duplicate pathnames, or that "
                    + "contain duplicates of existing pathnames will be "
                    + "rejected. Pathnames y and z are considered to be "
                    + "duplicates if they are equal, case-insensitive."));
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_REJECT_DUPLICATE_PATHNAMES_LOCALE))
            .toInstance(new ProjectConfigEntry("Reject Duplicate Pathnames Locale",
                "en", ProjectConfigEntryType.STRING, getAvailableLocales(), false,
                "To avoid problems caused by comparing pathnames with different "
                    + "locales it is possible to use a specific locale. The "
                    + "default is English (en)."));
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
  Map<String, String> allPaths(Collection<String> leafs) {
    Map<String, String> paths = new HashMap<>();
    for (String cp : leafs) {
      int n = cp.indexOf('/');
      while (n > -1) {
        String s = cp.substring(0, n);
        paths.put(s.toLowerCase(locale), s);
        n = cp.indexOf('/', n + 1);
      }
      paths.put(cp.toLowerCase(locale), cp);
    }
    return paths;
  }

  Set<String> allParentFolders(Collection<String> paths) {
    Set<String> folders = new HashSet<>();
    for (String cp : paths) {
      int n = cp.indexOf('/');
      while (n > -1) {
        String s = cp.substring(0, n);
        folders.add(s);
        n = cp.indexOf('/', n + 1);
      }
    }
    return folders;
  }

  @VisibleForTesting
  static CommitValidationMessage conflict(String f1, String f2) {
    return new CommitValidationMessage(f1 + ": pathname conflicts with " + f2,
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
          .getFromProjectConfigWithInheritance(
              receiveEvent.project.getNameKey(), pluginName);
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
          "failed to check for duplicate pathnames", e);
    }
    return Collections.emptyList();
  }

  @VisibleForTesting
  List<CommitValidationMessage> performValidation(Repository repo, RevCommit c)
      throws IOException {
    List<CommitValidationMessage> messages = new LinkedList<>();

    Set<String> pathnames = CommitUtils.getChangedPaths(repo, c);
    checkForDuplicatesInSet(pathnames, messages);
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
      Set<String> changed, List<CommitValidationMessage> messages)
          throws IOException {
    Map<String, String> all = allPaths(changed);

    while (tw.next()) {
      String currentPath = tw.getPathString();

      if (isDeleted(tw)) {
        continue;
      }

      String potentialDuplicate = all.get(currentPath.toLowerCase(locale));
      if (potentialDuplicate == null) {
        continue;
      } else if (potentialDuplicate.equals(currentPath)) {
        if (tw.isSubtree()) {
          tw.enterSubtree();
        }
        continue;
      } else {
        messages.add(conflict(potentialDuplicate, currentPath));
      }
    }
  }

  private void checkForDuplicatesInSet(Set<String> files,
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
}
