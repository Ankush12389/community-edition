<template>
    <layout-with-tabs title="Settings" :tabs="getTabs()">
        <template slot="Data types">
            <data-types title="Data types" :data_types="data_types" :toggleActivateFieldFunc='toggleActivateDataTypes'
                :createNewDataType="createNewDataType" @selectedEntry="selectedDataType">
                <template #details-container="{}">
                    <a-card title="Details" color="rgba(33, 150, 243)" style="min-height: 600px">
                        <data-type-details :data_type="data_type" />
                    </a-card>
                </template>
            </data-types>
        </template>
        <template slot="Auth types">
            <data-types title="Auth types" :data_types="auth_types" :toggleActivateFieldFunc='toggleActivateAuthTypes'
                :createNewDataType="createNewAuthType" @selectedEntry="selectedAuthType">
                <template #details-container="{}">
                    <a-card title="Details" color="rgba(33, 150, 243)" style="min-height: 600px">
                        <auth-type-details :auth_type_copy="auth_type" />
                    </a-card>
                </template>
            </data-types>
        </template>
        <template slot="Tags">
            <data-types title="Tags" :data_types="tag_configs" :toggleActivateFieldFunc='toggleActivateTagConfig'
                :createNewDataType="createNewTagConfig" @selectedEntry="selectedTagConfig">
                <template #details-container="{}">
                    <a-card title="Details" color="rgba(33, 150, 243)" style="min-height: 600px">
                        <tag-config-details :tag_config_copy="tag_config" />
                    </a-card>
                </template>
            </data-types>
        </template>
        <template slot="Integrations">
            <integration-center />
        </template>
        <template slot="Account">
            <div>
                <account />
                <div class="px-8">
                    <div class="py-4">
                        <v-dialog v-model="showDialog" width="500">
                            <template v-slot:activator="{ on, attrs }">
                                <v-btn primary dark color="#6200EA" @click="showDialog = true" v-bind="attrs" v-on="on">
                                    Update Akto
                                </v-btn>
                            </template>
                            <div class="dialog-box">
                                <div class="entry-text"> Please note that this will incur a downtime of 10 mins to
                                    update the system. </div>
                                <div class="d-flex jc-end">
                                    <v-btn primary dark color="#6200EA" @click="takeUpdate">
                                        Proceed
                                    </v-btn>
                                </div>
                            </div>
                        </v-dialog>
                    </div>
                </div>
            </div>
        </template>
        <template slot="Users">
            <team-overview />
        </template>
        <template slot="Health">
            <health />
        </template>
    </layout-with-tabs>
</template>

<script>
import LayoutWithTabs from '@/apps/dashboard/layouts/LayoutWithTabs'
import TeamOverview from './components/TeamOverview'
import Health from './components/Health'
import api from './api'
import DataTypes from './components/data_types/DataTypes.vue'
import Account from './components/Account'
import DataTypeDetails from './components/data_types/components/DataTypeDetails.vue'
import TagSettings from './components/tag_configs/TagSettings.vue'
import TagConfigDetails from './components/tag_configs/TagConfigDetails.vue'
import ACard from '@/apps/dashboard/shared/components/ACard'
import IntegrationCenter from './components/integrations/IntegrationCenter'
import AuthTypeDetails from './components/auth_types/AuthTypeDetails.vue'

import { mapState } from 'vuex'
export default {
    name: "PageSettings",
    components: {
        LayoutWithTabs,
        TeamOverview,
        Health,
        Account,
        IntegrationCenter,
        DataTypes,
        TagSettings,
        TagConfigDetails,
        DataTypeDetails,
        AuthTypeDetails,
        ACard
    },
    mounted() {
        this.$store.dispatch("data_types/fetchDataTypes")
        this.$store.dispatch("tag_configs/fetchTagConfigs")
        this.$store.dispatch("auth_types/fetchCustomAuthTypes")
    },
    data() {
        return {
            showDialog: false
        }
    },
    methods: {
        createNewDataType() {
            return this.$store.dispatch('data_types/setNewDataType')
        },
        toggleActivateDataTypes(item) {
            return this.$store.dispatch('data_types/toggleActiveParam', item)
        },
        selectedDataType(item) {
            this.$store.state.data_types.data_type = item
        },
        createNewTagConfig() {
            return this.$store.dispatch('tag_configs/setNewTagConfig')
        },
        toggleActivateTagConfig(item) {
            return this.$store.dispatch('tag_configs/toggleActiveTagConfig', item)
        },
        selectedTagConfig(item) {
            this.$store.state.tag_configs.tag_config = item
        },
        createNewAuthType() {
            return this.$store.dispatch('auth_types/setNewAuthType')
        },
        toggleActivateAuthTypes(item) {
            return this.$store.dispatch('auth_types/toggleActivateAuthType',item)
        },
        selectedAuthType(item) {
            this.$store.state.auth_types.auth_type = item
        },
        getActiveAccount() {
            return this.$store.state.auth.activeAccount
        },
        takeUpdate() {
            this.showDialog = false
            api.takeUpdate()

        },
        getTabs() {
          if(window.DASHBOARD_MODE && window.DASHBOARD_MODE.toLowerCase() === 'local_deploy'){
            return ['Data types','Auth types', 'Tags', 'Users', 'Integrations'];
          }
          return ['Data types','Auth types', 'Tags', 'Account', 'Users', 'Health', 'Integrations'];
        }
    },
    computed: {
        ...mapState('data_types', ['data_types', 'data_type']),
        ...mapState('tag_configs', ['tag_configs', 'tag_config']),
        ...mapState('auth_types', ['auth_types', 'auth_type'])
    }
}
</script>
<style lang="sass" scoped>
.entry-text
    font-weight: 500
    margin-right: 16px
    color: #47466A

.dialog-box
    padding: 16px
    background: #ffffff    
</style>