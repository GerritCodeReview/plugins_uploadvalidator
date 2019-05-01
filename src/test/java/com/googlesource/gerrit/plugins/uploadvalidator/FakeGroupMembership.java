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

import com.google.gerrit.server.account.GroupMembership;
import com.google.gerrit.server.group.db.GroupUuid;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FakeGroupMembership implements GroupMembership {
  private final Set<String> memberOfGroup = new HashSet<>();

  public FakeGroupMembership(String... memberOfGroup) {
    Collections.addAll(this.memberOfGroup, memberOfGroup);
  }

  @Override
  public boolean contains(GroupUuid groupId) {
    return memberOfGroup.contains(groupId.get());
  }

  @Override
  public boolean containsAnyOf(Iterable<GroupUuid> groupIds) {
    return !intersection(groupIds).isEmpty();
  }

  @Override
  public Set<GroupUuid> intersection(Iterable<GroupUuid> groupIds) {
    return StreamSupport.stream(groupIds.spliterator(), false)
        .filter(this::contains)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<GroupUuid> getKnownGroups() {
    return new HashSet<>();
  }
}
