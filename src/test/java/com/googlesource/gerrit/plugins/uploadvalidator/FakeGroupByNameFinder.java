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

import com.google.common.collect.ImmutableSet;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.server.group.InternalGroup;
import java.util.Objects;
import java.util.Optional;

public class FakeGroupByNameFinder implements ValidatorConfig.GroupByNameFinder {

  private final Optional<InternalGroup> onlyGroup;

  public FakeGroupByNameFinder() {
    onlyGroup = Optional.empty();
  }

  public FakeGroupByNameFinder(AccountGroup accountGroup) {
    onlyGroup =
        Optional.of(InternalGroup.create(accountGroup, ImmutableSet.of(), ImmutableSet.of()));
  }

  @Override
  public Optional<InternalGroup> get(AccountGroup.NameKey groupName) {
    return onlyGroup.filter(group -> Objects.equals(group.getNameKey(), groupName));
  }
}
