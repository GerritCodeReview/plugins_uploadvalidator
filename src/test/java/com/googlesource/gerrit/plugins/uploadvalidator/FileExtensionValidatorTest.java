package com.googlesource.gerrit.plugins.uploadvalidator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.git.validators.CommitValidationMessage;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileExtensionValidatorTest extends ValidatorTestCase {
  private static final String[] BLOCKED_EXTENSIONS_LC =
      new String[] {"jpeg", "pdf", "zip", "exe"};
  private static final String[] BLOCKED_EXTENSIONS_UC =
      new String[] {"JPEG", "PDF", "ZIP", "EXE"};

  private RevCommit makeCommit(String[] blockedExtensions)
      throws NoFilepatternException, IOException, GitAPIException {
    Set<File> files = new HashSet<>();
    for (String extension : blockedExtensions) {
      files.add(new File(repo.getDirectory().getParent(), "foo." + extension));
    }
    // valid extensions
    files.add(new File(repo.getDirectory().getParent(), "foo.java"));
    files.add(new File(repo.getDirectory().getParent(), "foo.c"));
    files.add(new File(repo.getDirectory().getParent(), "foo.txt"));
    return TestUtils.makeCommit(repo, "Commit with empty test files.", files);
  }

  @Test
  public void testBlockedExtensions() throws Exception {
    RevCommit c = makeCommit(BLOCKED_EXTENSIONS_LC);
    List<CommitValidationMessage> m = FileExtensionValidator
        .performValidation(repo, c, BLOCKED_EXTENSIONS_LC);
    List<CommitValidationMessage> expected = new ArrayList<>();
    for (String extension : BLOCKED_EXTENSIONS_LC) {
      expected.add(new CommitValidationMessage(
          "blocked file: " + "foo." + extension, true));
    }
    assertEquals(BLOCKED_EXTENSIONS_LC.length, m.size());
    assertTrue(TestUtils.compareCommitValidationMessage(m, expected));
  }

  @Test
  public void testBlockedExtensionsCaseInsensitive() throws Exception {
    RevCommit c = makeCommit(BLOCKED_EXTENSIONS_UC);
    List<CommitValidationMessage> m = FileExtensionValidator
        .performValidation(repo, c, BLOCKED_EXTENSIONS_LC);
    List<CommitValidationMessage> expected = new ArrayList<>();
    for (String extension : BLOCKED_EXTENSIONS_UC) {
      expected.add(new CommitValidationMessage(
          "blocked file: " + "foo." + extension, true));
    }
    assertEquals(BLOCKED_EXTENSIONS_UC.length, m.size());
    assertTrue(TestUtils.compareCommitValidationMessage(m, expected));
  }

  @Test
  public void testDefaultValues() {
    PluginConfig cfg = new PluginConfig("", new Config());
    assertEquals(0, FileExtensionValidator.getBlockedExtensions(cfg).length);
  }
}
