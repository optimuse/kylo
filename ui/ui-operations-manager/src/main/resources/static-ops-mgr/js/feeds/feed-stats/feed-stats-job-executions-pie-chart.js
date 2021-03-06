/*-
 * #%L
 * thinkbig-ui-operations-manager
 * %%
 * Copyright (C) 2017 ThinkBig Analytics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
(function () {

    var directive = function () {
        return {
            restrict: "EA",
            scope: {},
            bindToController: {
                panelTitle: "@",
                refreshIntervalTime: "@",
                feedName: '@'
            },
            controllerAs: 'vm',
            templateUrl: 'js/feeds/feed-stats/feed-stats-job-executions-pie-chart.html',
            controller: "FeedStatsJobExecutionsPieChartController",
            link: function ($scope, element, attrs) {
                $scope.$on('$destroy', function () {

                });
            }
        }

    };

    var controller = function ($scope, $element, $http, $interval, $timeout) {
        var self = this;
        this.dataLoaded = false;

        this.statusPieChartApi = {};

        this.statusPieChartOptions = {
            chart: {
                type: 'pieChart',
                x: function (d) {
                    return d.key;
                },
                y: function (d) {
                    return d.value;
                },
                showLabels: false,
                duration: 100,
                "height": 150,
                labelThreshold: 0.01,
                labelSunbeamLayout: false,
                "margin": {"top": 10, "right": 10, "bottom": 10, "left": 10},
                donut: false,
                // donutRatio: 0.65,
                showLegend: false,
                valueFormat: function (d) {
                    return parseInt(d);
                },
                color: function (d) {
                    if (d.key == 'SUCCESS') {
                        return '#009933';
                    }
                    else if (d.key == 'FAILED') {
                        return '#FF0000';
                    }
                    else if (d.key == 'RUNNING') {
                        return '#FF9901';
                    }
                },
                dispatch: {
                    renderEnd: function () {

                    }
                }
            }
        };

        this.statusPieChartData = [];
        this.statusPieChartData.push({key: "SUCCESS", value: 265})
        this.statusPieChartData.push({key: "FAILED", value: 20})
        this.statusPieChartData.push({key: "RUNNING", value: 26})

        this.successPieChartCount = function () {
            return _.find(self.statusPieChartData, function (data) {
                return data.key == 'SUCCESS';
            }).value;
        }
        this.failedPieChartCount = function () {
            var item = _.find(self.statusPieChartData, function (data) {
                return data.key == 'FAILED';
            });
            return item.value;
        }

        this.updateChart = function (title) {
            var title = title + " Total";
            self.statusPieChartOptions.chart.title = title
            if (self.statusPieChartApi.update) {
                self.statusPieChartApi.update();
            }
        }

        //fetch status
        $timeout(function () {
            self.dataLoaded = true;
        }, 4000);
    };

    angular.module(MODULE_OPERATIONS).controller('FeedStatsJobExecutionsPieChartController', controller);

    angular.module(MODULE_OPERATIONS)
        .directive('tbaFeedStatsJobExecutionsPieChart', directive);

}());
