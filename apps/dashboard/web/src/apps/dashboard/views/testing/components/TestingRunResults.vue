<template>
    <div class="testing-run-results-container">
        <div class="testing-run-header">
            <span class="testing-run-title">{{(testingRun && testingRun.name) || "Tests"}}</span>
            <span>({{endpoints}})</span> | 
            <span>{{getScheduleStr()}}</span> | 
            <span>{{collectionName}}</span>
        </div>

        <div class="testing-runs-history" v-if="!isWorkflow">
            <div class="d-flex jc-end">
                <date-range v-model="dateRange"/>
            </div>
            <stacked-chart
                type='column'
                color='#FF000080'
                :areaFillHex="false"
                :height="250"
                title="Test results"
                :data="testResultsChartData()"
                :defaultChartOptions="{legend:{enabled: false}}"
                background-color="rgba(0,0,0,0.0)"
                :text="true"
                class="pa-5"
                @dateClicked=dateClicked
            />     
            <div class="testing-results-header" v-if="currentTest">
                <span>Test results: </span>    
                <span>{{selectedDateStr()}}</span>
            </div>                  
            <simple-table
                :headers="testingRunResultsHeaders" 
                :items="testingRunResultsItems" 
                name="" 
                sortKeyDefault="vulnerable" 
                :sortDescDefault="true"
                @rowClicked="openDetails"
            >
                <template #item.severity="{item}">
                    <sensitive-chip-group 
                        :sensitiveTags="item.severity ? [item.severity] : []" 
                        :chipColor="getColor(item.severity)"
                        :hideTag="true"
                    />
                </template>
            
            </simple-table>

            <v-dialog v-model="openDetailsDialog">
                <div class="details-dialog">
                    <a-card
                        title="Test details"
                        color="rgba(33, 150, 243)"
                        subtitle=""
                        icon="$fas_stethoscope"
                    >
                        <template #title-bar>
                            <v-btn
                                plain
                                icon
                                @click="openDetailsDialog = false"
                                style="margin-left: auto"
                            >
                                <v-icon>$fas_times</v-icon>
                            </v-btn>
                        </template>
                        <div class="pa-4">
                            <test-results-dialog 
                                :similarlyAffectedIssues="similarlyAffectedIssues"
                                :testingRunResult="testingRunResult"
                                :subCatogoryMap="subCatogoryMap"
                                :issuesDetails="dialogBoxIssue"
                                isTestingPage
                                :mapCollectionIdToName="mapCollectionIdToName"/>
                        </div>
                    </a-card>
                </div>
            </v-dialog>
        </div>
        <div v-else>
            <workflow-test-builder :endpointsList="[]" apiCollectionId=0 :originalStateFromDb="originalStateFromDb" :defaultOpenResult="true" class="white-background"/>
        </div>
    </div>
</template>

<script>
import DateRange from '@/apps/dashboard/shared/components/DateRange'
import ACard from '@/apps/dashboard/shared/components/ACard'
import StackedChart from '@/apps/dashboard/shared/components/charts/StackedChart'
import SimpleTable from '@/apps/dashboard/shared/components/SimpleTable'
import SensitiveChipGroup from '@/apps/dashboard/shared/components/SensitiveChipGroup'
import TestResultsDialog from "./TestResultsDialog";
import WorkflowTestBuilder from '../../observe/inventory/components/WorkflowTestBuilder'

import api from '../api'
import issuesApi from '../../issues/api'

import obj from "@/util/obj"
import func from "@/util/func"
import testing from "@/util/testing"

import {mapState} from "vuex"

