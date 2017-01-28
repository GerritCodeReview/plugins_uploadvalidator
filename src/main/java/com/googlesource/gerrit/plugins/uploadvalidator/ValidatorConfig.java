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
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.GroupCache;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.project.RefPatternMatcher;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Stream;

public class ValidatorConfig {
  private static final Logger log = LoggerFactory
      .getLogger(ValidatorConfig.class);
  private final ConfigFactory configFactory;
  private final GroupCache groupCache;

  @Inject
  public ValidatorConfig(ConfigFactory configFactory,
      GroupCache groupCache) {
    this.configFactory = configFactory;
    this.groupCache = groupCache;
  }

  public boolean isEnabledForRef(IdentifiedUser user,
      Project.NameKey projectName, String refName, String validatorOp) {
    PluginConfig conf = configFactory.get(projectName);

    return conf != null
        && isValidConfig(conf, projectName)
        && (activeForRef(conf, refName))
        && (!hasCriteria(conf, "skipGroup")
            || !canSkipValidation(conf, validatorOp)
            || !canSkipRef(conf, refName)
            || !canSkipGroup(conf, user));
  }

  private boolean isValidConfig(PluginConfig config, Project.NameKey projectName) {
    return hasValidConfigRef(config, "ref", projectName)
        && hasValidConfigRef(config, "skipRef", projectName);
  }

  private boolean hasValidConfigRef(PluginConfig config, String refKey,
      Project.NameKey projectName) {
    boolean valid = true;
    for (String refPattern : config.getStringList(refKey)) {
      if (!RefConfigSection.isValid(refPattern)) {
        log.error(
            "Invalid {} name/pattern/regex '{}' in {} project's plugin config",
            refKey, refPattern, projectName.get());
        valid = false;
      }
    }
    return valid;
  }

  private boolean hasCriteria(PluginConfig config, String criteria) {
    return config.getStringList(criteria).length > 0;
  }

  private boolean activeForRef(PluginConfig config, String ref) {
    return matchCriteria(config, "ref", ref, true);
  }

  private boolean canSkipValidation(PluginConfig config, String validatorOp) {
    return matchCriteria(config, "skipValidation", validatorOp, false);
  }

  private boolean canSkipRef(PluginConfig config, String ref) {
    return matchCriteria(config, "skipRef", ref, true);
  }

  private boolean matchCriteria(PluginConfig config, String criteria,
      String value, boolean allowRegex) {
    boolean match = true;
    for (String s : config.getStringList(criteria)) {
      if ((allowRegex && match(value, s)) || (!allowRegex && s.equals(value))) {
        return true;
      }
      match = false;
    }
    return match;
  }

  private static boolean match(String value, String pattern) {
    return RefPatternMatcher.getMatcher(pattern).match(value, null);
  }

  private boolean canSkipGroup(PluginConfig conf, IdentifiedUser user) {
    if (!user.isIdentifiedUser()) {
      return false;
    }

    Stream<AccountGroup.UUID> skipGroups =
        Arrays.stream(conf.getStringList("skipGroup")).map(this::groupUUID);
    return user.asIdentifiedUser().getEffectiveGroups()
        .containsAnyOf(skipGroups::iterator);
  }

  private AccountGroup.UUID groupUUID(String groupNameOrUUID) {
    AccountGroup group =
        groupCache.get(new AccountGroup.NameKey(groupNameOrUUID));
    if (group == null) {
      return new AccountGroup.UUID(groupNameOrUUID);
    }
    return group.getGroupUUID();
  }
}
