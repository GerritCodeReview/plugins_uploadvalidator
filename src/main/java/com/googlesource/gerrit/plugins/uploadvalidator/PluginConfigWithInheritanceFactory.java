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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;

public class PluginConfigWithInheritanceFactory implements ConfigFactory {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final PluginConfigFactory pluginConfigFactory;
  private final String pluginName;

  @Inject
  public PluginConfigWithInheritanceFactory(PluginConfigFactory pcf, @PluginName String pn) {
    this.pluginConfigFactory = pcf;
    this.pluginName = pn;
  }

  @Override
  public PluginConfig get(Project.NameKey projectName) {
    try {
      return pluginConfigFactory.getFromProjectConfigWithInheritance(projectName, pluginName);
    } catch (NoSuchProjectException e) {
      logger.atWarning().log("%s not found", projectName.get());
      return null;
    }
  }
}