export default {
    name: "TestingRunResults",
    props: {
        testingRunHexId: obj.strR,
        defaultStartTimestamp: obj.numN,
        defaultEndTimestamp: obj.numN
    },
    components: {
        DateRange,
        ACard,
        StackedChart,
        SimpleTable,
        SensitiveChipGroup,
        TestResultsDialog,
        WorkflowTestBuilder
    },
    data () {
        let endTimestamp = this.defaultEndTimestamp || func.timeNow()
        return {
            title: "Test",
            testTypes: ["Bola", "Workflow", "Bua"],
            startTimestamp: this.defaultStartTimestamp || (func.timeNow() - func.recencyPeriod/9),
            endTimestamp: endTimestamp,
            selectedDate: +func.dayStart(endTimestamp * 1000) / 1000,
            testingRunResultSummaries: [],
            testingRunResults: [],
            testingRunResultsHeaders: [
                {
                    text: "",
                    value: "color"
                },
                {
                    text: "Endpoint",
                    value: "endpoint"
                },
                {
                    text: "Issue category",
                    value: "testSuperType"
                },
                {
                    text: "Test",
                    value: "testSubType"
                },
                {
                    text: "Severity",
                    value: "severity"
                },
                {
                    text: "Vulnerable",
                    value: "vulnerable"
                }
            ],
            testingRunResult: null,
            openDetailsDialog: false,
            isWorkflow: false,
            originalStateFromDb: null,
            dialogBoxIssue: {},
            similarlyAffectedIssues: []
        }
    },
    methods: {
        getColor(severity) {
            switch (severity) {
                case "HIGH": return "#FF000080"
                case "MEDIUM":  return "#FF5C0080"
                case "LOW": return "#F9B30080"
            }
            
        },
        selectedDateStr() {
            return func.toTimeStr(new Date(this.currentTest.startTimestamp * 1000), true)
        },
        getScheduleStr() {
            return this.isDaily ? "Running daily" : "Run once"
        },
        toHyphenatedDate(epochInMs) {
            return func.toDateStrShort(new Date(epochInMs))
        },
        testResultsChartData () {
            let retH = []
            let retM = []
            let retL = []

            this.testingRunResultSummaries.forEach((x) => {
                let ts = x["startTimestamp"] * 1000
                let countIssuesMap = x["countIssues"]

                let dt = +func.dayStart(ts)
                let s = +func.dayStart(this.startTimestamp*1000)
                let e = +func.dayStart(this.endTimestamp*1000)
                if (dt < s || dt > e) return

                retH.push([ts, countIssuesMap["HIGH"]])
                retM.push([ts, countIssuesMap["MEDIUM"]])
                retL.push([ts, countIssuesMap["LOW"]])
            })

            return [
                {
                    data: retH,
                    color: "#FF000080",
                    name: "High"
                },
                {
                    data: retM,
                    color: "#FF5C0080",
                    name: "Medium"
                },
                {
                    data: retL,
                    color: "#F9B30080",
                    name: "Low"
                }
            ]
        },
        dateClicked(point) {
            this.selectedDate = point / 1000
        },
        refreshSummaries() {
            api.fetchTestingRunResultSummaries(this.startTimestamp, this.endTimestamp, this.testingRunHexId).then(resp => {
                if (resp.testingRun.testIdConfig == 1) {
                    this.isWorkflow = true
                    this.originalStateFromDb = resp.workflowTest
                }
                this.testingRunResultSummaries = resp.testingRunResultSummaries
                this.selectedDate = Math.max(...this.testingRunResultSummaries.map(o => o.startTimestamp))
            })
        },
        getRunResultSubCategory (runResult) {
            debugger
            if (this.subCatogoryMap[runResult.testSubType] === undefined) {
                return this.subCategoryFromSourceConfigMap[runResult.testSubType].subcategory
            } else {
                return this.subCatogoryMap[runResult.testSubType].testName
            }
        },
        getRunResultCategory (runResult) {
            if (this.subCatogoryMap[runResult.testSubType] === undefined) {
                return this.subCategoryFromSourceConfigMap[runResult.testSubType].category.shortName
            } else {
                return this.subCatogoryMap[runResult.testSubType].superCategory.shortName
            }
        },
        prepareForTable(runResult) {
            return {
                ...runResult,
                endpoint: runResult.apiInfoKey.method + " " + runResult.apiInfoKey.url,
                severity: runResult["vulnerable"] ? "HIGH" : null,
                testSubType: this.getRunResultSubCategory (runResult),
                testSuperType: this.getRunResultCategory(runResult)
            }
        },
        async openDetails(row) {
            await api.fetchTestRunResultDetails(row["hexId"]).then(async resp => {
                this.testingRunResult = resp["testingRunResult"]
                if (this.testingRunResult) {
                    await api.fetchIssueFromTestRunResultDetails(row["hexId"]).then(async respIssue => {
                        this.dialogBoxIssue = respIssue['runIssues']
                        if (this.dialogBoxIssue) {
                            await issuesApi.fetchAffectedEndpoints(this.dialogBoxIssue.id).then(affectedResp => {
                                this.similarlyAffectedIssues = affectedResp['similarlyAffectedIssues']
                            })
                        }
                    })
                    this.openDetailsDialog = true
                }
            })
        },
    },
    async mounted() {
        this.refreshSummaries()
        await this.$store.dispatch('issues/fetchAllSubCategories')
    },
    computed: {
        ...mapState('testing', ['testingRuns', 'pastTestingRuns']),
        subCatogoryMap: {
            get() {
                return this.$store.state.issues.subCatogoryMap
            }
        },
        subCategoryFromSourceConfigMap: {
            get() {
                return this.$store.state.issues.subCategoryFromSourceConfigMap
            }
        },
        mapCollectionIdToName() {
            return this.$store.state.collections.apiCollections.reduce((m, e) => {
                m[e.id] = e.displayName
                return m
            }, {})
        },
        testingRun() {
            return [...this.testingRuns, ...this.pastTestingRuns].filter(x => x.hexId === this.testingRunHexId)[0]
        },
        endpoints() {
            return this.testingRun ? testing.getEndpoints(this.testingRun.testingEndpoints) : "-"
        },
        collectionName() {
            if (this.testingRun) {
                return testing.getCollectionName(this.testingRun.testingEndpoints, this.mapCollectionIdToName)
            } else {
                return ""
            }
        },
        dateRange: {
            get () {
                return [this.toHyphenatedDate(this.startTimestamp * 1000), this.toHyphenatedDate(this.endTimestamp * 1000)]
            },
            set(newDateRange) {
                let start = Math.min(func.toEpochInMs(newDateRange[0]), func.toEpochInMs(newDateRange[1]));
                let end = Math.max(func.toEpochInMs(newDateRange[0]), func.toEpochInMs(newDateRange[1]));

                this.startTimestamp = +func.dayStart(start) / 1000
                this.endTimestamp = +func.dayEnd(end) / 1000
                this.selectedDate = this.endTimestamp
                this.refreshSummaries()
            }
        },
        currentTest() {
            let currentSummary = this.testingRunResultSummaries.filter(x => x.startTimestamp === this.selectedDate)[0]
            if (currentSummary) {
                api.fetchTestingRunResults(currentSummary.hexId).then(resp => {
                    this.testingRunResults = resp.testingRunResults
                })
            }
            return currentSummary
        },
        testingRunResultsItems() {
            return (this.testingRunResults || []).map(x => this.prepareForTable(x))
        }
    }
}
</script>

<style lang="sass" scoped>
.testing-run-results-container
    color: #47466A !important
    
.testing-run-title
    font-weight: 500 

.testing-run-header       
    font-size: 14px

.testing-runs-history
    padding: 16px    

.testing-results-header
    font-size: 14px        
    font-weight: 500
    color: #47466A80
</style>