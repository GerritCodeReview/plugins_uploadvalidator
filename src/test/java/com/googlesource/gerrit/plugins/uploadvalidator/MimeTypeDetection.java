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

import static java.util.Comparator.comparing;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil2;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class MimeTypeDetection {
  public String getMimeType(String path, byte[] content) {
    MimeUtil2 mimeUtil = new MimeUtil2();

    Set<MimeType> mimeTypes = new HashSet<>();
    mimeTypes.addAll(mimeUtil.getMimeTypes(content));
    mimeTypes.addAll(mimeUtil.getMimeTypes(path));

    if (mimeTypes.isEmpty()
        || (mimeTypes.size() == 1 && mimeTypes.contains(MimeUtil2.UNKNOWN_MIME_TYPE))) {
      return MimeUtil2.UNKNOWN_MIME_TYPE.toString();
    }

    return Collections.max(mimeTypes, comparing(MimeType::getSpecificity)).toString();
  }
}
