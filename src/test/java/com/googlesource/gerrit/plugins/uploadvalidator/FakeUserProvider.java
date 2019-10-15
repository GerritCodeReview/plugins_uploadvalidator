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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.util.time.TimeUtil;
import com.google.inject.Provider;

public class FakeUserProvider implements Provider<IdentifiedUser> {
  public static final String FAKE_EMAIL = "fake@example.com";

  private final String[] groupUUID;

  public FakeUserProvider(String... groupUUID) {
    this.groupUUID = groupUUID;
  }

  @Override
  public IdentifiedUser get() {
    return createNew(FAKE_EMAIL);
  }

  public IdentifiedUser get(String email) {
    return createNew(email);
  }

  private IdentifiedUser createNew(String email) {
    IdentifiedUser user = mock(IdentifiedUser.class);
    Account account =
        Account.builder(Account.id(1), TimeUtil.nowTs()).setPreferredEmail(email).build();
    when(user.isIdentifiedUser()).thenReturn(true);
    when(user.asIdentifiedUser()).thenReturn(user);
    when(user.getAccount()).thenReturn(account);
    when(user.getEffectiveGroups()).thenReturn(new FakeGroupMembership(groupUUID));
    return user;
  }
}
