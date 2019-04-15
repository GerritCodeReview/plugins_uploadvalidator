// Copyright (C) 2018 The Android Open Source Project
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
import com.google.common.base.Strings;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.api.projects.ProjectConfigEntryType;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.ProjectConfigEntry;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class ChangeEmailValidator implements CommitValidationListener {
  public static AbstractModule module() {
    return new AbstractModule() {
      @Override
      public void configure() {
        DynamicSet.bind(binder(), CommitValidationListener.class).to(ChangeEmailValidator.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_ALLOWED_AUTHOR_EMAIL_PATTERN))
            .toInstance(
                new ProjectConfigEntry(
                    "Author Email Pattern",
                    null,
                    ProjectConfigEntryType.ARRAY,
                    null,
                    false,
                    "Commits with author email not matching one of these pattterns will be rejected."));
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_ALLOWED_COMMITTER_EMAIL_PATTERN))
            .toInstance(
                new ProjectConfigEntry(
                    "Committer Email Pattern",
                    null,
                    ProjectConfigEntryType.ARRAY,
                    null,
                    false,
                    "Commits with committer email not matching one of these patterns will be rejected."));
      }
    };
  }

  public static final String KEY_ALLOWED_AUTHOR_EMAIL_PATTERN = "allowedAuthorEmailPattern";
  public static final String KEY_ALLOWED_COMMITTER_EMAIL_PATTERN = "allowedCommitterEmailPattern";
  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final ValidatorConfig validatorConfig;

  @Inject
  ChangeEmailValidator(
      @PluginName String pluginName,
      PluginConfigFactory cfgFactory,
      ValidatorConfig validatorConfig) {
    this.pluginName = pluginName;
    this.cfgFactory = cfgFactory;
    this.validatorConfig = validatorConfig;
  }

  @VisibleForTesting
  static String[] getAllowedAuthorEmailPatterns(PluginConfig cfg) {
    return cfg.getStringList(KEY_ALLOWED_AUTHOR_EMAIL_PATTERN);
  }

  @VisibleForTesting
  static String[] getAllowedCommitterEmailPatterns(PluginConfig cfg) {
    return cfg.getStringList(KEY_ALLOWED_COMMITTER_EMAIL_PATTERN);
  }

  @VisibleForTesting
  static boolean isAuthorActive(PluginConfig cfg) {
    return cfg.getStringList(KEY_ALLOWED_AUTHOR_EMAIL_PATTERN).length > 0;
  }

  @VisibleForTesting
  static boolean isCommitterActive(PluginConfig cfg) {
    return cfg.getStringList(KEY_ALLOWED_COMMITTER_EMAIL_PATTERN).length > 0;
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
      throws CommitValidationException {
    try {
      PluginConfig cfg =
          cfgFactory.getFromProjectConfigWithInheritance(
              receiveEvent.project.getNameKey(), pluginName);
      if (isAuthorActive(cfg)
          && validatorConfig.isEnabledForRef(
              receiveEvent.user,
              receiveEvent.getProjectNameKey(),
              receiveEvent.getRefName(),
              KEY_ALLOWED_AUTHOR_EMAIL_PATTERN)) {
        if (!performValidation(
            receiveEvent.commit.getAuthorIdent().getEmailAddress(),
            getAllowedAuthorEmailPatterns(cfg))) {
          throw new CommitValidationException(
              "Author Email <"
                  + receiveEvent.commit.getAuthorIdent().getEmailAddress()
                  + "> - is not allowed for this Project.");
        }
      }
      if (isCommitterActive(cfg)
          && validatorConfig.isEnabledForRef(
              receiveEvent.user,
              receiveEvent.getProjectNameKey(),
              receiveEvent.getRefName(),
              KEY_ALLOWED_COMMITTER_EMAIL_PATTERN)) {
        if (!performValidation(
            receiveEvent.commit.getCommitterIdent().getEmailAddress(),
            getAllowedCommitterEmailPatterns(cfg))) {
          throw new CommitValidationException(
              "Committer Email <"
                  + receiveEvent.commit.getCommitterIdent().getEmailAddress()
                  + "> - is not allowed for this Project.");
        }
      }
    } catch (NoSuchProjectException e) {
      throw new CommitValidationException("Failed to check for Change Email Patterns ", e);
    }
    return Collections.emptyList();
  }

  @VisibleForTesting
  static boolean performValidation(String email, String[] allowedEmailPatterns) {
    return Arrays.stream(allowedEmailPatterns)
        .anyMatch(s -> Pattern.matches(s, Strings.nullToEmpty(email)));
  }
}
