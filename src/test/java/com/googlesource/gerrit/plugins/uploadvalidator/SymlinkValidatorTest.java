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

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.uploadvalidator.TestUtils.EMPTY_PLUGIN_CONFIG;

import com.google.common.collect.ImmutableSet;
import com.google.gerrit.server.git.validators.CommitValidationMessage;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SymlinkValidatorTest extends ValidatorTestCase {
  private RevCommit makeCommitWithSymlink()
      throws IOException, GitAPIException {
    Map<File, byte[]> files = new HashMap<>();
    File link = new File(repo.getDirectory().getParent(), "foo.txt");
    Files.createSymbolicLink(link.toPath(), Paths.get("bar.txt"));
    files.put(link, null);

    link = new File(repo.getDirectory().getParent(), "symbolicFolder");
    Files.createSymbolicLink(link.toPath(), Paths.get("folder"));
    files.put(link, null);

    return TestUtils.makeCommit(repo, "Commit with symlink.", files);
  }

  @Test
  public void testWithSymlink() throws Exception {
    RevCommit c = makeCommitWithSymlink();
    List<CommitValidationMessage> m =
        SymlinkValidator.performValidation(repo, c, new RevWalk(repo));
    Set<String> expected = ImmutableSet.of(
        "ERROR: Symbolic links are not allowed: foo.txt",
        "ERROR: Symbolic links are not allowed: symbolicFolder");
    assertThat(TestUtils.transformMessages(m))
        .containsExactlyElementsIn(expected);
  }

  private RevCommit makeCommitWithoutSymlink()
      throws IOException, GitAPIException {
    Map<File, byte[]> files = new HashMap<>();
    files.put(new File(repo.getDirectory().getParent(), "foo.txt"), null);
    return TestUtils.makeCommit(repo, "Commit with empty test files.", files);
  }

  @Test
  public void testWithoutSymlink() throws Exception {
    RevCommit c = makeCommitWithoutSymlink();
    List<CommitValidationMessage> m =
        SymlinkValidator.performValidation(repo, c, new RevWalk(repo));
    assertThat(m).isEmpty();
  }

  @Test
  public void validatorInactiveWhenConfigEmpty() {
    assertThat(SymlinkValidator.isActive(EMPTY_PLUGIN_CONFIG)).isFalse();
  }
}
