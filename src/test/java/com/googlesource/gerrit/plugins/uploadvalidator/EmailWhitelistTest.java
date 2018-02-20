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

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.uploadvalidator.TestUtils.EMPTY_PLUGIN_CONFIG;

import org.junit.Test;

public class EmailWhitelistTest {
  private static final String[] emailWhitelist = {
    ".*@example.com.*", "testing.com", "tester@testing.com", ".*google.com", "tester@gerrit.*"
  };

  @Test
  public void testEmailValid() throws Exception {
    assertThat(EmailWhitelistValidator.performValidation("tester@example.com.net", emailWhitelist))
        .isTrue();
    assertThat(EmailWhitelistValidator.performValidation("tester@testing.com", emailWhitelist))
        .isTrue();
    assertThat(EmailWhitelistValidator.performValidation("tester@google.com", emailWhitelist))
        .isTrue();
    assertThat(EmailWhitelistValidator.performValidation("tester@gerrit.net", emailWhitelist))
        .isTrue();
  }

  @Test
  public void testEmailInvalid() throws Exception {
    assertThat(EmailWhitelistValidator.performValidation("tester@example.org", emailWhitelist))
        .isFalse();
    assertThat(EmailWhitelistValidator.performValidation("test@testing.com", emailWhitelist))
        .isFalse();
    assertThat(EmailWhitelistValidator.performValidation("tester@google.com.net", emailWhitelist))
        .isFalse();
    assertThat(EmailWhitelistValidator.performValidation("emailtester@gerrit.net", emailWhitelist))
        .isFalse();
  }

  @Test
  public void testEmailNull() throws Exception {
    assertThat(EmailWhitelistValidator.performValidation(null, emailWhitelist)).isFalse();
  }

  @Test
  public void testEmailEmpty() throws Exception {
    assertThat(EmailWhitelistValidator.performValidation("", emailWhitelist)).isFalse();
  }

  @Test
  public void validatorBehaviorWhenAuthorConfigEmpty() {
    assertThat(EmailWhitelistValidator.isAuthorActive(EMPTY_PLUGIN_CONFIG)).isFalse();
    assertThat(EmailWhitelistValidator.getAuthorEmailWhiteList(EMPTY_PLUGIN_CONFIG)).isEmpty();
  }

  @Test
  public void validatorBehaviorWhenCommitterConfigEmpty() {
    assertThat(EmailWhitelistValidator.isCommitterActive(EMPTY_PLUGIN_CONFIG)).isFalse();
    assertThat(EmailWhitelistValidator.getCommitterEmailWhiteList(EMPTY_PLUGIN_CONFIG)).isEmpty();
  }
}
