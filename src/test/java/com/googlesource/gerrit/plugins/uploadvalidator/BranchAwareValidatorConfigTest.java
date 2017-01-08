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

import org.junit.Test;

public class BranchAwareValidatorConfigTest {
  private String pluginName = "uploadvalidator";
  private Project.NameKey projectName = new Project.NameKey("testProject");

  @Test
  public void isEnabledForAllBranchesByDefault() throws Exception {
    String defaultConfig =
        "[plugin \"uploadvalidator\"]\n" + "blockedFileExtension = jar";
    ValidatorConfig config =
        new ValidatorConfig(pluginName, new FakeConfigFactory(projectName,
            defaultConfig));

    assertThat(config.isEnabledForRef(projectName, "anyRef")).isTrue();
  }

  @Test
  public void isEnabledForSingleBranch() throws Exception {
    String defaultConfig =
        "[plugin \"uploadvalidator\"]\n" + "   branch = anyRef\n"
            + "   blockedFileExtension = jar";
    ValidatorConfig config =
        new ValidatorConfig(pluginName, new FakeConfigFactory(projectName,
            defaultConfig));

    assertThat(config.isEnabledForRef(projectName, "anyRef")).isFalse();
  }

  @Test
  public void isEnabledForRegexBranch() throws Exception {
    String defaultConfig =
        "[plugin \"uploadvalidator\"]\n"
            + "   branch = ^refs/heads/mybranch.*\n"
            + "   blockedFileExtension = jar";
    ValidatorConfig config =
        new ValidatorConfig(pluginName, new FakeConfigFactory(projectName,
            defaultConfig));

    assertThat(config.isEnabledForRef(projectName, "refs/heads/anotherref"))
        .isFalse();
    assertThat(config.isEnabledForRef(projectName, "refs/heads/mybranch123"))
        .isTrue();
  }

  @Test
  public void isEnabledForMultipleBranches() throws Exception {
    String defaultConfig =
        "[plugin \"uploadvalidator\"]\n" + "   branch = refs/heads/branch1\n"
            + "   branch = refs/heads/branch2\n"
            + "   blockedFileExtension = jar";
    ValidatorConfig config =
        new ValidatorConfig(pluginName, new FakeConfigFactory(projectName,
            defaultConfig));

    assertThat(config.isEnabledForRef(projectName, "refs/heads/branch1"))
        .isTrue();
    assertThat(config.isEnabledForRef(projectName, "refs/heads/branch2"))
        .isTrue();
    assertThat(config.isEnabledForRef(projectName, "refs/heads/branch3"))
        .isFalse();
  }
}
