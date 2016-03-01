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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class InvalidFilenameValidator implements CommitValidationListener, Validator {
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
      if (cfg.getStringList(KEY_INVALID_FILENAME_PATTERN).length == 0) {
        return Collections.emptyList();
      }
      try (Repository repo = repoManager.openRepository(
          receiveEvent.project.getNameKey())) {
        List<CommitValidationMessage> messages = performValidation(
            repo, receiveEvent.commit, cfg);
        if (!messages.isEmpty()) {
          throw new CommitValidationException(
              "contains files with an invalid filename", messages);
        }
      }
    } catch (NoSuchProjectException | IOException e) {
      throw new CommitValidationException(
          "failed to check on invalid file names", e);
    }
    return Collections.emptyList();
  }

  @Override
  public List<CommitValidationMessage> performValidation(Repository repo,
      RevCommit c, PluginConfig cfg) throws IOException {
    List<Pattern> invalidFilenamePatterns = new ArrayList<>();
    for (String s : cfg.getStringList(KEY_INVALID_FILENAME_PATTERN)) {
      invalidFilenamePatterns.add(Pattern.compile(s));
    }
    List<CommitValidationMessage> messages = new LinkedList<>();
    for (String file : ChangeUtils.getChangedPaths(repo, c)) {
      for (Pattern p : invalidFilenamePatterns) {
        if (p.matcher(file).find()) {
          messages.add(new CommitValidationMessage(
              "invalid characters found in filename: " + file, true));
          break;
        }
      }
    }
    return messages;
  }

  public static AbstractModule module() {
    return new AbstractModule() {

      @Override
      protected void configure() {
        DynamicSet.bind(binder(), CommitValidationListener.class)
            .to(InvalidFilenameValidator.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_INVALID_FILENAME_PATTERN))
            .toInstance(new ProjectConfigEntry("Invalid Filename Pattern", null,
                ProjectConfigEntry.Type.ARRAY, null, false,
                "Invalid filenames. Pushes of commits that contain filenames "
                    + "which match one of these patterns will be rejected."));
      }
    };
  }
}
