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

import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SymlinkValidator implements CommitValidationListener {

  public static AbstractModule module() {
    return new AbstractModule() {

      @Override
      protected void configure() {
        DynamicSet.bind(binder(), CommitValidationListener.class)
            .to(SymlinkValidator.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_CHECK_SYMLINK))
            .toInstance(new ProjectConfigEntry("Reject Symbolic Links", "false",
                ProjectConfigEntry.Type.BOOLEAN, null, false,
                "Symbolic Links. Pushes of commits that include symbolic "
                    + "links will be rejected."));
      }
    };
  }

  public static String KEY_CHECK_SYMLINK = "rejectSymlink";

  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final GitRepositoryManager repoManager;

  @Inject
  SymlinkValidator(@PluginName String pluginName,
      PluginConfigFactory cfgFactory, GitRepositoryManager repoManager) {
    this.pluginName = pluginName;
    this.cfgFactory = cfgFactory;
    this.repoManager = repoManager;
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(
      CommitReceivedEvent receiveEvent) throws CommitValidationException {
    try {
      PluginConfig cfg =
          cfgFactory.getFromProjectConfig(
              receiveEvent.project.getNameKey(), pluginName);
      boolean rejectSymlink = cfg.getBoolean(KEY_CHECK_SYMLINK, false);
      if(!rejectSymlink) {
        return Collections.emptyList();
      }
      try (Repository repo =
          repoManager.openRepository(receiveEvent.project.getNameKey())) {
        List<CommitValidationMessage> messages =
            performValidation(repo, receiveEvent.commit);
        if (!messages.isEmpty()) {
          throw new CommitValidationException("contains symbolic links",
              messages);
        }
      }
    } catch (NoSuchProjectException | IOException e) {
      throw new CommitValidationException(
          "failed to check on symbolic links", e);
    }
    return Collections.emptyList();
  }

  private static void addValidationMessage(
      List<CommitValidationMessage> messages, TreeWalk tw) {
    messages.add(new CommitValidationMessage(
        "Symbolic links are not allowed: "
        + tw.getPathString(), true));
  }

  private static boolean isSymLink(int rawMode) {
    return (rawMode & FileMode.TYPE_MASK) == FileMode.TYPE_SYMLINK;
  }

  List<CommitValidationMessage> performValidation(Repository repo, RevCommit c)
      throws IOException {
    try (TreeWalk tw = new TreeWalk(repo)) {
      final List<CommitValidationMessage> messages = new LinkedList<>();

      CommitUtils.runOnChangedTreeEntry(repo, c, new TreeWalkListener() {
        @Override
        public void onEnterEntry(TreeWalk tw) {
          if (isSymLink(tw.getRawMode(0))) {
            addValidationMessage(messages, tw);
          }
        }
      });
      return messages;
    }
  }
}
