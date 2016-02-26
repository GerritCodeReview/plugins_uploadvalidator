// Copyright (C) 2016 The Android Open Source Project
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

import com.google.gerrit.server.config.PluginConfig;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;

public abstract class ValidatorTestCase {
  protected File repoFolder;
  protected Repository repo;
  protected PluginConfig cfg;

  @Before
  public void init() throws IOException {
    repoFolder = File.createTempFile("Git", "");
    repoFolder.delete();
    repo = TestUtils.createNewRepository(repoFolder);
    cfg = new PluginConfig("", new Config());
    initValidator();
  }

  @After
  public void cleanup() throws IOException {
    repo.close();
    if (repoFolder.exists()) {
      FileUtils.deleteDirectory(repoFolder);
    }
  }

  protected abstract void initValidator();
}
