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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SymlinkValidator implements CommitValidationListener {
  public static String KEY_CHECK_SYMLINK = "symlink";

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
      boolean symlinkCheck = cfg.getBoolean(KEY_CHECK_SYMLINK, false);

      if (symlinkCheck) {
        try (Repository repo = repoManager.openRepository(
            receiveEvent.project.getNameKey())) {
          List<CommitValidationMessage> messages = new LinkedList<>();
          TreeWalk tw = new TreeWalk(repo);
          tw.setRecursive(true);
          tw.setFilter(TreeFilter.ANY_DIFF);
          if (receiveEvent.commit.getParentCount() > 1) {
            List<RevTree> trees = new ArrayList<>();
            trees.add(receiveEvent.commit.getTree());
            for (RevCommit p : receiveEvent.commit.getParents()) {
              trees.add(p.getTree());
            }
            tw.reset(trees.toArray(new AnyObjectId[trees.size()]));
            while (tw.next()) {
              boolean diff = true;
              for (int p=1; p < trees.size(); p++) {
                if (tw.getObjectId(0).equals(tw.getObjectId(p))) {
                  diff = false;
                }
              }
              if (((tw.getRawMode(0) & FileMode.TYPE_MASK) == FileMode.TYPE_SYMLINK)
                  && diff) {
                messages.add(new CommitValidationMessage(
                    "Symbolic links are not allowed: "
                    + tw.getPathString(), true));
              }
            }
            tw.close();
          } else {
            tw.reset(receiveEvent.commit.getTree());
            while(tw.next()) {
              if ((tw.getRawMode(0) & FileMode.TYPE_MASK) == FileMode.TYPE_SYMLINK)
                messages.add(new CommitValidationMessage(
                    "Symbolic links are not allowed: "
                    + tw.getPathString(), true));
            }
          }
          if (!messages.isEmpty()) {
            throw new CommitValidationException(
                "contains symbolic links", messages);
          }
        }
      }
    } catch (NoSuchProjectException | IOException e) {
      throw new CommitValidationException(
          "failed to check on symbolic links", e);
    }

    return Collections.emptyList();
  }
}
