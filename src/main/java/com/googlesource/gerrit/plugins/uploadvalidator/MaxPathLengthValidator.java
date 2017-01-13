// Copyright (C) 2014 The Android Open Source Project
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MaxPathLengthValidator implements CommitValidationListener {

  public static AbstractModule module() {
    return new AbstractModule() {

      @Override
      protected void configure() {
        DynamicSet.bind(binder(), CommitValidationListener.class)
            .to(MaxPathLengthValidator.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_MAX_PATH_LENGTH))
            .toInstance(new ProjectConfigEntry("Max Path Length", 0, false,
                "Maximum path length. Pushes of commits that "
                    + "contain files with longer paths will be rejected. "
                    + "'0' means no limit."));
      }
    };
  }

  public static String KEY_MAX_PATH_LENGTH = "maxPathLength";

  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final GitRepositoryManager repoManager;
  private final ValidatorConfig validatorConfig;

  @Inject
  MaxPathLengthValidator(@PluginName String pluginName,
      PluginConfigFactory cfgFactory, GitRepositoryManager repoManager,
      ValidatorConfig validatorConfig) {
    this.pluginName = pluginName;
    this.cfgFactory = cfgFactory;
    this.repoManager = repoManager;
    this.validatorConfig = validatorConfig;
  }

  static boolean isActive(PluginConfig cfg) {
    return cfg.getInt(KEY_MAX_PATH_LENGTH, 0) > 0;
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(
      CommitReceivedEvent receiveEvent) throws CommitValidationException {
    try {
      PluginConfig cfg =
          cfgFactory.getFromProjectConfigWithInheritance(
              receiveEvent.project.getNameKey(), pluginName);
      if (isActive(cfg)
          && validatorConfig.isEnabledForRef(receiveEvent.getProjectNameKey(),
              receiveEvent.getRefName(), KEY_MAX_PATH_LENGTH)) {
        int maxPathLength = cfg.getInt(KEY_MAX_PATH_LENGTH, 0);
        try (Repository repo =
            repoManager.openRepository(receiveEvent.project.getNameKey())) {
          List<CommitValidationMessage> messages =
              performValidation(repo, receiveEvent.commit, maxPathLength);
          if (!messages.isEmpty()) {
            throw new CommitValidationException(
                "contains files with too long paths (max path length: "
                    + maxPathLength + ")", messages);
          }
        }
      }
    } catch (NoSuchProjectException | IOException e) {
      throw new CommitValidationException(
          "failed to check for max file path length", e);
    }
    return Collections.emptyList();
  }

  static List<CommitValidationMessage> performValidation(Repository repo,
      RevCommit c, int maxPathLength) throws IOException {
    List<CommitValidationMessage> messages = new LinkedList<>();
    for (String file : CommitUtils.getChangedPaths(repo, c)) {
      if (file.length() > maxPathLength) {
        messages.add(new CommitValidationMessage("path too long: " + file, true));
      }
    }
    return messages;
  }
}
