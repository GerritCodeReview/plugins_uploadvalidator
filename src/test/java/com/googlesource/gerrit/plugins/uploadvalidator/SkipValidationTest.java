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
import com.google.gerrit.reviewdb.client.AccountGroup;

import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.IdentifiedUser;

import org.junit.Test;

public class SkipValidationTest {
  private final Project.NameKey projectName =
      new Project.NameKey("testProject");
  private final IdentifiedUser anyUser = new FakeUserProvider().get();

  @Test
  public void dontSkipByDefault() throws Exception {
    ValidatorConfig validatorConfig =
        new ValidatorConfig(new FakeConfigFactory(projectName, ""),
            new FakeGroupCacheUUIDByName());

    assertThat(
        validatorConfig
            .isEnabledForRef(anyUser, projectName, "anyRef", "anyOp")).isTrue();
  }

  @Test
  public void skipWhenUserBelongsToGroupUUID() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n"
            + "skipValidation=testOp\n"
            + "skipGroup=testGroup\n"
            + "skipGroup=anotherGroup";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(new FakeConfigFactory(projectName, config),
            new FakeGroupCacheUUIDByName());

    assertThat(
        validatorConfig.isEnabledForRef(new FakeUserProvider("testGroup",
            "yetAnotherGroup").get(), projectName, "anyRef", "testOp"))
        .isFalse();
  }

  @Test
  public void skipWhenUserBelongsToGroupName() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n"
            + "skipValidation=testOp\n"
            + "skipGroup=testGroupName\n";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(new FakeConfigFactory(projectName, config),
            new FakeGroupCacheUUIDByName(new AccountGroup(
                new AccountGroup.NameKey("testGroupName"), new AccountGroup.Id(
                    1), new AccountGroup.UUID("testGroupId"))));

    assertThat(
        validatorConfig.isEnabledForRef(
            new FakeUserProvider("testGroupId").get(), projectName, "anyRef",
            "testOp")).isFalse();
  }

  @Test
  public void dontSkipWhenUserBelongsToOtherGroupsUUID() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n"
            + "skipValidation=testOp\n"
            + "skipGroup=testGroup\n"
            + "skipGroup=anotherGroup";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(new FakeConfigFactory(projectName, config),
            new FakeGroupCacheUUIDByName());

    assertThat(
        validatorConfig.isEnabledForRef(
            new FakeUserProvider("yetAnotherGroup").get(), projectName,
            "anyRef", "testOp")).isTrue();
  }

  @Test
  public void dontSkipForOtherOps() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n"
            + "skipValidation=testOp\n"
            + "skipGroup=testGroup\n"
            + "skipGroup=anotherGroup";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(new FakeConfigFactory(projectName, config),
            new FakeGroupCacheUUIDByName());

    assertThat(
        validatorConfig.isEnabledForRef(anyUser, projectName, "anyRef",
            "anotherOp")).isTrue();
  }

  @Test
  public void skipOnlyOnSpecificRef() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n"
            + "skipValidation=testOp\n"
            + "skipRef=refs/heads/myref\n"
            + "skipGroup=testGroup";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(new FakeConfigFactory(projectName, config),
            new FakeGroupCacheUUIDByName());

    assertThat(
        validatorConfig.isEnabledForRef(anyUser, projectName,
            "refs/heads/myref", "testOp")).isFalse();
  }

  @Test
  public void dontSkipOnOtherRefs() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n"
            + "skipValidation=testOp\n"
            + "skipRef=refs/heads/myref\n"
            + "skipGroup=testGroup";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(new FakeConfigFactory(projectName, config),
            new FakeGroupCacheUUIDByName());

    assertThat(
        validatorConfig.isEnabledForRef(anyUser, projectName,
            "refs/heads/anotherRef", "testOp")).isTrue();
  }
}
