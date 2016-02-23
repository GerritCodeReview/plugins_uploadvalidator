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

import eu.medsea.mimeutil.MimeUtil2;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectStream;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class MimeTypeValidator extends ContentValidator {
  public static String KEY_BLOCKED_MIME_TYPE = "blockedMimeType";

  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final GitRepositoryManager repoManager;
  private static final MimeUtil2 mimeUtil = new MimeUtil2();
  static {
    mimeUtil.registerMimeDetector(
        "eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
  }

  @Inject
  MimeTypeValidator(@PluginName String pluginName,
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
      if (cfg.getStringList(KEY_BLOCKED_MIME_TYPE).length > 0) {
        StringBuilder badTypes = new StringBuilder();
        for (String t : cfg.getStringList(KEY_BLOCKED_MIME_TYPE)) {
          badTypes.append('|').append(t);
        }
        Pattern badTypesPattern = Pattern.compile(
            "^(" + badTypes.substring(1) + ")$");
        try (Repository repo = repoManager.openRepository(
            receiveEvent.project.getNameKey())) {
          List<CommitValidationMessage> messages = new LinkedList<>();
          Map<ObjectId, String> content = getContent(repo, receiveEvent.commit);
          for (ObjectId oid : content.keySet()) {
            ObjectLoader ol = repo.open(oid);
            try (ObjectStream os = ol.openStream()) {
              String type = MimeUtil2.getMostSpecificMimeType(
                  mimeUtil.getMimeTypes(os)).toString();
              if (badTypesPattern.matcher(type).matches()) {
                messages.add(new CommitValidationMessage(
                    "found blocked mime type in file: "
                        + content.get(oid), true));
              }
            }
          }
          if (!messages.isEmpty()) {
            throw new CommitValidationException(
                "contains blocked mime type", messages);
          }
        }
      }
    } catch (NoSuchProjectException | IOException e) {
      throw new CommitValidationException(
          "failed to check on mime type", e);
    }

    return Collections.emptyList();
  }
}
