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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.reviewdb.client.Patch;
import com.google.gerrit.server.git.validators.CommitValidationMessage;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class BlockedKeywordValidatorTest extends ValidatorTestCase {
  private static ImmutableMap<String, Pattern> getPatterns() {
    return ImmutableMap.<String, Pattern>builder()
        .put("myp4ssw0rd", Pattern.compile("myp4ssw0rd"))
        .put("foobar", Pattern.compile("foobar"))
        .put("\\$(Id|Header):[^$]*\\$", Pattern.compile("\\$(Id|Header):[^$]*\\$"))
        .build();
  }

  private RevCommit makeCommit(RevWalk rw) throws IOException, GitAPIException {
    Map<File, byte[]> files = new HashMap<>();
    // invalid files
    String content = "http://foo.bar.tld/?pw=myp4ssw0rdTefoobarstline2\n";
    files.put(
        new File(repo.getDirectory().getParent(), "foo.txt"),
        content.getBytes(StandardCharsets.UTF_8));

    content =
        "$Id$\n"
            + "$Header$\n"
            + "$Author$\n"
            + "processXFile($File::Find::name, $Config{$type});\n"
            + "$Id: foo bar$\n";
    files.put(
        new File(repo.getDirectory().getParent(), "bar.txt"),
        content.getBytes(StandardCharsets.UTF_8));

    // valid file
    content = "Testline1\n" + "Testline2\n" + "Testline3\n" + "Testline4";
    files.put(
        new File(repo.getDirectory().getParent(), "foobar.txt"),
        content.getBytes(StandardCharsets.UTF_8));
    return TestUtils.makeCommit(rw, repo, "Commit foobar with test files.", files);
  }

  @Test
  public void testKeywords() throws Exception {
    try (RevWalk rw = new RevWalk(repo)) {
      RevCommit c = makeCommit(rw);
      BlockedKeywordValidator validator =
          new BlockedKeywordValidator(null, PATTERN_CACHE, null, null, null);
      List<CommitValidationMessage> m =
          validator.performValidation(repo, c, rw, getPatterns().values(), EMPTY_PLUGIN_CONFIG);
      Set<String> expected =
          ImmutableSet.of(
              "ERROR: blocked keyword(s) found in: foo.txt (Line: 1)"
                  + " (found: myp4ssw0rd, foobar)",
              "ERROR: blocked keyword(s) found in: bar.txt (Line: 5)" + " (found: $Id: foo bar$)",
              "ERROR: blocked keyword(s) found in: "
                  + Patch.COMMIT_MSG
                  + " (Line: 1) (found: foobar)");
      assertThat(TestUtils.transformMessages(m)).containsExactlyElementsIn(expected);
    }
  }

  @Test
  public void validatorInactiveWhenConfigEmpty() {
    assertThat(BlockedKeywordValidator.isActive(EMPTY_PLUGIN_CONFIG)).isFalse();
  }
}
