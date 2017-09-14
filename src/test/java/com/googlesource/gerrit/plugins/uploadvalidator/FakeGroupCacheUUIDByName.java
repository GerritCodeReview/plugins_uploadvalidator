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

import com.google.common.collect.ImmutableList;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.reviewdb.client.AccountGroup.Id;
import com.google.gerrit.reviewdb.client.AccountGroup.NameKey;
import com.google.gerrit.reviewdb.client.AccountGroup.UUID;
import com.google.gerrit.server.account.GroupCache;
import com.google.gerrit.server.group.InternalGroup;
import java.util.Optional;

public class FakeGroupCacheUUIDByName implements GroupCache {
  private AccountGroup accountGroup;

  public FakeGroupCacheUUIDByName(AccountGroup accountGroup) {
    this.accountGroup = accountGroup;
  }

  public FakeGroupCacheUUIDByName() {
    // TODO Auto-generated constructor stub
  }

  @Override
  public AccountGroup get(Id groupId) {
    return null;
  }

  @Override
  public AccountGroup get(NameKey name) {
    return accountGroup != null && accountGroup.getNameKey().equals(name) ? accountGroup : null;
  }

  @Override
  public Optional<InternalGroup> get(UUID uuid) {
    return null;
  }

  @Override
  public ImmutableList<AccountGroup> all() {
    return null;
  }

  @Override
  public void onCreateGroup(NameKey newGroupName) {}

  @Override
  public void evict(
      AccountGroup.UUID groupUuid, AccountGroup.Id groupId, AccountGroup.NameKey groupName) {}

  @Override
  public void evictAfterRename(NameKey oldName, NameKey newName) {}
}
