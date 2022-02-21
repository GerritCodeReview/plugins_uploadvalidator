// Copyright (C) 2022 The Android Open Source Project
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

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.git.validators.ValidationMessage;
import com.google.gerrit.server.patch.DiffNotAvailableException;
import com.google.gerrit.server.patch.DiffOperations;
import com.google.gerrit.server.project.ProjectConfig;
import com.google.gerrit.server.project.ProjectLevelConfig;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;

public class PluginConfigValidator implements CommitValidationListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final DiffOperations diffOperations;
  private final ProjectConfig.Factory projectConfigFactory;
  private final ProjectState.Factory projectStateFactory;
  private final ChangeEmailValidator changeEmailValidator;

  @Inject
  PluginConfigValidator(
      DiffOperations diffOperations,
      ProjectConfig.Factory projectConfigFactory,
      ProjectState.Factory projectStateFactory,
      ChangeEmailValidator changeEmailValidator) {
    this.diffOperations = diffOperations;
    this.projectConfigFactory = projectConfigFactory;
    this.projectStateFactory = projectStateFactory;
    this.changeEmailValidator = changeEmailValidator;
  }

  public static AbstractModule module() {
    return new AbstractModule() {
      @Override
      public void configure() {
        DynamicSet.bind(binder(), CommitValidationListener.class).to(PluginConfigValidator.class);
      }
    };
  }

  @Override
  public ImmutableList<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
      throws CommitValidationException {
    String fileName = "project.config";

    try {
      if (!receiveEvent.refName.equals(RefNames.REFS_CONFIG)
          || !isFileChanged(receiveEvent, fileName)) {
        // the project.config file in refs/meta/config was not modified, so no need to modify
        return ImmutableList.of();
      }

      ProjectConfig projectConfig = getProjectConfig(receiveEvent);
      ProjectLevelConfig.Bare cfg = loadConfig(receiveEvent, fileName);
      // Project Level Config looks at what's in the refs/meta/config file.
      ImmutableList<CommitValidationMessage> validationMessages =
          validateConfig(fileName, cfg.getConfig());
      if (!validationMessages.isEmpty()) {
        throw new CommitValidationException(
            exceptionMessage(fileName, cfg.getRevision()), validationMessages);
      }
      return ImmutableList.of();
    } catch (IOException | DiffNotAvailableException | ConfigInvalidException e) {
      String errorMessage =
          String.format(
              "failed to validate file %s for revision %s in ref %s of project %s",
              fileName,
              receiveEvent.commit.getName(),
              RefNames.REFS_CONFIG,
              receiveEvent.project.getNameKey());
      logger.atSevere().withCause(e).log("%s", errorMessage);
      throw new CommitValidationException(errorMessage, e);
    }
  }

  /**
   * Whether the given file was changed in the given revision.
   *
   * @param receiveEvent the receive event
   * @param fileName the name of the file
   */
  private boolean isFileChanged(CommitReceivedEvent receiveEvent, String fileName)
      throws IOException, DiffNotAvailableException {
    return diffOperations
        .listModifiedFilesAgainstParent(
            receiveEvent.project.getNameKey(), receiveEvent.commit, /* parentNum=*/ 0)
        .keySet().stream()
        .anyMatch(fileName::equals);
  }

  /**
   * Loads the configuration from the file and revision.
   *
   * @param receiveEvent the receive event
   * @param fileName the name of the config file
   * @return the loaded configuration
   * @throws CommitValidationException thrown if the configuration is invalid and cannot be parsed
   */
  private ProjectLevelConfig.Bare loadConfig(CommitReceivedEvent receiveEvent, String fileName)
      throws CommitValidationException, IOException {
    ProjectLevelConfig.Bare cfg = new ProjectLevelConfig.Bare(fileName);
    try {
      cfg.load(receiveEvent.project.getNameKey(), receiveEvent.revWalk, receiveEvent.commit);
    } catch (ConfigInvalidException e) {
      throw new CommitValidationException(
          exceptionMessage(fileName, receiveEvent.commit),
          new CommitValidationMessage(e.getMessage(), ValidationMessage.Type.ERROR));
    }
    return cfg;
  }

  /**
   * Creates the message for {@link CommitValidationException}s that are thrown for validation
   * errors in the project-level code-owners configuration.
   *
   * @param fileName the name of the config file
   * @param revision the revision in which the configuration is invalid
   * @return the created exception message
   */
  private static String exceptionMessage(String fileName, ObjectId revision) {
    return String.format("invalid %s file in revision %s", fileName, revision.getName());
  }

  /**
   * Validates the project.config for uploadvalidator
   *
   * @param fileName the name of the config file
   * @param cfg the project-level code-owners configuration that should be validated
   * @return list of messages with validation issues, empty list if there are no issues
   */
  public ImmutableList<CommitValidationMessage> validateConfig(String fileName, Config cfg) {
    ImmutableList.Builder<CommitValidationMessage> validationMessages = ImmutableList.builder();
    validationMessages.addAll(changeEmailValidator.validateConfig(fileName, cfg));
    // This is where we can add the validators for the other modules in the plugin.
    return validationMessages.build();
  }

  private ProjectConfig getProjectConfig(CommitReceivedEvent receiveEvent)
      throws IOException, ConfigInvalidException {
    ProjectConfig projectConfig = projectConfigFactory.create(receiveEvent.project.getNameKey());
    projectConfig.load(receiveEvent.revWalk, receiveEvent.commit);
    return projectConfig;
  }
}
