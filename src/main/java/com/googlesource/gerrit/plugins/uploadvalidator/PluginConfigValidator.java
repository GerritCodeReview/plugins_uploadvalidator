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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.primitives.Ints;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.git.validators.ValidationMessage;
import com.google.gerrit.server.project.ProjectConfig;
import com.google.gerrit.server.project.ProjectLevelConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;

public class PluginConfigValidator implements CommitValidationListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final String pluginName;

  @Inject
  PluginConfigValidator(@PluginName String pluginName) {
    this.pluginName = pluginName;
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
    String fileName = ProjectConfig.PROJECT_CONFIG;

    try {
      if (!receiveEvent.refName.equals(RefNames.REFS_CONFIG)) {
        // the project.config file in refs/meta/config was not modified, so no need to
        // modify
        return ImmutableList.of();
      }

      ProjectLevelConfig.Bare cfg = loadConfig(receiveEvent, fileName);
      // Project Level Config looks at what's in the refs/meta/config file.
      ImmutableList<CommitValidationMessage> validationMessages =
          validateConfig(fileName, cfg.getConfig());
      if (!validationMessages.isEmpty()) {
        throw new CommitValidationException(
            exceptionMessage(fileName, cfg.getRevision()), validationMessages);
      }
      return ImmutableList.of();
    } catch (IOException e) {
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
    validationMessages.addAll(
        validateRegex(fileName, cfg, ChangeEmailValidator.KEY_ALLOWED_AUTHOR_EMAIL_PATTERN));
    validationMessages.addAll(
        validateRegex(fileName, cfg, ChangeEmailValidator.KEY_ALLOWED_COMMITTER_EMAIL_PATTERN));
    validationMessages.addAll(
        validateInteger(fileName, cfg, MaxPathLengthValidator.KEY_MAX_PATH_LENGTH));
    return validationMessages.build();
  }

  /**
   * Validates the regex
   *
   * @param fileName the name of the config file
   * @param cfg the project.config to validate
   * @return list of messages with validation issues, empty list if there are no issues
   */
  @VisibleForTesting
  public ImmutableList<CommitValidationMessage> validateRegex(
      String fileName, Config cfg, String validatorKey) {

    String pattern = cfg.getString("plugin", pluginName, validatorKey);

    if (pattern != null) {
      try {
        Pattern.compile(pattern);
      } catch (PatternSyntaxException e) {
        return ImmutableList.of(
            new CommitValidationMessage(
                String.format(
                    "The value '%s' configured in %s (parameter %s.%s) is invalid.",
                    pattern, fileName, pluginName, validatorKey),
                ValidationMessage.Type.ERROR));
      }
    }
    return ImmutableList.of();
  }

  /**
   * Validates the path length set in project.config for uploadvalidator. Must be integer
   *
   * @param fileName the name of the config file
   * @param cfg the project.config to validate
   * @return list of messages with validation issues, empty list if there are no issues
   */
  @VisibleForTesting
  public ImmutableList<CommitValidationMessage> validateInteger(
      String fileName, Config cfg, String validatorKey) {

    String value = cfg.getString("plugin", pluginName, validatorKey);

    if (Ints.tryParse(value) == null) {
      return ImmutableList.of(
          new CommitValidationMessage(
              String.format(
                  "The value '%s' configured in %s (parameter %s.%s) is invalid.",
                  value, fileName, pluginName, validatorKey),
              ValidationMessage.Type.ERROR));
    }
    return ImmutableList.of();
  }
}
