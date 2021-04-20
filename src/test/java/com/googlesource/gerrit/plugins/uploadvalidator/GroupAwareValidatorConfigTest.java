// Copyright (C) 2021 The Android Open Source Project
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
import com.google.gerrit.entities.AccountGroup;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.util.time.TimeUtil;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.junit.Test;

public class GroupAwareValidatorConfigTest {
  private Project.NameKey projectName = Project.nameKey("testProject");
  private static final String pluginName = "uploadvalidator";

  @Test
  public void isEnabledForNoGroupsByDefault() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n"
            + "blockedFileExtension = jar\n";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(
            pluginName,
            new FakeConfigFactory(projectName, config),
            new FakeGroupByNameFinder());

    assertThat(validatorConfig.isEnabled(
        new FakeUserProvider().get(),
        projectName,
        "anyRef",
        "blockedFileExtension",
        ImmutableListMultimap.of()))
        .isTrue();
  }

  @Test
  public void isEnabledWhenUserBelongsToOneGroup() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n"
            + "blockedFileExtension = jar\n"
            + "group=fooGroup\n"
            + "group=barGroup\n";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(
            pluginName,
            new FakeConfigFactory(projectName, config),
            new FakeGroupByNameFinder());

    assertThat(
            validatorConfig.isEnabled(
                new FakeUserProvider("fooGroup", "bazGroup").get(),
                projectName,
                "anyRef",
                "blockedFileExtension",
                ImmutableListMultimap.of()))
        .isTrue();
  }

  @Test
  public void isEnabledWhenUserInGroupUUID() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n"
            + "blockedFileExtension = jar\n"
            + "group=testGroupName\n";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(
            pluginName,
            new FakeConfigFactory(projectName, config),
            new FakeGroupByNameFinder(
                AccountGroup.nameKey("testGroupName"),
                AccountGroup.id(1),
                AccountGroup.uuid("testGroupId"),
                TimeUtil.nowTs()));

    assertThat(
            validatorConfig.isEnabled(
                new FakeUserProvider("testGroupId").get(),
                projectName,
                "anyRef",
                "blockedFileExtension",
                ImmutableListMultimap.of()))
        .isTrue();
  }

  @Test
  public void isDisabledWhenUserNotInGroup() throws Exception {
    String config =
        "[plugin \"uploadvalidator\"]\n"
            + "blockedFileExtension = jar\n"
            + "group=fooGroup\n"
            + "group=barGroup\n";

    ValidatorConfig validatorConfig =
        new ValidatorConfig(
            pluginName,
            new FakeConfigFactory(projectName, config),
            new FakeGroupByNameFinder());

    assertThat(
            validatorConfig.isEnabled(
                new FakeUserProvider("bazGroup").get(),
                projectName,
                "anyRef",
                "blockedFileExtension",
                ImmutableListMultimap.of()))
        .isFalse();
  }
}
