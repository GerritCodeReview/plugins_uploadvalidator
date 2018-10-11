// Copyright (C) 2017 The Android Open Source Project
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

import com.google.gerrit.server.mime.FileTypeRegistry;
import com.google.inject.Singleton;
import eu.medsea.mimeutil.MimeType;
import java.io.InputStream;

@Singleton
class FakeMimeUtilFileTypeRegistry implements FileTypeRegistry {

  @Override
  public MimeType getMimeType(String path, byte[] content) {
    if (path.endsWith(".pdf")) {
      return new MimeType("application/pdf");
    }
    if (path.endsWith(".xml")) {
      return new MimeType("application/xml");
    }
    if (path.endsWith(".html")) {
      return new MimeType("text/html");
    }
    return new MimeType("application/octet-stream");
  }

  @Override
  public MimeType getMimeType(String path, InputStream is) {
    if (path.endsWith(".pdf")) {
      return new MimeType("application/pdf");
    }
    if (path.endsWith(".xml")) {
      return new MimeType("application/xml");
    }
    if (path.endsWith(".html")) {
      return new MimeType("text/html");
    }
    return new MimeType("application/octet-stream");
  }

  @Override
  public boolean isSafeInline(MimeType type) {
    return false;
  }
}
