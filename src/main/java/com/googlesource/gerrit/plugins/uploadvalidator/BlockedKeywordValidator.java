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

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockedKeywordValidator implements CommitValidationListener {

  public static AbstractModule module() {
    return new AbstractModule() {

      @Override
      protected void configure() {
        DynamicSet.bind(binder(), CommitValidationListener.class)
            .to(BlockedKeywordValidator.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_CHECK_BLOCKED_KEYWORD_PATTERN))
            .toInstance(new ProjectConfigEntry("Blocked Keyword Pattern", null,
                ProjectConfigEntry.Type.ARRAY, null, false,
                "Pushes of commits that contain files with blocked keywords "
                    + "will be rejected."));
      }
    };
  }

  public static String KEY_CHECK_BLOCKED_KEYWORD_PATTERN =
      "blockedKeywordPattern";
  public static String CG_NAME = "blockedKeywordCaptureGroup";

  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final GitRepositoryManager repoManager;

  @Inject
  BlockedKeywordValidator(@PluginName String pluginName,
      PluginConfigFactory cfgFactory, GitRepositoryManager repoManager) {
    this.pluginName = pluginName;
    this.cfgFactory = cfgFactory;
    this.repoManager = repoManager;
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(
      CommitReceivedEvent receiveEvent) throws CommitValidationException {
    try {
      PluginConfig cfg = cfgFactory
          .getFromProjectConfig(receiveEvent.project.getNameKey(), pluginName);
      List<Pattern> blockedKeywordPatterns = new ArrayList<>();
      for (String s : cfg.getStringList(KEY_CHECK_BLOCKED_KEYWORD_PATTERN)) {
        blockedKeywordPatterns
            .add(Pattern.compile("(?<" + CG_NAME + ">" + s + ")"));
      }
      if (blockedKeywordPatterns.size() > 0) {
        try (Repository repo =
            repoManager.openRepository(receiveEvent.project.getNameKey())) {
          List<CommitValidationMessage> messages = performValidation(repo,
              receiveEvent.commit, blockedKeywordPatterns);
          if (!messages.isEmpty()) {
            throw new CommitValidationException(
                "includes files containing blocked keywords", messages);
          }
        }
      }
    } catch (NoSuchProjectException | IOException e) {
      throw new CommitValidationException("failed to check on blocked keywords",
          e);
    }
    return Collections.emptyList();
  }

  List<CommitValidationMessage> performValidation(Repository repo, RevCommit c,
      List<Pattern> blockedKeywordPatterns) throws IOException {
    List<CommitValidationMessage> messages = new LinkedList<>();
    Map<String, ObjectId> content = CommitUtils.getChangedContent(repo, c);
    for (String path : content.keySet()) {
      ObjectLoader ol = repo.open(content.get(path));
      checkFileForBlockedKeywords(blockedKeywordPatterns, messages, path, ol);
    }
    return messages;
  }

  private static void checkFileForBlockedKeywords(
      List<Pattern> blockedKeywordPatterns,
      List<CommitValidationMessage> messages, String path, ObjectLoader ol)
          throws IOException {
    try (BufferedReader br = new BufferedReader(
        new InputStreamReader(ol.openStream(), StandardCharsets.UTF_8))) {
      int line = 0;
      for (String l = br.readLine(); l != null; l = br.readLine()) {
        line++;
        checkLineForBlockedKeywords(blockedKeywordPatterns, messages, path,
            line, l);
      }
    }
  }

  private static void checkLineForBlockedKeywords(
      List<Pattern> invalidKeywordPatterns,
      List<CommitValidationMessage> messages, String path, int line, String l) {
    String found = "";
    for (Pattern p : invalidKeywordPatterns) {
      Matcher matcher = p.matcher(l);
      while (matcher.find()) {
        found = found + ", " + matcher.group(CG_NAME);
      }
    }
    if (!found.isEmpty()) {
      found = found.substring(2);
      messages.add(new CommitValidationMessage(
          "blocked keyword(s) found in file: " + path + " (Line: " + line + ")"
              + " (found: " + found + ")",
          true));
    }
  }
}
