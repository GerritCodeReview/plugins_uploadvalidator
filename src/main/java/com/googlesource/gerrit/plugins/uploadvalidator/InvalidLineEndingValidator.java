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

import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.annotations.PluginName;
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

import org.apache.commons.io.FilenameUtils;
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
import java.util.regex.Pattern;

public class InvalidLineEndingValidator
    implements CommitValidationListener, Validator {
  public static String KEY_CHECK_RECJECT_WINDOWS_LINE_ENDINGS =
      "rejectWindowsLineEndings";
  public static String KEY_IGNORE_FILES = "ignoreFilesWhenCheckLineEndings";

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
      if (!lineEndingCheck) {
        return Collections.emptyList();
      }
      try (Repository repo =
          repoManager.openRepository(receiveEvent.project.getNameKey())) {
        List<CommitValidationMessage> messages =
            performValidation(repo, receiveEvent.commit, cfg);
        if (!messages.isEmpty()) {
          throw new CommitValidationException(
              "contains files with a Windows line ending", messages);
        }
      }
    } catch (NoSuchProjectException | IOException e) {
      throw new CommitValidationException(
          "failed to check on Windows line endings", e);
    }
    return Collections.emptyList();
  }

  @Override
  public List<CommitValidationMessage> performValidation(Repository repo,
      RevCommit c, PluginConfig cfg) throws IOException {
    List<CommitValidationMessage> messages = new LinkedList<>();
    Pattern ignoreFiles = null;
    if (cfg.getString(KEY_IGNORE_FILES) != null
        && !cfg.getString(KEY_IGNORE_FILES).isEmpty()) {
      ignoreFiles = Pattern.compile(cfg.getString(KEY_IGNORE_FILES));
    }
    Map<String, ObjectId> content = CommitUtils.getChangedContent(repo, c);
    for (String path : content.keySet()) {
      if (ignoreFiles != null
          && ignoreFiles.matcher(FilenameUtils.getExtension(path)).find()) {
        continue;
      }
      ObjectLoader ol = repo.open(content.get(path));
      try (InputStreamReader isr = new InputStreamReader(ol.openStream(),
          StandardCharsets.UTF_8)) {
        if(doesInputStreanContainCR(isr)) {
          messages.add(new CommitValidationMessage(
              "found carriage return (CR) character in file: " + path, true));
        }
      }
    }
    return messages;
  }

  private boolean doesInputStreanContainCR(InputStreamReader isr)
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

  public static AbstractModule module() {
    return new AbstractModule() {

      @Override
      protected void configure() {
        DynamicSet.bind(binder(), CommitValidationListener.class)
            .to(InvalidLineEndingValidator.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(
                Exports.named(KEY_CHECK_RECJECT_WINDOWS_LINE_ENDINGS))
            .toInstance(new ProjectConfigEntry("Reject Windows Line Endings",
                "false", ProjectConfigEntry.Type.BOOLEAN, null, false,
                "Windows line endings. Pushes of commits that include files "
                    + "containing carriage return (CR) characters will be "
                    + "rejected."));

        DynamicSet.bind(binder(), CommitValidationListener.class)
            .to(InvalidLineEndingValidator.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_IGNORE_FILES))
            .toInstance(new ProjectConfigEntry(
                "Ignore Files During Windows Line Endings Check", null,
                ProjectConfigEntry.Type.STRING, null, false,
                "At the moment, there is no ideal solution to detect binary "
                    + "files. Because of that you can define file extensions, "
                    + "to prevent that this check validate this files."));
      }
    };
  }
}
