// Copyright (C) 2016 The Android Open Source Project
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

import com.google.gerrit.reviewdb.client.Project;

import org.junit.Test;

public class SkipValidationTest {
  private Project.NameKey projectName = new Project.NameKey("testProject");

  @Test
  public void dontSkipByDefault() throws Exception {
    ValidatorConfig validatorConfig =
        new ValidatorConfig(new FakeConfigFactory(projectName, ""),
            new FakeUserProvider());

    assertThat(validatorConfig.isEnabledForRef(projectName, "anyRef", "anyOp"))
        .isTrue();
  }

  @Test
  public void skipWhenUserBelongsToGroup() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n" + "skipValidation=testOp\n"
            + "skipGroup=testGroup\n" + "skipGroup=anotherGroup";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(new FakeConfigFactory(projectName, config),
            new FakeUserProvider("testGroup", "yetAnotherGroup"));

    assertThat(validatorConfig.isEnabledForRef(projectName, "anyRef", "testOp"))
        .isFalse();
  }

  @Test
  public void dontSkipWhenUserBelongsToOtherGroups() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n" + "skipValidation=testOp\n"
            + "skipGroup=testGroup\n" + "skipGroup=anotherGroup";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(new FakeConfigFactory(projectName, config),
            new FakeUserProvider("yetAnotherGroup"));

    assertThat(validatorConfig.isEnabledForRef(projectName, "anyRef", "testOp"))
        .isTrue();
  }

  @Test
  public void dontSkipForOtherOps() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n" + "skipValidation=testOp\n"
            + "skipGroup=testGroup\n" + "skipGroup=anotherGroup";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(new FakeConfigFactory(projectName, config),
            new FakeUserProvider("testGroup", "yetAnotherGroup"));

    assertThat(
        validatorConfig.isEnabledForRef(projectName, "anyRef", "anotherOp"))
        .isTrue();
  }

  @Test
  public void skipOnlyOnSpecificRef() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n" + "skipValidation=testOp\n"
            + "skipBranch=refs/heads/myref\n" + "skipGroup=testGroup";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(new FakeConfigFactory(projectName, config),
            new FakeUserProvider("testGroup"));

    assertThat(
        validatorConfig.isEnabledForRef(projectName, "refs/heads/myref",
            "testOp")).isFalse();
  }

  @Test
  public void dontSkipOnOtherRefs() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n" + "skipValidation=testOp\n"
            + "skipBranch=refs/heads/myref\n" + "skipGroup=testGroup";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(new FakeConfigFactory(projectName, config),
            new FakeUserProvider("testGroup"));

    assertThat(
        validatorConfig.isEnabledForRef(projectName, "refs/heads/anotherRef",
            "testOp")).isTrue();
  }
}
