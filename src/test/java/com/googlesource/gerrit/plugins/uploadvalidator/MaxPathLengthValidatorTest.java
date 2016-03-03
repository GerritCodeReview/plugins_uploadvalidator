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

public class MaxPathLengthValidatorTest extends ValidatorTestCase {
  private static final String TOO_LONG = "foo/bar/test/too/long.java";
  private static final String GOOD = "not/too/long.c";

  private RevCommit makeCommit()
      throws NoFilepatternException, IOException, GitAPIException {
    Set<File> files = new HashSet<>();
    files.add(new File(repo.getDirectory().getParent(), TOO_LONG));
    files.add(new File(repo.getDirectory().getParent(), GOOD));
    return TestUtils.makeCommit(repo, "Commit with empty test files.", files);
  }

  @Test
  public void test() throws Exception {
    RevCommit c = makeCommit();
    List<CommitValidationMessage> m = MaxPathLengthValidator
        .performValidation(repo, c, (TOO_LONG.length() + GOOD.length()) / 2);
    List<CommitValidationMessage> expected = new ArrayList<>();
    expected.add(new CommitValidationMessage(
        "path too long: " + "foo/bar/test/too/long.java", true));
    assertEquals(1, m.size());
    assertTrue(TestUtils.compareCommitValidationMessage(m, expected));
  }
}
