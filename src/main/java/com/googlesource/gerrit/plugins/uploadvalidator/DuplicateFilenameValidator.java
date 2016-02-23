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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DuplicateFilenameValidator
    implements CommitValidationListener, Validator {
  public static String KEY_ALLOW_DUPLICATE_FILENAMES =
      "allowDuplicateFilenames";

  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final GitRepositoryManager repoManager;

  @Inject
  DuplicateFilenameValidator(@PluginName String pluginName,
      PluginConfigFactory cfgFactory, GitRepositoryManager repoManager) {
    this.pluginName = pluginName;
    this.cfgFactory = cfgFactory;
    this.repoManager = repoManager;
  }

  private boolean areDuplicateFilenamesAllowed(CommitReceivedEvent receiveEvent)
      throws NoSuchProjectException {
    PluginConfig cfg = cfgFactory
        .getFromProjectConfig(receiveEvent.project.getNameKey(), pluginName);
    return cfg.getBoolean(KEY_ALLOW_DUPLICATE_FILENAMES, false);
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived (
      CommitReceivedEvent receiveEvent) throws CommitValidationException {
    try {
      if (areDuplicateFilenamesAllowed(receiveEvent)) {
        return Collections.emptyList();
      }
      try (Repository repo =
          repoManager.openRepository(receiveEvent.project.getNameKey())) {
        List<CommitValidationMessage> messages =
            performValidation(repo, receiveEvent.commit, null);
        if (!messages.isEmpty()) {
          throw new CommitValidationException("contains duplicate filenames",
              messages);
        }
      }
    } catch (NoSuchProjectException | IOException e) {
      throw new CommitValidationException(
          "failed to check on duplicate filenames", e);
    }
    return Collections.emptyList();
  }

  @Override
  public List<CommitValidationMessage> performValidation(Repository repo,
      RevCommit c, PluginConfig cfg) throws IOException {
    List<CommitValidationMessage> messages = new LinkedList<>();
    Set<String> files = CommitUtils.getChangedPaths(repo, c);
    checkForDuplicatesWithinThisCommit(files, messages);
    if(messages.isEmpty()) {
      // Don't do this every time because it is expensive
      // check against the whole tree
      try (TreeWalk tw = new TreeWalk(repo)) {
        CaseInsensitiveFilter f = new CaseInsensitiveFilter(files);
        tw.setFilter(f);
        tw.setRecursive(f.shouldBeRecursive());
        if (c.getParentCount() > 0) {
          for (RevCommit p : c.getParents()) {
            tw.addTree(p.getTree());
          }
          while (tw.next()) {
            handleTreeWalkEntry(tw, files, messages);
          }
        }
      }
    }
    return messages;
  }

  private void handleTreeWalkEntry(TreeWalk tw, Set<String> files,
      List<CommitValidationMessage> messages) throws IOException {
    if (tw.isSubtree()) {
      tw.enterSubtree();
    } else {
      String path = tw.getPathString();
      for (String file : files) {
        if (file.equalsIgnoreCase(path) && !file.equals(path)) {
          messages.add(new CommitValidationMessage(
              file + ": filename collides with " + path, true));
        }
      }
    }
  }

  private void checkForDuplicatesWithinThisCommit(
      Set<String> files, List<CommitValidationMessage> messages) {
    Map<String, String> paths = new HashMap<>();
    for (String file : files) {
      if (paths.containsKey(file.toLowerCase(Locale.ENGLISH))) {
        messages.add(new CommitValidationMessage(file
            + ": filename collides with "
            + paths.get(file.toLowerCase(Locale.ENGLISH)), true));
      } else {
        paths.put(file.toLowerCase(Locale.ENGLISH), file);
      }
    }
  }

  public static AbstractModule module() {
    return new AbstractModule() {

      @Override
      protected void configure() {
        DynamicSet.bind(binder(), CommitValidationListener.class)
            .to(DuplicateFilenameValidator.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_ALLOW_DUPLICATE_FILENAMES))
            .toInstance(new ProjectConfigEntry("Allow Duplicate Filenames",
                null, ProjectConfigEntry.Type.BOOLEAN, null, false,
                "Pushes of commits that contain duplicate filenames, or that "
                    + "contain duplicates of existing file names will be "
                    + "rejected. If y.equalsIgnoreCase(z) == true, then 'y' "
                    + "and 'z' are duplicates."));
      }
    };
  }
}

