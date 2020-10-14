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

import com.google.gerrit.entities.AccountGroup;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.util.time.TimeUtil;
import org.junit.Test;

public class SkipValidationTest {
  private final Project.NameKey projectName = Project.nameKey("testProject");
  private final IdentifiedUser anyUser = new FakeUserProvider().get();

  @Test
  public void dontSkipByDefault() throws Exception {
    ValidatorConfig validatorConfig =
        new ValidatorConfig(new FakeConfigFactory(projectName, ""), new FakeGroupByNameFinder());

    assertThat(validatorConfig.isEnabled(anyUser, projectName, "anyRef", "anyOp")).isTrue();
  }

  @Test
  public void skipWhenUserBelongsToGroupUUID() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n"
            + "skipValidation=testOp\n"
            + "skipGroup=testGroup\n"
            + "skipGroup=anotherGroup";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(
            new FakeConfigFactory(projectName, config), new FakeGroupByNameFinder());

    assertThat(
            validatorConfig.isEnabled(
                new FakeUserProvider("testGroup", "yetAnotherGroup").get(),
                projectName,
                "anyRef",
                "testOp"))
        .isFalse();
  }

  @Test
  public void skipWhenUserBelongsToGroupName() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n" + "skipValidation=testOp\n" + "skipGroup=testGroupName\n";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(
            new FakeConfigFactory(projectName, config),
            new FakeGroupByNameFinder(
                AccountGroup.nameKey("testGroupName"),
                AccountGroup.id(1),
                AccountGroup.uuid("testGroupId"),
                TimeUtil.nowTs()));

    assertThat(
            validatorConfig.isEnabled(
                new FakeUserProvider("testGroupId").get(), projectName, "anyRef", "testOp"))
        .isFalse();
  }

  @Test
  public void dontSkipWhenUserBelongsToOtherGroupsUUID() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n"
            + "skipValidation=testOp\n"
            + "skipGroup=testGroup\n"
            + "skipGroup=anotherGroup";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(
            new FakeConfigFactory(projectName, config), new FakeGroupByNameFinder());

    assertThat(
            validatorConfig.isEnabled(
                new FakeUserProvider("yetAnotherGroup").get(), projectName, "anyRef", "testOp"))
        .isTrue();
  }

  @Test
  public void dontSkipForOtherOps() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n"
            + "skipValidation=testOp\n"
            + "skipGroup=testGroup\n"
            + "skipGroup=anotherGroup";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(
            new FakeConfigFactory(projectName, config), new FakeGroupByNameFinder());

    assertThat(validatorConfig.isEnabled(anyUser, projectName, "anyRef", "anotherOp")).isTrue();
  }

  @Test
  public void skipOnlyOnSpecificRef() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n"
            + "skipValidation=testOp\n"
            + "skipRef=refs/heads/myref\n"
            + "skipGroup=testGroup";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(
            new FakeConfigFactory(projectName, config), new FakeGroupByNameFinder());

    assertThat(
            validatorConfig.isEnabled(
                new FakeUserProvider("testGroup").get(), projectName, "refs/heads/myref", "testOp"))
        .isFalse();
  }

  @Test
  public void skipSpecificProject() throws Exception {
    String config = "[plugin \"uploadvalidator\"]\n" + "disabledProject=testProject";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(
            new FakeConfigFactory(projectName, config), new FakeGroupByNameFinder());

    assertThat(
            validatorConfig.isEnabled(
                new FakeUserProvider("testGroup").get(), projectName, "refs/heads/myref", "testOp"))
        .isFalse();
  }

  @Test
  public void dontSkipOnOtherRefs() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n"
            + "skipValidation=testOp\n"
            + "skipRef=refs/heads/myref\n"
            + "skipGroup=testGroup";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(
            new FakeConfigFactory(projectName, config), new FakeGroupByNameFinder());

    assertThat(validatorConfig.isEnabled(anyUser, projectName, "refs/heads/anotherRef", "testOp"))
        .isTrue();
  }
}
