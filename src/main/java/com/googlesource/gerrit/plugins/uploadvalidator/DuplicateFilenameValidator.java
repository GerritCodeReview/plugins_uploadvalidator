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
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;


import org.eclipse.jgit.api.errors.GitAPIException;

import org.eclipse.jgit.lib.ObjectId;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DuplicateFilenameValidator extends PathValidator {
  public static String KEY_ALLOW_DUPLICATE_FILENAMES = "allowDuplicateFilenames";

  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final GitRepositoryManager repoManager;
  private static final Comparator<String> caseInsensitiveCompare =
      new Comparator<String>() {
    @Override
    public int compare(final String o1, final String o2) {
      return o1.compareToIgnoreCase(o2);
    }
  };

  @Inject
  DuplicateFilenameValidator(@PluginName String pluginName,
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
      boolean allow = cfg.getBoolean(KEY_ALLOW_DUPLICATE_FILENAMES, false);
      if (!allow) {
        try (Repository repo = repoManager.openRepository(
            receiveEvent.project.getNameKey())) {
          List<CommitValidationMessage> messages = new LinkedList<>();
          List<String> files = getFiles(repo, receiveEvent.commit);
          // check inside of the commit
          Map<String, String> paths = new HashMap<>();
          for (String file : files) {
            if (paths.containsKey(file.toLowerCase())) {
              messages.add(new CommitValidationMessage(file
                  + ": filename collides with "
                  + paths.get(file.toLowerCase()), true));
            } else {
              paths.put(file.toLowerCase(), file);
            }
          }
          // check against the whole tree
          Collections.sort(files, caseInsensitiveCompare);
          try (TreeWalk tw = new TreeWalk(repo)) {
            tw.setRecursive(true);
            if (receiveEvent.commit.getParentCount() > 0) {
              for (RevCommit p : receiveEvent.commit.getParents()) {
                tw.addTree(p.getTree());
              }
              while (tw.next() && !files.isEmpty()) {
                String path = tw.getPathString();
                int i = Collections.binarySearch(files, path, caseInsensitiveCompare);
                if (i > 0) {
                  if (path.equals(files.get(i))) {
                    files.remove(i);
                  } else {
                    // Error
                  }
                }
              }
            }
          }
          if (!messages.isEmpty()) {
            throw new CommitValidationException(
                "contains duplicate filenames", messages);
          }
        }
      }
    } catch (NoSuchProjectException | IOException | GitAPIException e) {
      throw new CommitValidationException(
          "failed to check on duplicate filenames", e);
    }

    return Collections.emptyList();
  }
}
