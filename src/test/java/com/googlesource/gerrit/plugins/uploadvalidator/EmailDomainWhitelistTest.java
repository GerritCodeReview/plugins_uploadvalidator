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

public class EmailDomainWhitelistTest {
  private static final String[] emailDomainWhitelist = {
    ".*@example.com$", "testing.com", "tester@testing.com"
  };

  private static final String emailValidListed01 = "test@example.com";
  private static final String emailValidListed02 = "tester@testing.com";
  private static final String emailValidNotListed01 = "test@android.com";
  private static final String emailValidNotListed02 = "test@testing.com";
  private static final String emailInvalidNotListed = "email[/]example?test.com";
  private static final String emailNullNotListed = null;
  private static final String emailEmptyNotListed = "";

  @Test
  public void testEmailValidListed() throws Exception {
    assertThat(
            EmailDomainWhitelistValidator.performValidation(
                emailValidListed01, emailDomainWhitelist))
        .isTrue();
    assertThat(
            EmailDomainWhitelistValidator.performValidation(
                emailValidListed02, emailDomainWhitelist))
        .isTrue();
  }

  @Test
  public void testEmailValidNotListed() throws Exception {
    assertThat(
            EmailDomainWhitelistValidator.performValidation(
                emailValidNotListed01, emailDomainWhitelist))
        .isFalse();
    assertThat(
            EmailDomainWhitelistValidator.performValidation(
                emailValidNotListed02, emailDomainWhitelist))
        .isFalse();
  }

  @Test
  public void testEmailInvalidNotListed() throws Exception {
    assertThat(
            EmailDomainWhitelistValidator.performValidation(
                emailInvalidNotListed, emailDomainWhitelist))
        .isFalse();
  }

  @Test
  public void testEmailNullNotListed() throws Exception {
    assertThat(
            EmailDomainWhitelistValidator.performValidation(
                emailNullNotListed, emailDomainWhitelist))
        .isFalse();
  }

  @Test
  public void testEmailEmptyNotListed() throws Exception {
    assertThat(
            EmailDomainWhitelistValidator.performValidation(
                emailEmptyNotListed, emailDomainWhitelist))
        .isFalse();
  }

  @Test
  public void validatorBehaviorWhenConfigEmpty() {
    assertThat(EmailDomainWhitelistValidator.isActive(EMPTY_PLUGIN_CONFIG)).isFalse();
    assertThat(EmailDomainWhitelistValidator.getEmailDomainWhiteList(EMPTY_PLUGIN_CONFIG))
        .isEmpty();
  }
}
