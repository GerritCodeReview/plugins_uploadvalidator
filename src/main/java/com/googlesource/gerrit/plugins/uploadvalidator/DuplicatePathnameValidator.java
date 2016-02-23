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

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DuplicatePathnameValidator implements CommitValidationListener {

  public static AbstractModule module() {
    return new AbstractModule() {
      private List<String> getAvailableLocales() {
        return Lists.transform(
            Arrays.asList(Locale.getAvailableLocales()),
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
            .toInstance(new ProjectConfigEntry("Reject Duplicate Pathnames Locale",
                "en", ProjectConfigEntry.Type.STRING, getAvailableLocales(), false,
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
  static List<CommitValidationMessage> performValidation(Repository repo,
      RevCommit c, Locale locale) throws IOException {
    List<CommitValidationMessage> messages = new LinkedList<>();

    Set<String> pathnames = CommitUtils.getChangedPaths(repo, c);
    checkForDuplicatesWithinThisSet(pathnames, messages, locale);
    if (!messages.isEmpty() || c.getParentCount() == 0) {
      return messages;
    }

    Map<String, String> changed = new HashMap<>();
    for (String p : pathnames) {
      changed.put(p.toLowerCase(), p);
    }

    try (TreeWalk tw = new TreeWalk(repo)) {
      CaseInsensitiveFilter f = new CaseInsensitiveFilter(pathnames, locale);
      tw.setFilter(f);
      tw.setRecursive(f.shouldBeRecursive());
      tw.addTree(c.getTree());
      while (tw.next()) {
        checkForDuplicates(tw, changed, messages, locale);
      }
    }
    return messages;
  }

  private static void checkForDuplicatesWithinThisSet(Set<String> files,
      List<CommitValidationMessage> messages, Locale locale) {
    Map<String, String> seen = new HashMap<>();
    for (String file : files) {
      String lc = file.toLowerCase(locale);
      String duplicate = seen.get(lc);
      if (duplicate != null) {
        messages.add(new CommitValidationMessage(
            file + ": pathname collides with " + duplicate, true));
      } else {
        seen.put(lc, file);
      }
    }
  }

  private static void checkForDuplicates(TreeWalk tw, Map<String, String> changed,
      List<CommitValidationMessage> messages, Locale locale) throws IOException {
    if (tw.isSubtree()) {
      tw.enterSubtree();
    } else {
      String path = tw.getPathString();
      String duplicate = changed.get(path.toLowerCase(locale));
      if (duplicate != null && !duplicate.equals(path)) {
        messages.add(new CommitValidationMessage(
            duplicate + ": pathname collides with " + path, true));
      }
    }
  }

  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final GitRepositoryManager repoManager;

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
      try (Repository repo =
          repoManager.openRepository(receiveEvent.project.getNameKey())) {
        List<CommitValidationMessage> messages =
            performValidation(repo, receiveEvent.commit, getLocale(cfg));
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
}
