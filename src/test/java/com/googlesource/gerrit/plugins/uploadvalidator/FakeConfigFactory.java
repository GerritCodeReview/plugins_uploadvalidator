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

import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.Project.NameKey;
import com.google.gerrit.server.config.PluginConfig;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;

public class FakeConfigFactory implements ConfigFactory {
  private final Config config;
  private final Project.NameKey projectName;

  public FakeConfigFactory(Project.NameKey projectName, String configText)
      throws ConfigInvalidException {
    this.config = new Config();
    this.config.fromText(configText);
    this.projectName = projectName;
  }

  @Override
  public PluginConfig get(NameKey projectName) {
    if (this.projectName.equals(projectName)) {
      return new PluginConfig("uploadvalidator", config);
    }

    return new PluginConfig("uploadvalidator", new Config());
  }
}
