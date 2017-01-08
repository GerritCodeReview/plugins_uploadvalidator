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

import com.google.gerrit.reviewdb.client.Project;

import static com.google.common.truth.Truth.*;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.junit.Test;

public class BranchAwareValidatorConfigTest {
  private Project.NameKey projectName = new Project.NameKey("testProject");

  @Test
  public void isEnabledForAllBranchesByDefault() throws Exception {
    ValidatorConfig config =
        getConfig("[plugin \"uploadvalidator\"]\n"
            + "blockedFileExtension = jar");

    assertThat(config.isEnabledForRef(projectName, "anyRef")).isTrue();
  }

  @Test
  public void isEnabledForSingleBranch() throws Exception {
    ValidatorConfig config =
        getConfig("[plugin \"uploadvalidator\"]\n"
            + "   branch = refs/heads/anyref\n"
            + "   blockedFileExtension = jar");

    assertThat(config.isEnabledForRef(projectName, "refs/heads/anyref"))
      .isTrue();
  }

  @Test
  public void isEnabledForRegexBranch() throws Exception {
    ValidatorConfig config =
        getConfig("[plugin \"uploadvalidator\"]\n"
            + "   branch = ^refs/heads/mybranch.*\n"
            + "   blockedFileExtension = jar");

    assertThat(config.isEnabledForRef(projectName, "refs/heads/anotherref"))
        .isFalse();
    assertThat(config.isEnabledForRef(projectName, "refs/heads/mybranch123"))
        .isTrue();
  }

  @Test
  public void isEnabledForMultipleBranches() throws Exception {
    ValidatorConfig config =
        getConfig("[plugin \"uploadvalidator\"]\n"
            + "   branch = refs/heads/branch1\n"
            + "   branch = refs/heads/branch2\n"
            + "   blockedFileExtension = jar");

    assertThat(config.isEnabledForRef(projectName, "refs/heads/branch1"))
        .isTrue();
    assertThat(config.isEnabledForRef(projectName, "refs/heads/branch2"))
        .isTrue();
    assertThat(config.isEnabledForRef(projectName, "refs/heads/branch3"))
        .isFalse();
  }

  private ValidatorConfig getConfig(String defaultConfig)
      throws ConfigInvalidException {
    ValidatorConfig config =
        new ValidatorConfig(new FakeConfigFactory(projectName, defaultConfig));
    return config;
  }
}
