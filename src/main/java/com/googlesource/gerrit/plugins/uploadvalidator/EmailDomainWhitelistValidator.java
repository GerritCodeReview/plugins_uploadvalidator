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
import com.google.gerrit.server.git.GitRepositoryManager;
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

public class EmailDomainWhitelistValidator implements CommitValidationListener {

  public static AbstractModule module() {
    return new AbstractModule() {
      @Override
      public void configure() {
        DynamicSet.bind(binder(), CommitValidationListener.class)
            .to(EmailDomainWhitelistValidator.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_EMAIL_DOMAIN_WHITELIST))
            .toInstance(
                new ProjectConfigEntry(
                    "Email Domain White List",
                    null,
                    ProjectConfigEntryType.ARRAY,
                    null,
                    false,
                    "Whitelist of email domains that will be permitted."));
      }
    };
  }

  public static final String KEY_EMAIL_DOMAIN_WHITELIST = "emailDomainWhitelist";

  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final GitRepositoryManager repoManager;
  private final ValidatorConfig validatorConfig;

  @Inject
  EmailDomainWhitelistValidator(
      @PluginName String pluginName,
      PluginConfigFactory cfgFactory,
      GitRepositoryManager repoManager,
      ValidatorConfig validatorConfig) {
    this.pluginName = pluginName;
    this.cfgFactory = cfgFactory;
    this.repoManager = repoManager;
    this.validatorConfig = validatorConfig;
  }

  @VisibleForTesting
  static String[] getEmailDomainWhiteList(PluginConfig cfg) {
    return cfg.getStringList(KEY_EMAIL_DOMAIN_WHITELIST);
  }

  @VisibleForTesting
  static boolean isActive(PluginConfig cfg) {
    return cfg.getStringList(KEY_EMAIL_DOMAIN_WHITELIST).length > 0;
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
              KEY_EMAIL_DOMAIN_WHITELIST)) {
        if (!performValidation(
            receiveEvent.user.getAccount().getPreferredEmail(), getEmailDomainWhiteList(cfg))) {
          throw new CommitValidationException(
              "Email <"
                  + receiveEvent.user.getAccount().getPreferredEmail()
                  + "> - is not whitelisted for this Project.");
        }
      }
    } catch (NoSuchProjectException e) {
      throw new CommitValidationException("Failed to check for Email Domain Whitelist ", e);
    }
    return Collections.emptyList();
  }

  @VisibleForTesting
  static boolean performValidation(String email, String[] emailDomainWhitelist) {

    return Arrays.stream(emailDomainWhitelist)
        .anyMatch(s -> Pattern.matches(s, Strings.nullToEmpty(email)));
  }
}
