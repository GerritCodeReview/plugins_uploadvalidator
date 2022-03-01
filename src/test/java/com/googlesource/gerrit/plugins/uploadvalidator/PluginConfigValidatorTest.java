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

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.uploadvalidator.ChangeEmailValidator.KEY_ALLOWED_AUTHOR_EMAIL_PATTERN;
import static com.googlesource.gerrit.plugins.uploadvalidator.ChangeEmailValidator.KEY_ALLOWED_COMMITTER_EMAIL_PATTERN;
import static com.googlesource.gerrit.plugins.uploadvalidator.MaxPathLengthValidator.KEY_MAX_PATH_LENGTH;
import static com.google.gerrit.server.project.ProjectConfig.PROJECT_CONFIG;

import com.google.common.collect.ImmutableList;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;

public class PluginConfigValidatorTest {
  private static final String PLUGIN_NAME = "uploadvalidator";
  private static final String ILLEGAL_REGEX = "*";
  private static final String LEGAL_REGEX = ".*";
  private static final String LEGAL_PATH_LENGTH = "100";
  private static final String ILLEGAL_PATH_LENGTH = "10xi";

  private PluginConfigValidator configValidator;
  private Config cfg;

  @Before
  public void setUp() throws Exception {
    configValidator = new PluginConfigValidator(PLUGIN_NAME);
    cfg = new Config();
  }

  @Test
  public void hasLegalAuthorEmailPattern_noMessages() throws Exception {
    cfg.setString("plugin", PLUGIN_NAME, KEY_ALLOWED_AUTHOR_EMAIL_PATTERN, LEGAL_REGEX);
    ImmutableList<CommitValidationMessage> messages =
        configValidator.validateRegex(PROJECT_CONFIG, cfg, KEY_ALLOWED_AUTHOR_EMAIL_PATTERN);
    assertThat(messages).isEmpty();
  }

  @Test
  public void hasIllegalAuthorEmailPattern_messages() throws Exception {
    cfg.setString("plugin", PLUGIN_NAME, KEY_ALLOWED_AUTHOR_EMAIL_PATTERN, ILLEGAL_REGEX);
    ImmutableList<CommitValidationMessage> messages =
        configValidator.validateRegex(PROJECT_CONFIG, cfg, KEY_ALLOWED_AUTHOR_EMAIL_PATTERN);
    assertThat(messages).isNotEmpty();
  }

  @Test
  public void hasLegalCommitterEmailPattern_noMessages() throws Exception {
    cfg.setString("plugin", PLUGIN_NAME, KEY_ALLOWED_COMMITTER_EMAIL_PATTERN, LEGAL_REGEX);
    ImmutableList<CommitValidationMessage> messages =
        configValidator.validateRegex(PROJECT_CONFIG, cfg, KEY_ALLOWED_COMMITTER_EMAIL_PATTERN);
    assertThat(messages).isEmpty();
  }

  @Test
  public void hasIllegalCommitterEmailPattern_messages() throws Exception {
    cfg.setString("plugin", PLUGIN_NAME, KEY_ALLOWED_COMMITTER_EMAIL_PATTERN, ILLEGAL_REGEX);
    ImmutableList<CommitValidationMessage> messages =
        configValidator.validateRegex(PROJECT_CONFIG, cfg, KEY_ALLOWED_COMMITTER_EMAIL_PATTERN);
    assertThat(messages).isNotEmpty();
  }
  
  @Test
  public void hasLegalMaxPathLength_noMessages() throws Exception {
    cfg.setString("plugin", PLUGIN_NAME, KEY_MAX_PATH_LENGTH, LEGAL_PATH_LENGTH);
    ImmutableList<CommitValidationMessage> messages =
        configValidator.validateInteger(PROJECT_CONFIG, cfg, KEY_MAX_PATH_LENGTH);
    assertThat(messages).isEmpty();
  }

  @Test
  public void hasIllegalMaxPathLength_messages() throws Exception {
    cfg.setString("plugin", PLUGIN_NAME, KEY_MAX_PATH_LENGTH, ILLEGAL_PATH_LENGTH);

    ImmutableList<CommitValidationMessage> messages =
        configValidator.validateInteger(PROJECT_CONFIG, cfg, KEY_MAX_PATH_LENGTH);
    assertThat(messages).isNotEmpty();
  }
}