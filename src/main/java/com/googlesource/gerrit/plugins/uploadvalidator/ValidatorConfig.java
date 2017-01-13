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
import com.google.gerrit.reviewdb.client.AccountGroup.UUID;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.GroupCache;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.project.RefPatternMatcher;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        && !hasCriteria(pluginConfig, "skipGroup")
        && !canSkipValidation(pluginConfig, refName, validatorOp));
  }

  private boolean isMatchingRefsPatterns(PluginConfig pluginConfig,
      String refName) {
    String[] refPatterns = pluginConfig.getStringList("ref");

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
    Optional<Boolean> skipValidation =
        matchCriteria(conf, "skipValidation", validation, false);
    Optional<Boolean> skipRef = matchCriteria(conf, "skipRef", refName, true);

    if (!skipValidation.orElse(true) || !skipRef.orElse(true)) {
      return false;
    }

    return canSkipOnGroup(conf);
  }

  private boolean hasCriteria(PluginConfig config, String criteria) {
    return config.getStringList(criteria).length > 0;
  }

  private Optional<Boolean> matchCriteria(
      PluginConfig config, String criteria, String value,
      boolean allowRegex) {
    String[] criteriaValues = config.getStringList(criteria);
    if (criteriaValues.length == 0) {
      return Optional.empty();
    }

    for (String s : criteriaValues) {
      if ((allowRegex && match(value, s)) || (!allowRegex && s.equals(value))) {
        return Optional.of(true);
      }
    }
    return Optional.of(false);
  }

  private boolean canSkipOnGroup(PluginConfig conf) {
    CurrentUser user = userProvider.get();
    if (!user.isIdentifiedUser()) {
      return false;
    }

    Stream<UUID> skipGroups =
        Arrays.stream(conf.getStringList("skipGroup")).map(this::groupUUID);

    return user.asIdentifiedUser().getEffectiveGroups()
        .containsAnyOf(skipGroups::iterator);
  }

  private AccountGroup.UUID groupUUID(String groupNameOrUUID) {
    Optional<AccountGroup.UUID> uuidFromCache =
        Optional.ofNullable(
            groupCache.get(new AccountGroup.NameKey(groupNameOrUUID))).map(
            group -> group.getGroupUUID());
    return uuidFromCache.orElse(new AccountGroup.UUID(groupNameOrUUID));
  }

  private static boolean match(String value, String pattern) {
    return RefPatternMatcher.getMatcher(pattern).match(value, null);
  }
}
