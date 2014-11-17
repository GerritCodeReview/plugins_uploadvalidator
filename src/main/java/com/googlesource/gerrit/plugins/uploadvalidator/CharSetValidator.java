// Copyright (C) 2014 The Android Open Source Project
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
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;

import java.util.Collections;
import java.util.List;
import java.util.regex.*;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharacterCodingException;

public class CharSetValidator implements CommitValidationListener {
  public static String CHARSET_VALIDATOR = "charSetValidator";

  private final String pluginName;
  private final PluginConfigFactory cfgFactory;
  private final GitRepositoryManager repoManager;
  private boolean fastUtf8Check;
  private boolean validateUtf8;
  private Pattern okCharPattern;
  private String regexpPattern;
  private String rejectReasonBranch;
  private String rejectReasonCharset;
  private String internalErrorMessage;

  @Inject
  CharSetValidator(@PluginName String pluginName,
      PluginConfigFactory cfgFactory, GitRepositoryManager repoManager) {
    this.pluginName = pluginName;
    this.cfgFactory = cfgFactory;
    this.repoManager = repoManager;
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(
      CommitReceivedEvent receiveEvent) throws CommitValidationException {
    Repository repo = null;
    try {
      readConfiguration();

      if (validateUtf8) {
        boolean isValid;

        if (fastUtf8Check) {
          isValid = validateCommit(receiveEvent.commit.getRawBuffer());
        } else {
          isValid = isValidUTF8(receiveEvent.commit.getRawBuffer());
        }
        if (!isValid) {
          throw new CommitValidationException(rejectReasonCharset);
        }
      }

      repo = repoManager.openRepository(receiveEvent.project.getNameKey());
      if (repo.getRef(receiveEvent.refName) == null) {
        Matcher m = okCharPattern.matcher(receiveEvent.refName);
        if (!m.matches()) {
          throw new CommitValidationException(rejectReasonBranch);
        }
      }
    } catch (IOException e) {
      throw new CommitValidationException(internalErrorMessage, e);
    } finally {
      if (repo != null) {
        repo.close();
      }
    }

    return Collections.emptyList();
  }

  private void readConfiguration() {
    String configEntry = cfgFactory.getFromGerritConfig(CHARSET_VALIDATOR)
      .getString("branch_regex", "^[a-z0-9_\\-/]+$");
    if (regexpPattern == null || regexpPattern.hashCode() != configEntry.hashCode()) {
      okCharPattern = Pattern.compile(configEntry);
      regexpPattern = configEntry;
    }
    rejectReasonBranch = cfgFactory.getFromGerritConfig(CHARSET_VALIDATOR)
      .getString("branch_reject_reason", "Sorry, your branch is not valid.");
    rejectReasonCharset = cfgFactory.getFromGerritConfig(CHARSET_VALIDATOR)
      .getString("charset_reject_reason", "Sorry, your commit has non UTF8 content.");
    internalErrorMessage = cfgFactory.getFromGerritConfig(CHARSET_VALIDATOR)
      .getString("internal_error_message", "CharSetValidator failed to validate your commit.");
    fastUtf8Check = cfgFactory.getFromGerritConfig(CHARSET_VALIDATOR)
      .getBoolean("fast_utf8_check", true);
    validateUtf8 = cfgFactory.getFromGerritConfig(CHARSET_VALIDATOR)
      .getBoolean("validate_utf8", true);
  }

  private boolean isValidUTF8(byte[] input) {
    CharsetDecoder cs = Charset.forName("UTF-8").newDecoder();

    try {
      cs.decode(ByteBuffer.wrap(input));
      return true;
    } catch(CharacterCodingException e) {
      return false;
    }
  }

  private boolean validateLine(byte[] line, int lineStart, int lineEnd) {
    for (; lineStart < lineEnd; lineStart++) {
      if ((line[lineStart] & 128) > 0) {
        int utf8Index = lineStart++;
        int utf8Bit = 64;

        /* UTF8 format:
          First byte contains "count" 1 bits and a zero bit - the count is 2-4 and
          denotes the number of bytes followig that is part of that character.
          Each following byte has "10" as upper bits.
          Examples:
            11000010 10100010
            11100010 10000010 10101100
            11110000 10100100 10101101 10100010
        */

        // Is the seventh bit set on the UTF8 start character?
        if ((line[utf8Index] & utf8Bit) == 0) {
          return false;
        }

        for (;  (lineStart - utf8Index) < 4 &&   // Max size of a UTF8 character is 4 bytes
          (line[utf8Index] & utf8Bit) > 0 &&  // Still within the UTF8 character
          (line[lineStart] & 128) > 0 &&    // Eight bit is set
          (line[lineStart] & 64) == 0    // Seventh bit is *NOT* set
        ; lineStart++, utf8Bit >>= 1);

        // Roll back one iteration
        utf8Bit <<= 1;
        lineStart--;

        if ((lineStart - utf8Index) == 0) {
          return false;
        }

        if ((line[utf8Index] & utf8Bit) > 0 && (line[lineStart] & 128) == 0) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean validateCommit(byte[] commit) {
    boolean returnValue = true;

    for (int offset = 0; returnValue && offset < commit.length; offset++) {
      int lineEnd = 0;

      for (lineEnd = offset; lineEnd < commit.length && commit[lineEnd] != '\n'; lineEnd++);

      switch (commit[offset]) {
        case 'a': // Check authors
        case 'c': // Check commiters
          returnValue = validateLine(commit, offset, lineEnd);
          break;
        case 't': // Don't check tree
        case 'p': // Don't check parent
          break;
        default:
          return returnValue;
      }
      offset = lineEnd;
    }
    return returnValue;
  }
}
