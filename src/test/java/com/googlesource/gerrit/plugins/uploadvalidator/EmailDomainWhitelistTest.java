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

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Test;

public class EmailDomainWhitelistTest {
  private static final List<String> emailDomainWhitelist =
      Lists.newArrayList("example.com", "testing.com", "gerrit.com");

  private static final String emailValidListed = "test@example.com";
  private static final String emailValidNotListed = "test@android.com";
  private static final String emailInvalidNotListed = "email@example@google.com";

  @Test
  public void testEmailVaildListed() throws Exception {
    assertThat(
            EmailDomainWhitelistValidator.performValidation(emailValidListed, emailDomainWhitelist))
        .isTrue();
  }

  @Test
  public void testEmailVaildNotListed() throws Exception {
    assertThat(
            EmailDomainWhitelistValidator.performValidation(
                emailValidNotListed, emailDomainWhitelist))
        .isFalse();
  }

  @Test
  public void testEmailInvaildNotListed() throws Exception {
    assertThat(
            EmailDomainWhitelistValidator.performValidation(
                emailInvalidNotListed, emailDomainWhitelist))
        .isFalse();
  }

  @Test
  public void testGetEmailWhitelist() throws Exception {

  }

  @Test
  public void validatorInactiveWhenConfigEmpty() {
    assertThat(EmailDomainWhitelistValidator.isActive(EMPTY_PLUGIN_CONFIG)).isFalse();
  }
}
