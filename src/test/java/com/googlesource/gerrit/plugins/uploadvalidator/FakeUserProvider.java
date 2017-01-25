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

import static org.easymock.EasyMock.*;

import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.inject.Provider;

public class FakeUserProvider implements Provider<CurrentUser> {
  private final String[] groupUUID;

  public FakeUserProvider(String... groupUUID) {
    this.groupUUID = groupUUID;
  }

  @Override
  public CurrentUser get() {
    return createNew();
  }

  private IdentifiedUser createNew() {
    IdentifiedUser user = createMock(IdentifiedUser.class);
    expect(user.isIdentifiedUser()).andReturn(true);
    expect(user.asIdentifiedUser()).andReturn(user);
    expect(user.getEffectiveGroups()).andReturn(
        new FakeGroupMembership(groupUUID));
    replay(user);
    return user;
  }
}
