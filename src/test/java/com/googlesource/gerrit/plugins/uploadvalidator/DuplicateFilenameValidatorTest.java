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

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.gerrit.server.git.validators.CommitValidationMessage;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DuplicateFilenameValidatorTest extends ValidatorTestCase {

  @Override
  protected void initValidator() {
    validator = new DuplicateFilenameValidator(null, null, null);
  }

  private Set<File> getInitialFiles() {
    Set<File> files = new HashSet<>();
    files.add(new File(repo.getDirectory().getParent(), "foobar"));
    files.add(new File(repo.getDirectory().getParent(), "folder1/foo"));
    files.add(new File(repo.getDirectory().getParent(), "folder1/bar"));
    files.add(new File(repo.getDirectory().getParent(), "folder1/f2/foo"));
    files.add(new File(repo.getDirectory().getParent(), "folder1/f2/bar"));
    return files;
  }

  @Test
  public void testCheckInsideOfCommit() throws Exception {
    Set<File> files = getInitialFiles();
    // add files with duplicate names
    files.add(new File(repo.getDirectory().getParent(), "fooBar"));
    files.add(new File(repo.getDirectory().getParent(), "folder1/bAr"));
    files.add(new File(repo.getDirectory().getParent(), "folDer1/f2/foo"));
    files.add(new File(repo.getDirectory().getParent(), "folder1/F2/bar"));
    RevCommit c = TestUtils.makeCommit(
        repo, "Commit with empty test files.", files);
    List<CommitValidationMessage> m = validator.performValidation(
        repo, c, cfg);
    assertEquals(4, m.size());
    // During checking inside of the car it's unknown which file is checked
    // first, because of that, both capabilities must be checked.
    CommitValidationMessage msg1 = new CommitValidationMessage("fooBar"
        + ": filename collides with " + "foobar", true);
    CommitValidationMessage msg2 = new CommitValidationMessage("foobar"
        + ": filename collides with " + "fooBar", true);
    assertThat (true, anyOf(equalTo(TestUtils.doesCVMListContain(m, msg1)),
        equalTo(TestUtils.doesCVMListContain(m, msg2))));

    msg1 = new CommitValidationMessage("folder1/bAr"
        + ": filename collides with " + "folder1/bar", true);
    msg2 = new CommitValidationMessage("folder1/bar"
        + ": filename collides with " + "folder1/bAr", true);
    assertThat (true, anyOf(equalTo(TestUtils.doesCVMListContain(m, msg1)),
        equalTo(TestUtils.doesCVMListContain(m, msg2))));

    msg1 = new CommitValidationMessage("folDer1/f2/foo"
        + ": filename collides with " + "folder1/f2/foo", true);
    msg2 = new CommitValidationMessage("folder1/f2/foo"
        + ": filename collides with " + "folDer1/f2/foo", true);
    assertThat (true, anyOf(equalTo(TestUtils.doesCVMListContain(m, msg1)),
        equalTo(TestUtils.doesCVMListContain(m, msg2))));

    msg1 = new CommitValidationMessage("folder1/F2/bar"
        + ": filename collides with " + "folder1/f2/bar", true);
    msg2 = new CommitValidationMessage("folder1/f2/bar"
        + ": filename collides with " + "folder1/F2/bar", true);
    assertThat (true, anyOf(equalTo(TestUtils.doesCVMListContain(m, msg1)),
        equalTo(TestUtils.doesCVMListContain(m, msg2))));
  }

  private RevCommit makeCommitWithDuplicates()
      throws NoFilepatternException, IOException, GitAPIException {
    Map<File, byte[]> files = new HashMap<>();
    // content of file has changed
    files.put(new File(repo.getDirectory().getParent(), "foobar"),
        "Test".getBytes(StandardCharsets.UTF_8));
    // duplicate filenames
    files.put(new File(repo.getDirectory().getParent(), "folder1/Foo"), null);
    files.put(new File(repo.getDirectory().getParent(), "foldeR1/bar"), null);
    files.put(new File(repo.getDirectory().getParent(), "folder1/F2/foo"), null);
    files.put(new File(repo.getDirectory().getParent(), "folder1/f2/bAr"), null);
    // valid new filenames
    files.put(new File(repo.getDirectory().getParent(), "folder1/f2/valid"), null);
    files.put(new File(repo.getDirectory().getParent(), "valid"), null);
    files.put(new File(repo.getDirectory().getParent(), "folder1/valid/foo"), null);
    return TestUtils.makeCommit(repo, "Commit with empty test files.", files);
  }

  @Test
  public void testCheckAgainstWholeTree() throws Exception {
    Set<File> files = getInitialFiles();
    TestUtils.makeCommit(repo, "Commit with empty test files.", files);
    RevCommit c = makeCommitWithDuplicates();
    List<CommitValidationMessage> m = null;
    try (RevWalk revWalk = new RevWalk(repo)) {
      revWalk.parseHeaders(c.getParent(0));
      m = validator.performValidation(repo, c, cfg);
    }
    assertEquals(4, m.size());
    List<CommitValidationMessage> expected = new ArrayList<>();
    expected.add(new CommitValidationMessage("folder1/Foo"
          + ": filename collides with " + "folder1/foo", true));
    expected.add(new CommitValidationMessage("foldeR1/bar"
        + ": filename collides with " + "folder1/bar", true));
    expected.add(new CommitValidationMessage("folder1/F2/foo"
        + ": filename collides with " + "folder1/f2/foo", true));
    expected.add(new CommitValidationMessage("folder1/f2/bAr"
        + ": filename collides with " + "folder1/f2/bar", true));
    assertTrue(TestUtils.compareCommitValidationMessage(m, expected));
  }
}
