package com.googlesource.gerrit.plugins.uploadvalidator;

import static com.google.common.truth.Truth.assertThat;

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
  private static final String[] BLOCKED_EXTENSIONS_LC =
      new String[] {"jpeg", "pdf", "zip", "exe", "tar.gz"};
  private static final String[] BLOCKED_EXTENSIONS_UC =
      new String[] {"JPEG", "PDF", "ZIP", "EXE", "TAR.GZ"};

  private RevCommit makeCommit(String[] blockedExtensions)
      throws NoFilepatternException, IOException, GitAPIException {
    Set<File> files = new HashSet<>();
    for (String extension : blockedExtensions) {
      files.add(new File(repo.getDirectory().getParent(), "foo." + extension));
    }
    // valid extensions
    files.add(new File(repo.getDirectory().getParent(), "foo.java"));
    files.add(new File(repo.getDirectory().getParent(), "foo.core.tmp"));
    files.add(new File(repo.getDirectory().getParent(), "foo.c"));
    files.add(new File(repo.getDirectory().getParent(), "foo.txt"));
    return TestUtils.makeCommit(repo, "Commit with empty test files.", files);
  }

  @Test
  public void testBlockedExtensions() throws Exception {
    RevCommit c = makeCommit(BLOCKED_EXTENSIONS_LC);
    List<CommitValidationMessage> m = FileExtensionValidator
        .performValidation(repo, c, BLOCKED_EXTENSIONS_LC);
    List<ComparableCommitValidationMessage> expected = new ArrayList<>();
    for (String extension : BLOCKED_EXTENSIONS_LC) {
      expected.add(new ComparableCommitValidationMessage(
          "blocked file: foo." + extension, true));
    }
    assertThat(m).hasSize(BLOCKED_EXTENSIONS_LC.length);
    assertThat(TestUtils.transformMessages(m)).containsAllIn(expected);
  }

  @Test
  public void testBlockedExtensionsCaseInsensitive() throws Exception {
    RevCommit c = makeCommit(BLOCKED_EXTENSIONS_UC);
    List<CommitValidationMessage> m = FileExtensionValidator
        .performValidation(repo, c, BLOCKED_EXTENSIONS_LC);
    List<ComparableCommitValidationMessage> expected = new ArrayList<>();
    for (String extension : BLOCKED_EXTENSIONS_UC) {
      expected.add(new ComparableCommitValidationMessage(
          "blocked file: foo." + extension, true));
    }
    assertThat(m).hasSize(BLOCKED_EXTENSIONS_UC.length);
    assertThat(TestUtils.transformMessages(m)).containsAllIn(expected);
  }

  @Test
  public void testDefaultValue() {
    assertThat(FileExtensionValidator
        .isActive(TestUtils.getEmptyPluginConfig())).isFalse();
  }
}
