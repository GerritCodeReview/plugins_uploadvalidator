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
  private static final String[] emailPatterns = {
    ".*@example\\.com.*",
    "testing\\.com",
    "tester@testing\\.com",
    ".*google\\.com",
    "tester@gerrit\\..*",
    "(?!some_).*@someinc\\.com",
  };

  @Test
  public void testEmailMatches() throws Exception {
    assertThat(ChangeEmailValidator.match("tester@example.com.net", emailPatterns)).isTrue();
    assertThat(ChangeEmailValidator.match("tester@testing.com", emailPatterns)).isTrue();
    assertThat(ChangeEmailValidator.match("tester@google.com", emailPatterns)).isTrue();
    assertThat(ChangeEmailValidator.match("tester@gerrit.net", emailPatterns)).isTrue();
    assertThat(ChangeEmailValidator.match("user@someinc.com", emailPatterns)).isTrue();
    assertThat(ChangeEmailValidator.match("some_user@someinc.com", emailPatterns)).isFalse();
    assertThat(ChangeEmailValidator.match("some_user@something_different.com", emailPatterns))
        .isFalse();
  }

  @Test
  public void testEmailDoesNotMatch() throws Exception {
    assertThat(ChangeEmailValidator.match("tester@example.org", emailPatterns)).isFalse();
    assertThat(ChangeEmailValidator.match("test@testing.com", emailPatterns)).isFalse();
    assertThat(ChangeEmailValidator.match("tester@google.com.net", emailPatterns)).isFalse();
    assertThat(ChangeEmailValidator.match("emailtester@gerritnet.com", emailPatterns)).isFalse();
  }

  @Test
  public void testEmailNullDoesNotMatch() throws Exception {
    assertThat(ChangeEmailValidator.match(null, emailPatterns)).isFalse();
  }

  @Test
  public void testEmailEmptyDoesNotMatch() throws Exception {
    assertThat(ChangeEmailValidator.match("", emailPatterns)).isFalse();
  }

  @Test
  public void validatorBehaviorWhenAuthorAllowListConfigEmpty() {
    assertThat(ChangeEmailValidator.isAuthorAllowListActive(EMPTY_PLUGIN_CONFIG)).isFalse();
    assertThat(ChangeEmailValidator.getAllowedAuthorEmailPatterns(EMPTY_PLUGIN_CONFIG)).isEmpty();
  }

  @Test
  public void validatorBehaviorWhenAuthorRejectListConfigEmpty() {
    assertThat(ChangeEmailValidator.isAuthorRejectListActive(EMPTY_PLUGIN_CONFIG)).isFalse();
    assertThat(ChangeEmailValidator.getRejectedAuthorEmailPatterns(EMPTY_PLUGIN_CONFIG)).isEmpty();
  }

  @Test
  public void validatorBehaviorWhenCommitterAllowListConfigEmpty() {
    assertThat(ChangeEmailValidator.isCommitterAllowListActive(EMPTY_PLUGIN_CONFIG)).isFalse();
    assertThat(ChangeEmailValidator.getAllowedCommitterEmailPatterns(EMPTY_PLUGIN_CONFIG))
        .isEmpty();
  }

  @Test
  public void validatorBehaviorWhenCommitterRejectListConfigEmpty() {
    assertThat(ChangeEmailValidator.isCommitterRejectListActive(EMPTY_PLUGIN_CONFIG)).isFalse();
    assertThat(ChangeEmailValidator.getRejectedCommitterEmailPatterns(EMPTY_PLUGIN_CONFIG))
        .isEmpty();
  }
}
