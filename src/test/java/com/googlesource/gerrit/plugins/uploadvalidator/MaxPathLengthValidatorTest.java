package com.googlesource.gerrit.plugins.uploadvalidator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gerrit.server.git.validators.CommitValidationMessage;

import org.eclipse.jgit.api.Git;
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

public class MaxPathLengthValidatorTest extends ValidatorTestCase {
  private static final String TOO_LONG = "foo/bar/test/too/long.java";
  private static final String GOOD = "not/too/long.c";

  private static int getMaxPathLength() {
    return (TOO_LONG.length() + GOOD.length()) / 2;
  }

  private RevCommit makeCommit()
      throws NoFilepatternException, IOException, GitAPIException {
    Set<File> files = new HashSet<>();
    files.add(new File(repo.getDirectory().getParent(), TOO_LONG));
    files.add(new File(repo.getDirectory().getParent(), GOOD));
    return TestUtils.makeCommit(repo, "Commit with empty test files.", files);
  }

  @Test
  public void testAddTooLongPath() throws Exception {
    RevCommit c = makeCommit();
    List<CommitValidationMessage> m = MaxPathLengthValidator
        .performValidation(repo, c, getMaxPathLength());
    List<CommitValidationMessage> expected = new ArrayList<>();
    expected.add(new CommitValidationMessage(
        "path too long: " + "foo/bar/test/too/long.java", true));
    assertEquals(1, m.size());
    assertTrue(TestUtils.compareCommitValidationMessage(m, expected));
  }

  @Test
  public void testDeleteTooLongPath() throws Exception {
    RevCommit c = makeCommit();
    try(Git git = new Git(repo)) {
      Set<File> files = new HashSet<>();
      files.add(new File(repo.getDirectory().getParent(), TOO_LONG));
      TestUtils.removeFiles(git, files);
      c = git.commit().setMessage("Delete file which is too long").call();
    }
    List<CommitValidationMessage> m = MaxPathLengthValidator
        .performValidation(repo, c, getMaxPathLength());
    assertEquals(0, m.size());
  }
}
