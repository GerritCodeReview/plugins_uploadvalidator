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

import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class ContentTypeUtilTest {

  @Test
  public void testMatchesAny() throws ExecutionException {
    ContentTypeUtil ctu = new ContentTypeUtil(PATTERN_CACHE);
    String[] patterns =
        new String[] {"text/*", "^application/(pdf|xml)", "application/zip"};

    matchesAny(ctu, "text/xml", patterns);
    matchesAny(ctu, "text/html", patterns);
    matchesAny(ctu, "text/plain", patterns);
    matchesAny(ctu, "application/pdf", patterns);
    matchesAny(ctu, "application/xml", patterns);
    matchesAny(ctu, "application/zip", patterns);

    noMatch(ctu, "foo/bar", patterns);
    noMatch(ctu, "application/msword", patterns);
  }

  private void matchesAny(ContentTypeUtil ctu, String p, String[] patterns)
      throws ExecutionException {
    assertThat(ctu.matchesAny(p, patterns)).isTrue();
  }

  private void noMatch(ContentTypeUtil ctu, String p, String[] patterns)
      throws ExecutionException {
    assertThat(ctu.matchesAny(p, patterns)).isFalse();
  }

  @Test
  public void noBinaryTypesWhenConfigEmpty() {
    assertThat(ContentTypeUtil.getBinaryTypes(EMPTY_PLUGIN_CONFIG)).isEmpty();
  }
}
