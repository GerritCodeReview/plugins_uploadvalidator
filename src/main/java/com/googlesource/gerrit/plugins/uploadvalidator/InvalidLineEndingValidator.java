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

import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.api.projects.ProjectConfigEntryType;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.ProjectConfigEntry;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class InvalidLineEndingValidator implements CommitValidationListener {

  public static AbstractModule module() {
    return new AbstractModule() {

      @Override
      protected void configure() {
        DynamicSet.bind(binder(), CommitValidationListener.class)
            .to(InvalidLineEndingValidator.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(
                Exports.named(KEY_CHECK_REJECT_WINDOWS_LINE_ENDINGS))
            .toInstance(new ProjectConfigEntry("Reject Windows Line Endings",
                "false", ProjectConfigEntryType.BOOLEAN, null, false,
                "Windows line endings. Pushes of commits that include files "
                    + "containing carriage return (CR) characters will be "
                    + "rejected."));
      }
    };
  }

  public static String KEY_CHECK_REJECT_WINDOWS_LINE_ENDINGS =
      "rejectWindowsLineEndings";

  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final GitRepositoryManager repoManager;
  private final ContentTypeUtil contentTypeUtil;
  private final ValidatorConfig validatorConfig;

  @Inject
  InvalidLineEndingValidator(@PluginName String pluginName,
      ContentTypeUtil contentTypeUtil,
      PluginConfigFactory cfgFactory,
      GitRepositoryManager repoManager,
      ValidatorConfig validatorConfig) {
    this.pluginName = pluginName;
    this.cfgFactory = cfgFactory;
    this.repoManager = repoManager;
    this.contentTypeUtil = contentTypeUtil;
    this.validatorConfig = validatorConfig;
  }

  static boolean isActive(PluginConfig cfg) {
    return cfg.getBoolean(KEY_CHECK_REJECT_WINDOWS_LINE_ENDINGS, false);
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(
      CommitReceivedEvent receiveEvent) throws CommitValidationException {
    try {
      PluginConfig cfg =
          cfgFactory.getFromProjectConfigWithInheritance(
              receiveEvent.project.getNameKey(), pluginName);
      if (isActive(cfg)
          && validatorConfig.isEnabledForRef(receiveEvent.getProjectNameKey(),
              receiveEvent.getRefName(), KEY_CHECK_REJECT_WINDOWS_LINE_ENDINGS)) {
        try (Repository repo =
            repoManager.openRepository(receiveEvent.project.getNameKey())) {
          List<CommitValidationMessage> messages =
              performValidation(repo, receiveEvent.commit, cfg);
          if (!messages.isEmpty()) {
            throw new CommitValidationException(
                "contains files with a Windows line ending", messages);
          }
        }
      }
    } catch (NoSuchProjectException | IOException | ExecutionException e) {
      throw new CommitValidationException(
          "failed to check on Windows line endings", e);
    }
    return Collections.emptyList();
  }

  @VisibleForTesting
  List<CommitValidationMessage> performValidation(Repository repo, RevCommit c,
      PluginConfig cfg) throws IOException, ExecutionException {
    List<CommitValidationMessage> messages = new LinkedList<>();
    Map<String, ObjectId> content = CommitUtils.getChangedContent(repo, c);
    for (String path : content.keySet()) {
      ObjectLoader ol = repo.open(content.get(path));
      if (contentTypeUtil.isBinary(ol, path, cfg)) {
        continue;
      }
      try (InputStreamReader isr =
          new InputStreamReader(ol.openStream(), StandardCharsets.UTF_8)) {
        if (doesInputStreanContainCR(isr)) {
          messages.add(new CommitValidationMessage(
              "found carriage return (CR) character in file: " + path, true));
        }
      }
    }
    return messages;
  }

  private static boolean doesInputStreanContainCR(InputStreamReader isr)
      throws IOException {
    char[] buffer = new char[1024];
    int n;
    while ((n = isr.read(buffer, 0, buffer.length)) > 0) {
      for (int x = n - 1; x >= 0; x--) {
        if (buffer[x] == '\r') {
          return true;
        }
      }
    }
    return false;
  }
}
