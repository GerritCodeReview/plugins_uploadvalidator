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
import com.google.gerrit.reviewdb.client.AccountGroup.UUID;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.project.RefPatternMatcher;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.Arrays;

public class ValidatorConfig {
  private final ConfigFactory pluginCfgFactory;
  private final Provider<CurrentUser> userProvider;

  @Inject
  public ValidatorConfig(ConfigFactory pluginCfgFactory,
      Provider<CurrentUser> userProvider) {
    this.pluginCfgFactory = pluginCfgFactory;
    this.userProvider = userProvider;
  }

  public boolean isEnabledForRef(Project.NameKey projectName, String refName,
      String validatorOp) {
    PluginConfig pluginProjectConfig = pluginCfgFactory.get(projectName);
    if (pluginProjectConfig == null
        || canSkipOnRef(pluginProjectConfig, refName, validatorOp)) {
      return false;
    }

    String[] refPatterns = pluginProjectConfig.getStringList("branch");
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

  private boolean canSkipOnRef(PluginConfig pluginProjectConfig,
      String refName, String validation) {
    if (!skipValidation(pluginProjectConfig, validation)) {
      return false;
    }

    String[] skipBranches = pluginProjectConfig.getStringList("skipBranch");
    if (skipBranches.length == 0) {
      return canSkipOnGroup(pluginProjectConfig);
    }

    for (String refPattern : skipBranches) {
      if (RefConfigSection.isValid(refPattern) && match(refName, refPattern)) {
        return canSkipOnGroup(pluginProjectConfig);
      }
    }

    return false;
  }

  private boolean skipValidation(PluginConfig pluginProjectConfig,
      String validation) {
    for (String s : pluginProjectConfig.getStringList("skipValidation")) {
      if (s.equals(validation)) {
        return true;
      }
    }
    return false;
  }

  private boolean canSkipOnGroup(PluginConfig pluginProjectConfig) {
    String[] skipGroups = pluginProjectConfig.getStringList("skipGroup");
    if (skipGroups.length == 0) {
      return false;
    }

    CurrentUser user = userProvider.get();
    if (!user.isIdentifiedUser()) {
      return false;
    }

    return user.asIdentifiedUser().getEffectiveGroups()
        .containsAnyOf(Arrays.stream(skipGroups).map(UUID::new)::iterator);
  }

  private static boolean match(String refName, String refPattern) {
    return RefPatternMatcher.getMatcher(refPattern).match(refName, null);
  }
}
