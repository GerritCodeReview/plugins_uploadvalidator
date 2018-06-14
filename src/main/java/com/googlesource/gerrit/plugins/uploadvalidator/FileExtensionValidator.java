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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

public class FileExtensionValidator implements CommitValidationListener {

  public static AbstractModule module() {
    return new AbstractModule() {

      @Override
      public void configure() {
        DynamicSet.bind(binder(), CommitValidationListener.class).to(FileExtensionValidator.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_BLOCKED_FILE_EXTENSION))
            .toInstance(
                new ProjectConfigEntry(
                    "Blocked File Extensions",
                    null,
                    ProjectConfigEntryType.ARRAY,
                    null,
                    false,
                    "Forbidden file extensions. Pushes of commits that "
                        + "contain files with these extensions will be rejected."));
      }
    };
  }

  public static final String KEY_BLOCKED_FILE_EXTENSION = "blockedFileExtension";

  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final GitRepositoryManager repoManager;
  private final ValidatorConfig validatorConfig;

  @Inject
  FileExtensionValidator(
      @PluginName String pluginName,
      PluginConfigFactory cfgFactory,
      GitRepositoryManager repoManager,
      ValidatorConfig validatorConfig) {
    this.pluginName = pluginName;
    this.cfgFactory = cfgFactory;
    this.repoManager = repoManager;
    this.validatorConfig = validatorConfig;
  }

  private static List<String> getBlockedExtensions(PluginConfig cfg) {
    List<String> blockedExtensions = new ArrayList<>();
    for (String extension : cfg.getStringList(KEY_BLOCKED_FILE_EXTENSION)) {
      blockedExtensions.add(extension.toLowerCase());
    }
    return blockedExtensions;
  }

  static boolean isActive(PluginConfig cfg) {
    return cfg.getStringList(KEY_BLOCKED_FILE_EXTENSION).length > 0;
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
      throws CommitValidationException {
    try {
      PluginConfig cfg =
          cfgFactory.getFromProjectConfigWithInheritance(
              receiveEvent.project.getNameKey(), pluginName);
      if (isActive(cfg)
          && validatorConfig.isEnabledForRef(
              receiveEvent.user,
              receiveEvent.getProjectNameKey(),
              receiveEvent.getRefName(),
              KEY_BLOCKED_FILE_EXTENSION)) {
        try (Repository repo = repoManager.openRepository(receiveEvent.project.getNameKey())) {
          List<CommitValidationMessage> messages =
              performValidation(repo, receiveEvent.commit, getBlockedExtensions(cfg));
          if (!messages.isEmpty()) {
            throw new CommitValidationException(
                "contains files with blocked file extensions", messages);
          }
        }
      }
    } catch (NoSuchProjectException | IOException e) {
      throw new CommitValidationException("failed to check on file extensions", e);
    }
    return Collections.emptyList();
  }

  static List<CommitValidationMessage> performValidation(
      Repository repo, RevCommit c, List<String> blockedFileExtensions) throws IOException {
    List<CommitValidationMessage> messages = new LinkedList<>();
    for (String file : CommitUtils.getChangedPaths(repo, c)) {
      for (String blockedExtension : blockedFileExtensions) {
        if (file.toLowerCase().endsWith(blockedExtension.toLowerCase())) {
          messages.add(new CommitValidationMessage("blocked file: " + file, true));
          break;
        }
      }
    }
    return messages;
  }
}
