// Copyright (C) 2020 The Android Open Source Project
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class MimeTypeDetectionTest extends ValidatorTestCase {
  private class FileContent {
    public FileContent(String fileName, byte[] content, String contentType) {
      this.fileName = fileName;
      this.content = content;
      this.contentType = contentType;
    }

    public String fileName;
    public byte[] content;
    public String contentType;
  }

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

  private MimeTypeDetection detection;

  @Before
  public void setUp() {
    detection = new MimeTypeDetection();
  }

  @Test
  public void testMimeTypeDetection() throws Exception {
    List<FileContent> files = createFiles();
    for (FileContent file : files) {
      assertThat(detection.getMimeType(file.fileName, file.content).toString())
          .isEqualTo(file.contentType);
    }
  }

  private List<FileContent> createFiles() {
    List<FileContent> files = new ArrayList<>();

    String content = "<?xml version=\"1.0\"?><a><b>c</b></a>";
    files.add(
        new FileContent("foo.xml", content.getBytes(StandardCharsets.UTF_8), "application/xml"));

    content = "<html><body><h1>Hello World!</h1></body></html>";
    files.add(new FileContent("foo.html", content.getBytes(StandardCharsets.UTF_8), "text/html"));

    content = "Hello,World";
    files.add(new FileContent("foo.csv", content.getBytes(StandardCharsets.UTF_8), "text/csv"));

    content = "hello=world";
    files.add(new FileContent("foo", content.getBytes(StandardCharsets.UTF_8), "text/plain"));

    files.add(new FileContent("foo.pdf", TEST_PDF, "application/pdf"));
    return files;
  }
}
