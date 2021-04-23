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

public class ProjectAwareValidatorConfigTest {
  private Project.NameKey projectName = Project.nameKey("testProject");
  private IdentifiedUser anyUser = new FakeUserProvider().get();

  @Test
  public void isEnabledForAllProjectsByDefault() throws Exception {
    ValidatorConfig config =
        getConfig("[plugin \"uploadvalidator\"]\n" + "blockedFileExtension = jar", projectName);

    assertThat(config.isEnabled(anyUser, projectName, "anyRef", "blockedFileExtension")).isTrue();
  }

  @Test
  public void isEnabledForSingleProject() throws Exception {
    ValidatorConfig config =
        getConfig(
            "[plugin \"uploadvalidator\"]\n"
                + "   project = testProject\n"
                + "   blockedFileExtension = jar",
            projectName);

    assertThat(config.isEnabled(anyUser, projectName, "anyRef", "blockedFileExtension")).isTrue();
  }

  @Test
  public void isDisabledForInvalidProject() throws Exception {
    ValidatorConfig config =
        getConfig(
            "[plugin \"uploadvalidator\"]\n"
                + "   project = someOtherProject\n"
                + "   blockedFileExtension = jar",
            projectName);

    assertThat(config.isEnabled(anyUser, projectName, "anyRef", "blockedFileExtension")).isFalse();
  }

  @Test
  public void isEnabledForRegexProject() throws Exception {
    String configString =
        "[plugin \"uploadvalidator\"]\n"
            + "   project = test.*\n"
            + "   blockedFileExtension = jar";
    Project.NameKey otherNameKey = Project.nameKey("someOtherProject");
    ValidatorConfig config = getConfig(configString, projectName);
    ValidatorConfig config2 = getConfig(configString, otherNameKey);

    assertThat(config.isEnabled(anyUser, projectName, "anyRef", "blockedFileExtension")).isTrue();
    assertThat(config2.isEnabled(anyUser, otherNameKey, "anyRef", "blockedFileExtension"))
        .isFalse();
  }

  @Test
  public void isEnabledForMultipleProjects() throws Exception {
    String configString =
        "[plugin \"uploadvalidator\"]\n"
            + "   project = testProject\n"
            + "   project = another.*\n"
            + "   blockedFileExtension = jar";
    Project.NameKey anotherNameKey = Project.nameKey("anotherProject");
    Project.NameKey someOtherNameKey = Project.nameKey("someOtherProject");
    ValidatorConfig config = getConfig(configString, projectName);
    ValidatorConfig config2 = getConfig(configString, anotherNameKey);
    ValidatorConfig config3 = getConfig(configString, someOtherNameKey);

    assertThat(config.isEnabled(anyUser, projectName, "anyRef", "blockedFileExtension")).isTrue();
    assertThat(config2.isEnabled(anyUser, anotherNameKey, "anyRef", "blockedFileExtension"))
        .isTrue();
    assertThat(config3.isEnabled(anyUser, someOtherNameKey, "anyRef", "blockedFileExtension"))
        .isFalse();
  }

  private ValidatorConfig getConfig(String defaultConfig, Project.NameKey projName)
      throws ConfigInvalidException {
    return new ValidatorConfig(
        new FakeConfigFactory(projName, defaultConfig), new FakeGroupByNameFinder());
  }
}
