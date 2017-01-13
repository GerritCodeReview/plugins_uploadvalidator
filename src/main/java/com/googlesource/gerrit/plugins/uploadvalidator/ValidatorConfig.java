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

import com.google.gerrit.common.data.RefConfigSection;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.GroupCache;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.project.RefPatternMatcher;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.Arrays;
import java.util.Optional;

public class ValidatorConfig {
  private final ConfigFactory configFactory;
  private final Provider<CurrentUser> userProvider;
  private final GroupCache groupCache;

  @Inject
  public ValidatorConfig(ConfigFactory configFactory,
      Provider<CurrentUser> userProvider,
      GroupCache groupCache) {
    this.configFactory = configFactory;
    this.userProvider = userProvider;
    this.groupCache = groupCache;
  }

  public boolean isEnabledForRef(Project.NameKey projectName, String refName,
      String validatorOp) {
    PluginConfig pluginConfig = configFactory.get(projectName);
    return (pluginConfig != null
        && isMatchingRefsPatterns(pluginConfig, refName)
        && !canSkipValidation(pluginConfig, refName, validatorOp));
  }

  private boolean isMatchingRefsPatterns(PluginConfig pluginConfig,
      String refName) {
    String[] refPatterns = pluginConfig.getStringList("branch");

    if (refPatterns.length == 0) {
      return true; // Default behavior: no branch-specific config
    }

    for (String refPattern : refPatterns) {
      if (RefConfigSection.isValid(refPattern) && match(refName, refPattern)) {
        return true;
      }
    }

    return false;
  }

  private boolean canSkipValidation(PluginConfig conf, String refName,
      String validation) {
    String[] skipGroups = conf.getStringList("skipGroup");
    Optional<Boolean> skipOnValidation =
        skipValidationBasedOn(conf, "skipValidation", validation);
    Optional<Boolean> skipOnBranch =
        skipValidationBasedOn(conf, "skipBranch", refName);
    if (skipGroups.length == 0
        || (skipOnValidation.isPresent() && !skipOnValidation.get())
        || (skipOnBranch.isPresent() && !skipOnBranch.get())) {
      return false;
    }

    return canSkipOnGroup(skipGroups);
  }

  private Optional<Boolean> skipValidationBasedOn(
      PluginConfig pluginProjectConfig, String criteria, String value) {
    String[] criteriaValues = pluginProjectConfig.getStringList(criteria);
    if (criteriaValues.length == 0) {
      return Optional.empty();
    }

    for (String s : criteriaValues) {
      if (s.equals(value)) {
        return Optional.of(true);
      }
    }
    return Optional.of(false);
  }

  private boolean canSkipOnGroup(String[] skipGroups) {
    if (skipGroups.length == 0) {
      return false;
    }

    CurrentUser user = userProvider.get();
    if (!user.isIdentifiedUser()) {
      return false;
    }

    return user
        .asIdentifiedUser()
        .getEffectiveGroups()
        .containsAnyOf(Arrays.stream(skipGroups).map(this::groupUUID)::iterator);
  }

  private AccountGroup.UUID groupUUID(String groupNameOrUUID) {
    Optional<AccountGroup.UUID> uuidFromCache =
        Optional.ofNullable(
            groupCache.get(new AccountGroup.NameKey(groupNameOrUUID))).map(
            group -> group.getGroupUUID());
    return uuidFromCache.orElse(new AccountGroup.UUID(groupNameOrUUID));
  }

  private static boolean match(String refName, String refPattern) {
    return RefPatternMatcher.getMatcher(refPattern).match(refName, null);
  }
}
