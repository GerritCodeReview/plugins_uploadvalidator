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

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class ContentTypeUtilTest {
  private ContentTypeUtil ctu;

  @Before
  public void setUp() {
    ctu = new ContentTypeUtil(PATTERN_CACHE);
  }

  @Test
  public void testMatchesAny() throws ExecutionException {
    String[] patterns =
        new String[] {"text/*", "^application/(pdf|xml)", "application/zip"};

    matchesAny("text/xml", patterns);
    matchesAny("text/html", patterns);
    matchesAny("text/plain", patterns);
    matchesAny("application/pdf", patterns);
    matchesAny("application/xml", patterns);
    matchesAny("application/zip", patterns);

    noMatch("foo/bar", patterns);
    noMatch("application/msword", patterns);
  }

  private void matchesAny(String p, String[] patterns)
      throws ExecutionException {
    assertThat(ctu.matchesAny(p, patterns)).isTrue();
  }

  private void noMatch(String p, String[] patterns)
      throws ExecutionException {
    assertThat(ctu.matchesAny(p, patterns)).isFalse();
  }

  @Test
  public void noBinaryTypesWhenConfigEmpty() {
    assertThat(ContentTypeUtil.getBinaryTypes(EMPTY_PLUGIN_CONFIG)).isEmpty();
  }
}
