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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InvalidFilenameValidatorTest extends ValidatorTestCase {
  private Set<String> getInvalidFilenames() {
    Set<String> filenames = new HashSet<>();
    filenames.add("test#");
    filenames.add("test%");
    filenames.add("test*");
    filenames.add("test:");
    filenames.add("test@");
    return filenames;
  }

  private RevCommit makeCommit()
      throws NoFilepatternException, IOException, GitAPIException {
    Set<File> files = new HashSet<>();
    for (String filenames : getInvalidFilenames()) {
      files.add(new File(repo.getDirectory().getParent(), filenames));
    }
    // valid filename
    files.add(new File(repo.getDirectory().getParent(), "test"));
    return TestUtils.makeCommit(repo, "Commit with empty test files.", files);
  }

  @Test
  public void test() throws Exception {
    String[] invalidFilenamePattern = {"[@:]", "[#%*]"};
    RevCommit c = makeCommit();
    List<CommitValidationMessage> m = InvalidFilenameValidator
        .performValidation(repo, c, invalidFilenamePattern);
    Set<String> expected = new HashSet<>();
    for (String filenames : getInvalidFilenames()) {
      expected.add("ERROR: invalid characters found in filename: " + filenames);
    }
    assertThat(TestUtils.transformMessages(m))
        .containsExactlyElementsIn(expected);
  }
}
