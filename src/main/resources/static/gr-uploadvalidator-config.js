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
    is: 'gr-uploadvalidator-config',

    properties: {
      repoName: String,
      _config: Object,
      _changedConfig: Object,
      _prefsChanged: {
        type: Boolean,
        value: false,
      },
    },

    listeners: {
      listChanged: '_handleListDataChanged',
    },

    attached() {
      this._getPreferences().then(() => {
        this._changedConfig = Object.assign(new Object(), this._config);

        this.$.allowedAuthorEmailPattern.entries =
          this._config.allowedAuthorEmailPattern.values;
        this.$.allowedCommitterEmailPattern.entries =
          this._config.allowedCommitterEmailPattern.values;
        this.$.binaryTypes.entries = this._config.binaryTypes.values;
        this.$.blockedContentType.entries =
          this._config.blockedContentType.values;
        this.$.blockedFileExtension.entries =
          this._config.blockedFileExtension.values;
        this.$.blockedKeywordPattern.entries =
          this._config.blockedKeywordPattern.values;
        this.$.invalidFilenamePattern.entries =
          this._config.invalidFilenamePattern.values;
        this.$.project.entries = this._config.project.values;
        this.$.ref.entries = this._config.ref.values;
        this.$.requiredFooter.entries = this._config.requiredFooter.values;
      });
    },

    _getPreferences() {
      return this.plugin.restApi('/projects/')
        .get(`${this.repoName}/config`)
        .then(config => {
          if (!config) {
            return;
          }

          if (config['plugin_config']
            && config['plugin_config']['uploadvalidator']) {
            this._config = config['plugin_config']['uploadvalidator'];
          }
        })
    },

    _handleListDataChanged(event) {
      this._changedConfig[event.target.id] = { values: event.detail.allEntries };
      this._handlePrefsChanged();
    },

    _handlePrefsChanged() {
      this._prefsChanged = true;
    },

    _handlePrefsSave() {
      let body = { plugin_config_values: new Object() };
      body['plugin_config_values']['uploadvalidator'] = this._changedConfig;

      this.plugin.restApi('/projects/')
        .put(`${this.repoName}/config`, body)
        .then(() => {
          this._prefsChanged = false;
        }).catch(response => {
          this.fire('show-error', { message: response });
        });
    },

  });
})();
