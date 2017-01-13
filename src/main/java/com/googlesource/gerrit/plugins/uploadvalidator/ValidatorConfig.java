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

import static com.googlesource.gerrit.plugins.uploadvalidator.OverrideUploadValidation.OVERRIDE_UPLOAD_VALIDATION;

import com.google.gerrit.common.data.RefConfigSection;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.CapabilityControl;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.gerrit.server.project.RefPatternMatcher;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorConfig {
  private static final Logger log = LoggerFactory
      .getLogger(ValidatorConfig.class);

  private final String pluginName;
  private final ProjectCache projectCache;
  private final PluginConfigFactory pluginCfgFactory;
  private final Provider<CurrentUser> userProvider;

  @Inject
  public ValidatorConfig(@PluginName String pluginName,
      ProjectCache projectCache, PluginConfigFactory pluginCfgFactory,
      Provider<CurrentUser> userProvider) {
    this.pluginName = pluginName;
    this.projectCache = projectCache;
    this.pluginCfgFactory = pluginCfgFactory;
    this.userProvider = userProvider;
  }

  public boolean isEnabledForRef(Project.NameKey projectName, String refName) {
    if(canOverrideUploadValidation()) {
      return false;
    }

    ProjectState projectState = projectCache.get(projectName);
    if (projectState == null) {
      log.error("Failed to check if " + pluginName + " is enabled for project "
          + projectName.get() + ": Project " + projectName.get() + " not found");
      return false;
    }

    String[] refPatterns =
        pluginCfgFactory.getFromProjectConfigWithInheritance(projectState,
            pluginName).getStringList("branch");
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

  private boolean canOverrideUploadValidation() {
    CapabilityControl ctl = userProvider.get().getCapabilities();
    return ctl.canPerform(pluginName + "-" + OVERRIDE_UPLOAD_VALIDATION);
  }

  private static boolean match(String refName, String refPattern) {
    return RefPatternMatcher.getMatcher(refPattern).match(refName, null);
  }
}
