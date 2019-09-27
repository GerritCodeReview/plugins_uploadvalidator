// Copyright (C) 2019 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the 'License');
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an 'AS IS' BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

(function () {
  'use strict';

  Polymer({
    is: 'gr-uploadvalidator-config-table',

    properties: {
      disabled: {
        type: Boolean,
        reflectToAttribute: true,
      },
      entries: {
        type: Array,
        value: [],
      },
    },

    _handleAddEntry() {
      if (this.$.newEntry.value === '') {
        return;
      }

      if (!this.entries.includes(this.$.newEntry.value)) {
        this.push('entries', this.$.newEntry.value);
        this.fire('listChanged', {
          entry: this.$.newEntry.value,
          allEntries: this.entries,
        });
      } else {
        this.fire('show-alert', {message: 'Value already exists.'});
      }
      this.$.newEntry.value = '';
    },

    _handleDeleteEntry(event) {
      this.splice('entries', this.entries.indexOf(event.path[1].id), 1);
      this.fire('listChanged', {
        entry: event.target.id,
        allEntries: this.entries,
      });
    },
  })
})();
