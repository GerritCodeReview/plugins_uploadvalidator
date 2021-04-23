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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.entities.Patch;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.patch.PatchList;
import com.google.gerrit.server.patch.PatchListCache;
import com.google.gerrit.server.patch.PatchListEntry;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Test;

public class BlockedKeywordValidatorTest extends ValidatorTestCase {
  /** Maps file names to content. */
  private static final Map<String, String> FILE_CONTENTS =
      ImmutableMap.of(
          "bar.txt",
          "$Id$\n"
              + "$Header$\n"
              + "$Author$\n"
              + "processXFile($File::Find::name, $Config{$type});\n"
              + "$Id: foo bar$\n",
          "foo.txt",
          "http://foo.bar.tld/?pw=myp4ssw0rdTefoobarstline2\n",
          "foobar.txt",
          "Testline1\n" + "Testline2\n" + "Testline3\n" + "Testline4");

  private static ImmutableMap<String, Pattern> getPatterns() {
    return ImmutableMap.<String, Pattern>builder()
        .put("myp4ssw0rd", Pattern.compile("myp4ssw0rd"))
        .put("foobar", Pattern.compile("foobar"))
        .put("\\$(Id|Header):[^$]*\\$", Pattern.compile("\\$(Id|Header):[^$]*\\$"))
        .build();
  }

  private RevCommit makeCommit(RevWalk rw) throws IOException, GitAPIException {
    Map<File, byte[]> files = new HashMap<>();
    for (Map.Entry<String, String> fileContents : FILE_CONTENTS.entrySet()) {
      files.put(
          new File(repo.getDirectory().getParent(), fileContents.getKey()),
          fileContents.getValue().getBytes(StandardCharsets.UTF_8));
    }
    return TestUtils.makeCommit(rw, repo, "Commit foobar with test files.", files);
  }

  @Test
  public void keywords() throws Exception {
    // Mock the PatchListCache to return a diff for each file in our new commit
    PatchListCache patchListCacheMock = mock(PatchListCache.class);
    PatchList mockPatchList = mock(PatchList.class);
    when(patchListCacheMock.get(any(), any(Project.NameKey.class))).thenReturn(mockPatchList);
    for (Map.Entry<String, String> fileContent : FILE_CONTENTS.entrySet()) {
      PatchListEntry file = mock(PatchListEntry.class);
      when(file.getEdits())
          .thenReturn(
              ImmutableList.of(new Edit(0, 0, 0, numberOfLinesInString(fileContent.getValue()))));
      when(mockPatchList.get(fileContent.getKey())).thenReturn(file);
    }

    try (RevWalk rw = new RevWalk(repo)) {
      RevCommit c = makeCommit(rw);
      BlockedKeywordValidator validator =
          new BlockedKeywordValidator(
              null,
              new ContentTypeUtil(PATTERN_CACHE),
              PATTERN_CACHE,
              null,
              null,
              patchListCacheMock,
              null);
      List<CommitValidationMessage> m =
          validator.performValidation(
              Project.nameKey("project"), repo, c, rw, getPatterns().values(), EMPTY_PLUGIN_CONFIG);
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

  public static int numberOfLinesInString(String str) {
    return str.length() - str.replace("\n", "").length();
  }
}
