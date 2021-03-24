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

import com.google.gerrit.entities.Project;
import com.google.gerrit.server.IdentifiedUser;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.junit.Test;

public class RefAwareValidatorConfigTest {
  private final Project.NameKey projectName = Project.nameKey("testProject");
  private final IdentifiedUser anyUser = new FakeUserProvider().get();

  @Test
  public void isEnabledForAllRefsByDefault() throws Exception {
    ValidatorConfig config =
        getConfig("[plugin \"uploadvalidator\"]\n" + "blockedFileExtension = jar");

    assertThat(config.isEnabled(anyUser, projectName, "anyRef", "blockedFileExtension")).isTrue();
  }

  @Test
  public void isEnabledForSingleRef() throws Exception {
    ValidatorConfig config =
        getConfig(
            "[plugin \"uploadvalidator\"]\n"
                + "   ref = refs/heads/anyref\n"
                + "   blockedFileExtension = jar");

    assertThat(config.isEnabled(anyUser, projectName, "refs/heads/anyref", "blockedFileExtension"))
        .isTrue();
  }

  @Test
  public void isDisabledForInvalidRef() throws Exception {
    ValidatorConfig config =
        getConfig(
            "[plugin \"uploadvalidator\"]\n"
                + "   ref = anInvalidRef\n"
                + "   blockedFileExtension = jar");

    assertThat(config.isEnabled(anyUser, projectName, "refs/heads/anyref", "blockedFileExtension"))
        .isFalse();
  }

  @Test
  public void isEnabledForRegexRef() throws Exception {
    ValidatorConfig config =
        getConfig(
            "[plugin \"uploadvalidator\"]\n"
                + "   ref = ^refs/heads/mybranch.*\n"
                + "   blockedFileExtension = jar");

    assertThat(
            config.isEnabled(anyUser, projectName, "refs/heads/anotherref", "blockedFileExtension"))
        .isFalse();
    assertThat(
            config.isEnabled(
                anyUser, projectName, "refs/heads/mybranch123", "blockedFileExtension"))
        .isTrue();
  }

  @Test
  public void isEnabledForMultipleRefs() throws Exception {
    ValidatorConfig config =
        getConfig(
            "[plugin \"uploadvalidator\"]\n"
                + "   ref = refs/heads/branch1\n"
                + "   ref = refs/heads/branch2\n"
                + "   blockedFileExtension = jar");

    assertThat(config.isEnabled(anyUser, projectName, "refs/heads/branch1", "blockedFileExtension"))
        .isTrue();
    assertThat(config.isEnabled(anyUser, projectName, "refs/heads/branch2", "blockedFileExtension"))
        .isTrue();
    assertThat(config.isEnabled(anyUser, projectName, "refs/heads/branch3", "blockedFileExtension"))
        .isFalse();
  }

  private ValidatorConfig getConfig(String defaultConfig) throws ConfigInvalidException {
    return new ValidatorConfig(
        new FakeConfigFactory(projectName, defaultConfig), new FakeGroupByNameFinder());
  }
}
