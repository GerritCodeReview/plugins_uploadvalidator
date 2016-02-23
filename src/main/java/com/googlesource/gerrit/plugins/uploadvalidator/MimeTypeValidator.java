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
import org.eclipse.jgit.lib.ObjectStream;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MimeTypeValidator implements CommitValidationListener {

  public static AbstractModule module() {
    return new AbstractModule() {

      @Override
      protected void configure() {
        DynamicSet.bind(binder(), CommitValidationListener.class)
            .to(MimeTypeValidator.class);
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_BLOCKED_MIME_TYPE))
            .toInstance(new ProjectConfigEntry("Blocked Mime Type", null,
                ProjectConfigEntry.Type.ARRAY, null, false,
                "Pushes of commits that contain files with blocked mime types "
                    + "will be rejected."));
        bind(ProjectConfigEntry.class)
            .annotatedWith(Exports.named(KEY_BLOCKED_MIME_TYPE_WHITELIST))
            .toInstance(new ProjectConfigEntry("Blocked Mime Type Whitelist",
                "false", ProjectConfigEntry.Type.BOOLEAN, null, false,
                "If this option is checked, the entered mime types are "
                    + "interpreted as a whitelist. Otherwise commits that "
                    + "contain one of these mime types will be rejected."));
      }
    };
  }

  public static String KEY_BLOCKED_MIME_TYPE = "blockedMimeType";
  public static String KEY_BLOCKED_MIME_TYPE_WHITELIST =
      "blockedMimeTypeWhitelist";

  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final GitRepositoryManager repoManager;
  private final ContentTypeUtil contentTypeUtil;

  @Inject
  MimeTypeValidator(@PluginName String pluginName,
      ContentTypeUtil contentTypeUtil, PluginConfigFactory cfgFactory,
      GitRepositoryManager repoManager) {
    this.pluginName = pluginName;
    this.contentTypeUtil = contentTypeUtil;
    this.cfgFactory = cfgFactory;
    this.repoManager = repoManager;
  }

  private static String[] getBlockedTypes(PluginConfig cfg) {
    return cfg.getStringList(KEY_BLOCKED_MIME_TYPE);
  }

  static boolean isWhitelist(PluginConfig cfg) {
    return cfg.getBoolean(KEY_BLOCKED_MIME_TYPE_WHITELIST, false);
  }

  static boolean doCheckMimeType(PluginConfig cfg) {
    return getBlockedTypes(cfg).length > 0;
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(
      CommitReceivedEvent receiveEvent) throws CommitValidationException {
    try {
      PluginConfig cfg = cfgFactory
          .getFromProjectConfig(receiveEvent.project.getNameKey(), pluginName);
      if (!doCheckMimeType(cfg)) {
        return Collections.emptyList();
      }
      try (Repository repo =
          repoManager.openRepository(receiveEvent.project.getNameKey())) {
        List<CommitValidationMessage> messages =
            performValidation(repo, receiveEvent.commit, getBlockedTypes(cfg),
                isWhitelist(cfg), contentTypeUtil);
        if (!messages.isEmpty()) {
          throw new CommitValidationException("contains blocked mime type",
              messages);
        }
      }
    } catch (NoSuchProjectException | IOException | ExecutionException e) {
      throw new CommitValidationException("failed to check on mime type", e);
    }
    return Collections.emptyList();
  }

  static List<CommitValidationMessage> performValidation(Repository repo,
      RevCommit c, String[] blockedTypes, boolean whitelist,
      ContentTypeUtil contentTypeUtil) throws IOException, ExecutionException {
    List<CommitValidationMessage> messages = new LinkedList<>();
    Map<String, ObjectId> content = CommitUtils.getChangedContent(repo, c);
    for (String path : content.keySet()) {
      ObjectLoader ol = repo.open(content.get(path));
      try (ObjectStream os = ol.openStream()) {
        String contentType = contentTypeUtil.getContentType(os, path);
        if ((contentTypeUtil.doesTypeMatch(contentType, blockedTypes)
            && !whitelist)
            || !contentTypeUtil.doesTypeMatch(contentType, blockedTypes)
                && whitelist) {
          messages.add(new CommitValidationMessage(
              "found blocked mime type (" + contentType + ") in file: " + path,
              true));
        }
      }
    }
    return messages;
  }
}
