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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class InvalidLineEndingValidator extends ContentValidator {
  public static String KEY_CHECK_RECJECT_WINDOWS_LINE_ENDINGS =
      "rejectWindowsLineEndings";

  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final GitRepositoryManager repoManager;

  @Inject
  InvalidLineEndingValidator(@PluginName String pluginName,
      PluginConfigFactory cfgFactory, GitRepositoryManager repoManager) {
    this.pluginName = pluginName;
    this.cfgFactory = cfgFactory;
    this.repoManager = repoManager;
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(
      CommitReceivedEvent receiveEvent) throws CommitValidationException {
    try {
      PluginConfig cfg =
          cfgFactory.getFromProjectConfig(
              receiveEvent.project.getNameKey(), pluginName);
      boolean lineEndingCheck = cfg.getBoolean(
          KEY_CHECK_RECJECT_WINDOWS_LINE_ENDINGS, false);

      if (lineEndingCheck) {
        try (Repository repo = repoManager.openRepository(
            receiveEvent.project.getNameKey())) {
          List<CommitValidationMessage> messages = new LinkedList<>();
          Map<ObjectId, String> content = getContent(repo, receiveEvent.commit);
          for(ObjectId oid : content.keySet()) {
            ObjectLoader ol = repo.open(oid);
            try (InputStreamReader isr = new InputStreamReader(ol.openStream(),
                StandardCharsets.UTF_8)) {
              char[] buffer = new char[1024];
              int n;
              NEXT_OBJECT:
                while ((n = isr.read(buffer, 0, buffer.length)) > 0) {
                  for (int x = n - 1; x >= 0; x--) {
                    if (buffer[x] == '\r') {
                      messages.add(new CommitValidationMessage(
                          "found carriage return (CR) character in file: "
                              + content.get(oid), true));
                      break NEXT_OBJECT;
                    }
                  }
                }
            }
          }
          if (!messages.isEmpty()) {
            throw new CommitValidationException(
                "contains files with a Windows line ending", messages);
          }
        }
      }
    } catch (NoSuchProjectException | IOException e) {
      throw new CommitValidationException(
          "failed to check on Windows line endings", e);
    }

    return Collections.emptyList();
  }
}
