// Copyright (C) 2017 The Android Open Source Project
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

import com.google.common.collect.ImmutableListMultimap;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.IdentifiedUser;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.junit.Test;

public class EmailAwareValidatorConfigTest {
  private Project.NameKey projectName = Project.nameKey("testProject");
  private IdentifiedUser anyUser = new FakeUserProvider().get();

  @Test
  public void isEnabledForAllEmailsByDefault() throws Exception {
    ValidatorConfig config =
        getConfig("[plugin \"uploadvalidator\"]\n" + "blockedFileExtension = jar");

    assertThat(config.isEnabled(
        anyUser, projectName, "anyRef", "blockedFileExtension", ImmutableListMultimap.of()))
        .isTrue();
  }

  @Test
  public void isEnabledForMissingEmailByDefault() throws Exception {
    IdentifiedUser missingEmail = new FakeUserProvider().get(null);
    ValidatorConfig config =
        getConfig("[plugin \"uploadvalidator\"]\n" + "blockedFileExtension = jar");

    assertThat(config.isEnabled(
        missingEmail, projectName, "anyRef", "blockedFileExtension", ImmutableListMultimap.of()))
        .isTrue();
  }

  @Test
  public void isEnabledForSingleEmail() throws Exception {
    ValidatorConfig config =
        getConfig(
            "[plugin \"uploadvalidator\"]\n"
                + "   email = "
                + FakeUserProvider.FAKE_EMAIL
                + "\n"
                + "   blockedFileExtension = jar");

    assertThat(
            config.isEnabled(
                anyUser,
                projectName,
                "refs/heads/anyref",
                "blockedFileExtension",
                ImmutableListMultimap.of()))
        .isTrue();
  }

  @Test
  public void isDisabledForInvalidEmail() throws Exception {
    ValidatorConfig config =
        getConfig(
            "[plugin \"uploadvalidator\"]\n"
                + "   email = anInvalidEmail@example.com\n"
                + "   blockedFileExtension = jar");

    assertThat(
            config.isEnabled(
                anyUser,
                projectName,
                "refs/heads/anyref",
                "blockedFileExtension",
                ImmutableListMultimap.of()))
        .isFalse();
  }

  @Test
  public void isEnabledForRegexEmail() throws Exception {
    IdentifiedUser exampleOrgUser = new FakeUserProvider().get("a@example.org");
    ValidatorConfig config =
        getConfig(
            "[plugin \"uploadvalidator\"]\n"
                + "   email = .*@example.org$\n"
                + "   blockedFileExtension = jar");

    assertThat(
            config.isEnabled(
                anyUser,
                projectName,
                "refs/heads/anyref",
                "blockedFileExtension",
                ImmutableListMultimap.of()))
        .isFalse();
    assertThat(
            config.isEnabled(
                exampleOrgUser,
                projectName,
                "refs/heads/anyref",
                "blockedFileExtension",
                ImmutableListMultimap.of()))
        .isTrue();
  }

  @Test
  public void missingEmailDoesNotMatchRegex() throws Exception {
    IdentifiedUser missingEmail = new FakeUserProvider().get(null);
    ValidatorConfig config =
        getConfig(
            "[plugin \"uploadvalidator\"]\n"
                + "   email = .*$\n"
                + "   blockedFileExtension = jar");

    assertThat(
            config.isEnabled(
                missingEmail,
                projectName,
                "refs/heads/anyref",
                "blockedFileExtension",
                ImmutableListMultimap.of()))
        .isFalse();
  }

  @Test
  public void isEnabledForMultipleEmails() throws Exception {
    IdentifiedUser exampleOrgUser = new FakeUserProvider().get("a@example.org");
    IdentifiedUser xUser = new FakeUserProvider().get("x@example.com");
    ValidatorConfig config =
        getConfig(
            "[plugin \"uploadvalidator\"]\n"
                + "   email = .*@example.org$\n"
                + "   email = x@example.com\n"
                + "   blockedFileExtension = jar");

    assertThat(
            config.isEnabled(
                exampleOrgUser,
                projectName,
                "refs/heads/anyref",
                "blockedFileExtension",
                ImmutableListMultimap.of()))
        .isTrue();
    assertThat(
        config.isEnabled(xUser,
          projectName,
          "refs/heads/anyref",
          "blockedFileExtension",
          ImmutableListMultimap.of()))
        .isTrue();
    assertThat(
            config.isEnabled(
                anyUser,
                projectName,
                "refs/heads/anyref",
                "blockedFileExtension",
                ImmutableListMultimap.of()))
        .isFalse();
  }

  private ValidatorConfig getConfig(String defaultConfig) throws ConfigInvalidException {
    return new ValidatorConfig(
        "uploadvalidator",
        new FakeConfigFactory(projectName, defaultConfig),
        new FakeGroupByNameFinder());
  }
}
