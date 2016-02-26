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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gerrit.server.git.validators.CommitValidationMessage;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InvalidFilenameValidatorTest extends ValidatorTestCase {
  private InvalidFilenameValidator validator;

  @Override
  protected void initValidator() {
    List<String> patterns = new ArrayList<>();
    patterns.add("[@:]");
    patterns.add("[#%*]");
    cfg.setStringList(
        InvalidFilenameValidator.KEY_INVALID_FILENAME_PATTERN, patterns);
    validator =  new InvalidFilenameValidator(null, null, null);
  }

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
    RevCommit c = makeCommit();
    List<CommitValidationMessage> m = validator.performValidation(repo, c, cfg);

    List<CommitValidationMessage> expected = new ArrayList<>();
    for (String filenames : getInvalidFilenames()) {
      expected.add(new CommitValidationMessage(
          "invalid characters found in filename: " + filenames, true));
    }
    assertEquals(5, m.size());
    assertTrue(TestUtils.compareCommitValidationMessage(m, expected));
  }
}
