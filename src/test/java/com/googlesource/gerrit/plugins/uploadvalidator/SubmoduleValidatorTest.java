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

import com.google.gerrit.server.git.validators.CommitValidationMessage;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubmoduleValidatorTest extends ValidatorTestCase {
  private RevCommit makeCommitWithSubmodule(RevWalk rw)
      throws IOException, GitAPIException {
    try (Git git = new Git(repo)) {
      SubmoduleAddCommand addCommand = git.submoduleAdd();
      addCommand.setURI(repo.getDirectory().getCanonicalPath());
      addCommand.setPath("modules/library");
      addCommand.call().close();
      git.add().addFilepattern(".").call();
      return rw.parseCommit(git.commit().setMessage("Commit with submodule.").call());
    }
  }

  @Test
  public void testWithSubmodule() throws Exception {
    try (RevWalk rw = new RevWalk(repo)) {
      RevCommit c = makeCommitWithSubmodule(rw);
      List<CommitValidationMessage> m =
          SubmoduleValidator.performValidation(repo, c, rw);
      assertThat(TestUtils.transformMessages(m))
          .containsExactly("ERROR: submodules are not allowed: modules/library");
    }
  }

  private RevCommit makeCommitWithoutSubmodule(RevWalk rw)
      throws IOException, GitAPIException {
    Map<File, byte[]> files = new HashMap<>();
    files.put(new File(repo.getDirectory().getParent(), "foo.txt"), null);
    return TestUtils.makeCommit(rw, repo, "Commit with empty test files.", files);
  }

  @Test
  public void testWithoutSubmodule() throws Exception {
    try (RevWalk rw = new RevWalk(repo)) {
      RevCommit c = makeCommitWithoutSubmodule(rw);
      List<CommitValidationMessage> m =
          SubmoduleValidator.performValidation(repo, c, rw);
      assertThat(m).isEmpty();
    }
  }

  @Test
  public void validatorInactiveWhenConfigEmpty() {
    assertThat(SubmoduleValidator.isActive(EMPTY_PLUGIN_CONFIG)).isFalse();
  }
}
