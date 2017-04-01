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

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.uploadvalidator.TestUtils.EMPTY_PLUGIN_CONFIG;

import com.google.common.collect.Lists;
import com.google.gerrit.server.git.validators.CommitValidationMessage;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileExtensionValidatorTest extends ValidatorTestCase {
  private static final List<String> BLOCKED_EXTENSIONS_LC =
      Lists.newArrayList("jpeg", "pdf", "zip", "exe", "tar.gz");
  private static final List<String> BLOCKED_EXTENSIONS_UC =
      Lists.newArrayList("JPEG", "PDF", "ZIP", "EXE", "TAR.GZ");

  private RevCommit makeCommit(List<String> blockedExtensions)
      throws NoFilepatternException, IOException, GitAPIException {
    Set<File> files = new HashSet<>();
    for (String extension : blockedExtensions) {
      files.add(new File(repo.getDirectory().getParent(), "foo." + extension));
    }
    // valid extensions
    files.add(new File(repo.getDirectory().getParent(), "foo.java"));
    files.add(new File(repo.getDirectory().getParent(), "foo.core.tmp"));
    files.add(new File(repo.getDirectory().getParent(), "foo.c"));
    files.add(new File(repo.getDirectory().getParent(), "foo.txt"));
    return TestUtils.makeCommit(repo, "Commit with empty test files.", files);
  }

  @Test
  public void testBlockedExtensions() throws Exception {
    RevCommit c = makeCommit(BLOCKED_EXTENSIONS_LC);
    try (Revwalk rw = new RevWalk(repo)) {
      List<CommitValidationMessage> m = FileExtensionValidator
          .performValidation(repo, c, rw, BLOCKED_EXTENSIONS_LC);
      List<String> expected = new ArrayList<>();
      for (String extension : BLOCKED_EXTENSIONS_LC) {
        expected.add("ERROR: blocked file: foo." + extension);
      }
      assertThat(TestUtils.transformMessages(m))
          .containsExactlyElementsIn(expected);
    }
  }

  @Test
  public void testBlockedExtensionsCaseInsensitive() throws Exception {
    RevCommit c = makeCommit(BLOCKED_EXTENSIONS_UC);
    List<CommitValidationMessage> m = FileExtensionValidator
        .performValidation(repo, c, new RevWalk(repo), BLOCKED_EXTENSIONS_LC);
    List<String> expected = new ArrayList<>();
    for (String extension : BLOCKED_EXTENSIONS_UC) {
      expected.add("ERROR: blocked file: foo." + extension);
    }
    assertThat(TestUtils.transformMessages(m))
        .containsExactlyElementsIn(expected);
  }

  @Test
  public void validatorInactiveWhenConfigEmpty() {
    assertThat(FileExtensionValidator.isActive(EMPTY_PLUGIN_CONFIG)).isFalse();
  }
}
