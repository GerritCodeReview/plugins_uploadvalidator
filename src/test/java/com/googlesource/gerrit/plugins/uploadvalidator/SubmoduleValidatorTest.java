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

import com.google.common.collect.ImmutableSet;
import com.google.gerrit.server.git.validators.CommitValidationMessage;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubmoduleValidatorTest extends ValidatorTestCase {
  private RevCommit makeCommitWithSubmodule()
      throws NoFilepatternException, IOException, GitAPIException {
    try (Git git = new Git(repo)) {
      SubmoduleAddCommand addCommand = git.submoduleAdd();
      addCommand.setURI(repo.getDirectory().getCanonicalPath());
      addCommand.setPath("modules/library");
      addCommand.call().close();
      git.add().addFilepattern(".").call();
      return git.commit().setMessage("Commit with submodule.").call();
    }
  }

  @Test
  public void testWithSubmodule() throws Exception {
    RevCommit c = makeCommitWithSubmodule();
    List<CommitValidationMessage> m =
        SubmoduleValidator.performValidation(repo, c);
    assertThat(TestUtils.transformMessages(m))
        .containsExactlyElementsIn(ImmutableSet.of(
            "ERROR: submodules are not allowed: modules/library"));
  }

  private RevCommit makeCommitWithoutSubmodule()
      throws NoFilepatternException, IOException, GitAPIException {
    Map<File, byte[]> files = new HashMap<>();
    files.put(new File(repo.getDirectory().getParent(), "foo.txt"), null);
    return TestUtils.makeCommit(repo, "Commit with empty test files.", files);
  }

  @Test
  public void testWithoutSubmodule() throws Exception {
    RevCommit c = makeCommitWithoutSubmodule();
    List<CommitValidationMessage> m =
        SubmoduleValidator.performValidation(repo, c);
    assertThat(m).isEmpty();
  }

  @Test
  public void validatorInactiveWhenConfigEmpty() {
    assertThat(SubmoduleValidator.isActive(TestUtils.emptyPluginConfig))
        .isFalse();
  }
}
