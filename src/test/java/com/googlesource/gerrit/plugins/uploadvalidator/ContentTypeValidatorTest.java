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
import static com.googlesource.gerrit.plugins.uploadvalidator.TestUtils.PATTERN_CACHE;

import com.google.gerrit.server.git.validators.CommitValidationMessage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Before;
import org.junit.Test;

public class ContentTypeValidatorTest extends ValidatorTestCase {

  private static final byte[] TEST_PDF =
      ("%PDF-1.4\n"
              + "1 0 obj << /Type /Catalog /Outlines 2 0 R /Pages 3 0 R >>\n"
              + "endobj 2 0 obj << /Type /Outlines /Count 0 >>\n"
              + "endobj 3 0 obj << /Type /Pages /Kids [4 0 R] /Count 1\n"
              + ">> endobj 4 0 obj << /Type /Page /Parent 3 0 R\n"
              + "/MediaBox [0 0 612 144] /Contents 5 0 R /Resources << /ProcSet 6 0 R\n"
              + "/Font << /F1 7 0 R >> >> >> endobj 5 0 obj\n"
              + "<< /Length 73 >> stream BT\n"
              + "/F1 24 Tf\n"
              + "100 100 Td\n"
              + "(Small pdf) Tj\n"
              + "ET endstream endobj 6 0 obj [/PDF /Text] endobj 7 0 obj\n"
              + "<< /Type /Font /Subtype /Type1 /Name /F1 /BaseFont /Helvetica\n"
              + "/Encoding /MacRomanEncoding >> endobj xref 0 8\n"
              + "0000000000 65535 f 0000000009 00000 n 0000000074 00000 n\n"
              + "0000000120 00000 n 0000000179 00000 n 0000000364 00000 n\n"
              + "0000000466 00000 n 0000000496 00000 n\n"
              + "trailer << /Size 8 /Root 1 0 R >> startxref 625\n"
              + "%%EOF")
          .getBytes(StandardCharsets.UTF_8);

  private ContentTypeValidator validator;

  @Before
  public void setUp() {
    validator =
        new ContentTypeValidator(
            null,
            new ContentTypeUtil(PATTERN_CACHE, new FakeMimeUtilFileTypeRegistry()),
            null,
            null,
            null);
  }

  @Test
  public void testBlocked() throws Exception {
    String[] patterns = new String[] {"application/pdf", "application/xml", "text/html"};

    try (RevWalk rw = new RevWalk(repo)) {
      List<CommitValidationMessage> m =
          validator.performValidation(repo, makeCommit(rw), rw, patterns, false);
      assertThat(TestUtils.transformMessages(m))
          .containsExactly(
              "ERROR: found blocked content type (application/pdf) in file: foo.pdf",
              "ERROR: found blocked content type (application/xml) in file: foo.xml",
              "ERROR: found blocked content type (text/html) in file: foo.html");
    }
  }

  @Test
  public void testWhitelist() throws Exception {
    String[] patterns = new String[] {"application/pdf", "application/xml"};

    try (RevWalk rw = new RevWalk(repo)) {
      List<CommitValidationMessage> m =
          validator.performValidation(repo, makeCommit(rw), rw, patterns, true);
      assertThat(TestUtils.transformMessages(m))
          .containsExactly("ERROR: found blocked content type (text/html) in file: foo.html");
    }
  }

  @Test
  public void validatorBehaviorWhenConfigEmpty() {
    assertThat(ContentTypeValidator.isActive(EMPTY_PLUGIN_CONFIG)).isFalse();
    assertThat(ContentTypeValidator.isWhitelist(EMPTY_PLUGIN_CONFIG)).isFalse();
  }

  private RevCommit makeCommit(RevWalk rw) throws IOException, GitAPIException {
    Map<File, byte[]> files = new HashMap<>();

    String content = "<?xml version=\"1.0\"?><a><b>c</b></a>";
    files.put(TestUtils.createEmptyFile("foo.xml", repo), content.getBytes(StandardCharsets.UTF_8));

    content = "<html><body><h1>Hello World!</h1></body></html>";
    files.put(
        TestUtils.createEmptyFile("foo.html", repo), content.getBytes(StandardCharsets.UTF_8));

    files.put(TestUtils.createEmptyFile("foo.pdf", repo), TEST_PDF);
    return TestUtils.makeCommit(rw, repo, "Commit with test files.", files);
  }
}
