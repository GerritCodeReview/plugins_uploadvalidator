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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.gerrit.server.git.validators.CommitValidationMessage;

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
import java.util.regex.Pattern;

public class BlockedKeywordValidatorTest extends ValidatorTestCase {
  private static ImmutableMap<String, Pattern> getPatterns() {
    return ImmutableMap.<String, Pattern> builder()
        .put("myp4ssw0rd", Pattern.compile("myp4ssw0rd"))
        .put("foobar", Pattern.compile("foobar"))
        .put("\\$(Id|Header):[^$]*\\$",
            Pattern.compile("\\$(Id|Header):[^$]*\\$"))
        .build();
  }

  @Test
  public void testLoader() throws Exception {
    LoadingCache<String, Pattern> patternCache =
        CacheBuilder.newBuilder().build(new BlockedKeywordValidator.Loader());
    for (String pattern : getPatterns().keySet()) {
      assertEquals(getPatterns().get(pattern).pattern(),
          patternCache.get(pattern).pattern());
    }
  }

  private RevCommit makeCommit()
      throws NoFilepatternException, IOException, GitAPIException {
    Map<File, byte[]> files = new HashMap<>();
    // invalid files
    String content = "http://foo.bar.tld/?pw=myp4ssw0rdTefoobarstline2\n";
    files.put(new File(repo.getDirectory().getParent(), "foo.txt"),
        content.getBytes(StandardCharsets.UTF_8));

    content = "$Id$\n"
        + "$Header$\n"
        + "$Author$\n"
        + "processXFile($File::Find::name, $Config{$type});\n"
        + "$Id: bla bla bla$\n";
    files.put(new File(repo.getDirectory().getParent(), "bar.txt"),
        content.getBytes(StandardCharsets.UTF_8));

    // valid file
    content = "Testline1\n"
        + "Testline2\n"
        + "Testline3\n"
        + "Testline4";
    files.put(new File(repo.getDirectory().getParent(), "foobar.txt"),
        content.getBytes(StandardCharsets.UTF_8));
    return TestUtils.makeCommit(repo, "Commit with test files.", files);
  }

  @Test
  public void testKeywords() throws Exception {
    RevCommit c = makeCommit();
    List<CommitValidationMessage> m = BlockedKeywordValidator
        .performValidation(repo, c, getPatterns().values());
    assertEquals(2, m.size());
    List<CommitValidationMessage> expected = new ArrayList<>();
    expected
        .add(new CommitValidationMessage("blocked keyword(s) found in file: "
            + "foo.txt (Line: 1) (found: myp4ssw0rd, foobar)", true));
    expected
        .add(new CommitValidationMessage("blocked keyword(s) found in file: "
            + "bar.txt (Line: 5) (found: $Id: bla bla bla$)", true));
    assertTrue(TestUtils.compareCommitValidationMessage(m, expected));
  }
}
