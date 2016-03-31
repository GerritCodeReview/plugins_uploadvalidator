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

import com.google.gerrit.server.git.validators.CommitValidationMessage;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymlinkValidatorTest extends ValidatorTestCase {
  private RevCommit makeCommitWithSymlink()
      throws NoFilepatternException, IOException, GitAPIException {
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
        SymlinkValidator.performValidation(repo, c);
    assertThat(m).hasSize(2);
    List<ComparableCommitValidationMessage> expected = new ArrayList<>();
    expected.add(new ComparableCommitValidationMessage(
        "Symbolic links are not allowed: foo.txt", true));
    expected.add(new ComparableCommitValidationMessage(
        "Symbolic links are not allowed: symbolicFolder", true));
    assertThat(TestUtils.transformMessages(m)).containsAllIn(expected);
  }

  private RevCommit makeCommitWithoutSymlink()
      throws NoFilepatternException, IOException, GitAPIException {
    Map<File, byte[]> files = new HashMap<>();
    files.put(new File(repo.getDirectory().getParent(), "foo.txt"), null);
    return TestUtils.makeCommit(repo, "Commit with empty test files.", files);
  }

  @Test
  public void testWithoutSymlink() throws Exception {
    RevCommit c = makeCommitWithoutSymlink();
    List<CommitValidationMessage> m =
        SymlinkValidator.performValidation(repo, c);
    assertThat(m).isEmpty();
  }
}
