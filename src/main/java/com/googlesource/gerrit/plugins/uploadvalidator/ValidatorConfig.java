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
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.gerrit.server.project.RefPatternMatcher;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorConfig {
  private static final Logger log = LoggerFactory.getLogger(ValidatorConfig.class);

  private final String pluginName;
  private final ProjectCache projectCache;
  private final PluginConfigFactory pluginCfgFactory;

  @Inject
  public ValidatorConfig(@PluginName String pluginName, ProjectCache projectCache,
      PluginConfigFactory pluginCfgFactory) {
    this.pluginName = pluginName;
    this.projectCache = projectCache;
    this.pluginCfgFactory = pluginCfgFactory;
  }

  public boolean isEnabled(Project.NameKey projectNK, String refName) {
    ProjectState projectState = projectCache.get(projectNK);
    if (projectState == null) {
      log.error("Failed to check if " + pluginName + " is enabled for project "
          + projectNK.get() + ": Project " + projectNK.get() + " not found");
      return false;
    }

    for (ProjectState parentState : projectState.treeInOrder()) {
      if (isEnabledForBranch(parentState, refName)) {
        return true;
      }
    }

    return isEnabledForBranch(projectState, refName);
  }

  private boolean isEnabledForBranch(ProjectState project, String refName) {
    String[] refPatterns =
        pluginCfgFactory.getFromProjectConfigWithInheritance(project,
            pluginName).getStringList("branch");
    for (String refPattern : refPatterns) {
      if (RefConfigSection.isValid(refPattern) && match(refName, refPattern)) {
        return true;
      }
    }
    return false;
  }

  private boolean match(String refName, String refPattern) {
    return RefPatternMatcher.getMatcher(refPattern).match(refName, null);
  }
}
