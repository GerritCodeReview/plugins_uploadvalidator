package com.googlesource.gerrit.plugins.uploadvalidator;

import com.google.gerrit.server.project.RefPatternMatcher;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.collect.ImmutableList;

public class BlockedKeywordMatcher {
  private final Pattern matchPattern;
  private final List<Pattern> skipProjectPatterns;
  private final List<String> skipRefPatterns; // String not Pattern since RefPatternMatcher is used.
  private final List<Pattern> skipFilePatterns;
  private final List<Pattern> skipEmailPatterns;

  public BlockedKeywordMatcher(Pattern matchPattern) {
    this.matchPattern = matchPattern;
    this.skipProjectPatterns = List.of();
    this.skipRefPatterns = List.of();
    this.skipFilePatterns = List.of();
    this.skipEmailPatterns = List.of();
  }

  public BlockedKeywordMatcher(
      Pattern matchPattern,
      List<Pattern> skipProjectPatterns,
      List<String> skipRefPatterns,
      List<Pattern> skipFilePatterns,
      List<Pattern> skipEmailPatterns
  ) {
    this.matchPattern = matchPattern;
    this.skipProjectPatterns = skipProjectPatterns;
    this.skipRefPatterns = skipRefPatterns;
    this.skipFilePatterns = skipFilePatterns;
    this.skipEmailPatterns = skipEmailPatterns;
  }

  public Matcher matcher(CharSequence input) {
    return matchPattern.matcher(input);
  }

  public boolean isValid(String projectName, String ref, ImmutableList<String> emails) {
    return !skipProject(projectName) && !skipRef(ref) && !skipEmails(emails);
  }

  public boolean isValid(String projectName, String ref, ImmutableList<String> emails, String path) {
    return !skipProject(projectName) && !skipRef(ref) && !skipEmails(emails) && !skipPath(path);
  }

  public boolean skipProject(String projectName) {
    return skipProjectPatterns.stream().anyMatch(p -> p.matcher(projectName).matches());
  }

  public boolean skipRef(String ref) {
    return skipRefPatterns.stream().map(RefPatternMatcher::getMatcher).anyMatch(p -> p.match(ref, null));
  }

  public boolean skipEmails(ImmutableList<String> emails) {
    return emails.stream().anyMatch(this::skipEmail);
  }

  public boolean skipEmail(String email) {
    return skipEmailPatterns.stream().anyMatch(p -> p.matcher(email).matches());
  }

  public boolean skipPath(String path) {
    // skipPath uses Matcher.find instead of Matcher.matches for partial string matching support
    // since the most common use case is matching file extensions or filenames and adding `.*` to
    // every entry is inconvenient.
    return skipFilePatterns.stream().anyMatch(p -> p.matcher(path).find());
  }
}
