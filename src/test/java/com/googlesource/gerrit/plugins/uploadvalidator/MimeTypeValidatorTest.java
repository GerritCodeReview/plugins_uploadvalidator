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
import com.google.gerrit.server.mime.MimeUtil2Module;
import com.google.inject.Guice;

import eu.medsea.mimeutil.MimeUtil2;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MimeTypeValidatorTest extends ValidatorTestCase {
  private MimeUtil2 mu2;

  @Override
  protected void initValidator() {
    mu2 = Guice.createInjector(
        new MimeUtil2Module()).getInstance(MimeUtil2.class);
    validator = new MimeTypeValidator(null, mu2, null, null);
  }

  private RevCommit makeCommit()
      throws NoFilepatternException, IOException, GitAPIException {
    Map<File, byte[]> files = new HashMap<>();
    String content = "<?xml version=\"1.0\"?><a><b>c</b></a>";
    files.put(new File(repo.getDirectory().getParent(), "foo.xml"),
        content.getBytes(StandardCharsets.UTF_8));
    content = "public enum Test {TEST}";
    files.put(new File(repo.getDirectory().getParent(), "foo.java"),
        content.getBytes(StandardCharsets.UTF_8));
    content = "<html><body><h1>Hello World!</h1></body></html>";
    files.put(new File(repo.getDirectory().getParent(), "foo.html"),
        content.getBytes(StandardCharsets.UTF_8));
    files.put(new File(
        repo.getDirectory().getParent(), "foo.pdf"), getExamplePDF());
    return TestUtils.makeCommit(repo, "Commit with test files.", files);
  }

  @Test
  public void testMimeType() throws Exception {
    List<String> patterns = new ArrayList<>();
    patterns.add("text/html");
    patterns.add("text/xml");
    patterns.add("application/pdf");
    cfg.setStringList(MimeTypeValidator.KEY_BLOCKED_MIME_TYPE, patterns);

    RevCommit c = makeCommit();
    List<CommitValidationMessage> m = validator.performValidation(repo, c, cfg);
    assertEquals(3, m.size());
    List<CommitValidationMessage> expected = new ArrayList<>();
    expected.add(new CommitValidationMessage(
        "found blocked mime type (text/html) in file: foo.html", true));
    expected.add(new CommitValidationMessage(
        "found blocked mime type (text/xml) in file: foo.xml", true));
    expected.add(new CommitValidationMessage(
        "found blocked mime type (application/pdf) in file: foo.pdf", true));
    assertTrue(TestUtils.compareCommitValidationMessage(m, expected));
  }

  private byte[] getExamplePDF() {
    String pdf = "%PDF-1.4\n"
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
    + "%%EOF";
    return pdf.getBytes(StandardCharsets.UTF_8);
  }
}
