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

import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class InvalidFilenameValidator implements CommitValidationListener {
  public static String KEY_INVALID_FILENAME_PATTERN = "invalidFilenamePattern";

  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final GitRepositoryManager repoManager;

  @Inject
  InvalidFilenameValidator(@PluginName String pluginName,
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
      List<Pattern> invalidFilenamePatterns = new ArrayList<>();
      for (String s : cfg.getStringList(KEY_INVALID_FILENAME_PATTERN)) {
        invalidFilenamePatterns.add(Pattern.compile(s));
      }
      if (invalidFilenamePatterns.size() > 0) {
        try (Repository repo = repoManager.openRepository(
            receiveEvent.project.getNameKey())) {
          List<CommitValidationMessage> messages = new LinkedList<>();
          for (String file : CommitUtils.getChangedPaths(
              repo, receiveEvent.commit)) {
            for (Pattern p : invalidFilenamePatterns) {
              if (p.matcher(file).find()) {
                messages.add(new CommitValidationMessage(
                    "invalid characters found in filename: " + file, true));
                break;
              }
            }
          }
          if (!messages.isEmpty()) {
            throw new CommitValidationException(
                "contains files with an invalid filename", messages);
          }
        }
      }
    } catch (NoSuchProjectException | IOException e) {
      throw new CommitValidationException(
          "failed to check on invalid file names", e);
    }
    return Collections.emptyList();
  }
}
