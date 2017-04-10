package com.googlesource.gerrit.plugins.uploadvalidator;

import com.google.gerrit.server.mime.FileTypeRegistry;
import com.google.inject.Singleton;

import eu.medsea.mimeutil.MimeType;

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
  public boolean isSafeInline(MimeType type) {
    return false;
  }
}
