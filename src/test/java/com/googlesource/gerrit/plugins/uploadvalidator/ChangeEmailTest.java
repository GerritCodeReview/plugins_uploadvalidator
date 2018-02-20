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

public class ChangeEmailTest {
  private static final String[] allowedEmailPatterns = {
    ".*@example\\.com.*",
    "testing\\.com",
    "tester@testing\\.com",
    ".*google\\.com",
    "tester@gerrit\\..*"
  };

  @Test
  public void testEmailValid() throws Exception {
    assertThat(
            ChangeEmailValidator.performValidation("tester@example.com.net", allowedEmailPatterns))
        .isTrue();
    assertThat(ChangeEmailValidator.performValidation("tester@testing.com", allowedEmailPatterns))
        .isTrue();
    assertThat(ChangeEmailValidator.performValidation("tester@google.com", allowedEmailPatterns))
        .isTrue();
    assertThat(ChangeEmailValidator.performValidation("tester@gerrit.net", allowedEmailPatterns))
        .isTrue();
  }

  @Test
  public void testEmailInvalid() throws Exception {
    assertThat(ChangeEmailValidator.performValidation("tester@example.org", allowedEmailPatterns))
        .isFalse();
    assertThat(ChangeEmailValidator.performValidation("test@testing.com", allowedEmailPatterns))
        .isFalse();
    assertThat(
            ChangeEmailValidator.performValidation("tester@google.com.net", allowedEmailPatterns))
        .isFalse();
    assertThat(
            ChangeEmailValidator.performValidation(
                "emailtester@gerritnet.com", allowedEmailPatterns))
        .isFalse();
  }

  @Test
  public void testEmailNull() throws Exception {
    assertThat(ChangeEmailValidator.performValidation(null, allowedEmailPatterns)).isFalse();
  }

  @Test
  public void testEmailEmpty() throws Exception {
    assertThat(ChangeEmailValidator.performValidation("", allowedEmailPatterns)).isFalse();
  }

  @Test
  public void validatorBehaviorWhenAuthorConfigEmpty() {
    assertThat(ChangeEmailValidator.isAuthorActive(EMPTY_PLUGIN_CONFIG)).isFalse();
    assertThat(ChangeEmailValidator.getAllowedAuthorEmailPatterns(EMPTY_PLUGIN_CONFIG)).isEmpty();
  }

  @Test
  public void validatorBehaviorWhenCommitterConfigEmpty() {
    assertThat(ChangeEmailValidator.isCommitterActive(EMPTY_PLUGIN_CONFIG)).isFalse();
    assertThat(ChangeEmailValidator.getAllowedCommitterEmailPatterns(EMPTY_PLUGIN_CONFIG))
        .isEmpty();
  }
}
