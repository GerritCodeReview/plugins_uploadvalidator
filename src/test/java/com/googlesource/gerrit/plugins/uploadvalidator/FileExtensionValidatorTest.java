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

public class FileExtensionValidatorTest extends ValidatorTestCase {
  private String[] getBlockedExtensions() {
    String[] blockedExtensions = new String[] {"jpeg", "pdf", "zip", "exe"};
    return blockedExtensions;
  }

  private RevCommit makeCommit()
      throws NoFilepatternException, IOException, GitAPIException {
    Set<File> files = new HashSet<>();
    for (String extension : getBlockedExtensions()) {
      files.add(new File(repo.getDirectory().getParent(), "foo." + extension));
    }
    // valid extensions
    files.add(new File(repo.getDirectory().getParent(), "foo.java"));
    files.add(new File(repo.getDirectory().getParent(), "foo.c"));
    files.add(new File(repo.getDirectory().getParent(), "foo.txt"));
    return TestUtils.makeCommit(repo, "Commit with empty test files.", files);
  }

  @Test
  public void test() throws Exception {
    RevCommit c = makeCommit();
    List<CommitValidationMessage> m = FileExtensionValidator
        .performValidation(repo, c, getBlockedExtensions());
    List<CommitValidationMessage> expected = new ArrayList<>();
    for (String extension : getBlockedExtensions()) {
      expected.add(new CommitValidationMessage(
          "blocked file: " + "foo." + extension, true));
    }
    assertEquals(4, m.size());
    assertTrue(TestUtils.compareCommitValidationMessage(m, expected));
  }
}
