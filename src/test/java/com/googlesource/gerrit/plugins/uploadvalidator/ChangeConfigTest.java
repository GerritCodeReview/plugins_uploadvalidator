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

import com.google.common.collect.ImmutableList;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;

public class ChangeConfigTest {

  private String fileName = "project.config";
  private String pluginName = "uploadvalidator";
  private static final String illegalRegex = "*";
  private static final String legalRegex = ".*";
  private static final String legalPathLength = "100";
  private static final String illegalPathLength = "10xi";
  private PluginConfigValidator configValidator = new PluginConfigValidator(pluginName);
  private Config cfg = new Config();

  @Test
  public void hasLegalAuthorEmailPattern() throws Exception {
    String configKey = ChangeEmailValidator.KEY_ALLOWED_AUTHOR_EMAIL_PATTERN;
    cfg.setString("plugin", pluginName, configKey, legalRegex);
    ImmutableList<CommitValidationMessage> messages =
        configValidator.validateRegex(fileName, cfg, configKey);
    assertThat(messages).isEmpty();
  }

  @Test
  public void hasLegalCommitterEmailPattern() throws Exception {

    String configKey = ChangeEmailValidator.KEY_ALLOWED_COMMITTER_EMAIL_PATTERN;
    cfg.setString("plugin", pluginName, configKey, legalRegex);
    ImmutableList<CommitValidationMessage> messages =
        configValidator.validateRegex(fileName, cfg, configKey);
    assertThat(messages).isEmpty();
  }

  @Test
  public void hasIllegalAuthorEmailPattern() throws Exception {
    String configKey = ChangeEmailValidator.KEY_ALLOWED_AUTHOR_EMAIL_PATTERN;
    cfg.setString("plugin", pluginName, configKey, illegalRegex);
    ImmutableList<CommitValidationMessage> messages =
        configValidator.validateRegex(fileName, cfg, configKey);
    assertThat(messages).isNotEmpty();
  }

  @Test
  public void hasIllegalCommitterEmailPattern() throws Exception {
    String configKey = ChangeEmailValidator.KEY_ALLOWED_COMMITTER_EMAIL_PATTERN;
    cfg.setString("plugin", pluginName, configKey, illegalRegex);
    ImmutableList<CommitValidationMessage> messages =
        configValidator.validateRegex(fileName, cfg, configKey);
    assertThat(messages).isNotEmpty();
  }

  @Test
  public void hasLegalMaxPathLength() throws Exception {
    String configKey = MaxPathLengthValidator.KEY_MAX_PATH_LENGTH;
    cfg.setString("plugin", pluginName, configKey, legalPathLength);
    ImmutableList<CommitValidationMessage> messages =
        configValidator.validateInteger(fileName, cfg, configKey);
    assertThat(messages).isEmpty();
  }

  @Test
  public void hasIllegalMaxPathLength() throws Exception {
    String configKey = MaxPathLengthValidator.KEY_MAX_PATH_LENGTH;
    cfg.setString("plugin", pluginName, configKey, illegalPathLength);

    ImmutableList<CommitValidationMessage> messages =
        configValidator.validateInteger(fileName, cfg, configKey);
    assertThat(messages).isNotEmpty();
  }
}
