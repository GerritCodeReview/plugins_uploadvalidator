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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MaxPathLengthValidatorTest extends ValidatorTestCase {
  private static final String TOO_LONG = "foo/bar/test/too/long.java";
  private static final String GOOD = "not/too/long.c";

  private static int getMaxPathLength() {
    return (TOO_LONG.length() + GOOD.length()) / 2;
  }

  private RevCommit makeCommit() throws IOException, GitAPIException {
    Set<File> files = new HashSet<>();
    files.add(TestUtils.createEmptyFile(TOO_LONG, repo));
    files.add(TestUtils.createEmptyFile(GOOD, repo));
    return TestUtils.makeCommit(repo, "Commit with empty test files.", files);
  }

  @Test
  public void testAddTooLongPath() throws Exception {
    RevCommit c = makeCommit();
    List<CommitValidationMessage> m =
        MaxPathLengthValidator.performValidation(repo, c, new RevWalk(repo),
            getMaxPathLength());
    Set<String> expected =
        ImmutableSet.of("ERROR: path too long: " + TOO_LONG);
    assertThat(TestUtils.transformMessages(m))
        .containsExactlyElementsIn(expected);
  }

  @Test
  public void testDeleteTooLongPath() throws Exception {
    RevCommit c = makeCommit();
    try(Git git = new Git(repo)) {
      Set<File> files = new HashSet<>();
      files.add(TestUtils.createEmptyFile(TOO_LONG, repo));
      TestUtils.removeFiles(git, files);
      c = git.commit().setMessage("Delete file which is too long").call();
    }
    List<CommitValidationMessage> m = MaxPathLengthValidator
        .performValidation(repo, c, new RevWalk(repo), getMaxPathLength());
    assertThat(m).isEmpty();
  }

  @Test
  public void validatorInactiveWhenConfigEmpty() {
    assertThat(MaxPathLengthValidator.isActive(EMPTY_PLUGIN_CONFIG)).isFalse();
  }
}
