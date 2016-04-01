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

import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class ContentTypeUtilTest {

  @Test
  public void testDoesTypeMatch() throws ExecutionException {
    ContentTypeUtil ctu = new ContentTypeUtil(TestUtils.getPatternCache());
    String[] listOfTypes =
        new String[] {"text/*", "^application/(pdf|xml)", "application/zip"};
    // valid
    assertThat(ctu.doesTypeMatch("text/xml", listOfTypes)).isTrue();
    assertThat(ctu.doesTypeMatch("text/html", listOfTypes)).isTrue();
    assertThat(ctu.doesTypeMatch("text/plain", listOfTypes)).isTrue();
    assertThat(ctu.doesTypeMatch("application/pdf", listOfTypes)).isTrue();
    assertThat(ctu.doesTypeMatch("application/xml", listOfTypes)).isTrue();
    assertThat(ctu.doesTypeMatch("application/zip", listOfTypes)).isTrue();
    // mismatch
    assertThat(ctu.doesTypeMatch("foo/bar", listOfTypes)).isFalse();
    assertThat(ctu.doesTypeMatch("application/msword", listOfTypes)).isFalse();
  }

  @Test
  public void testDefaultValue() {
    assertThat(ContentTypeUtil.getBinaryTypes(TestUtils.getEmptyPluginConfig()))
        .isEmpty();
  }
}
