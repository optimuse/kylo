/*-
 * #%L
 * thinkbig-ui-common
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
(function() {

    /**
     * Config
     */
    var moduleName = COMMON_APP_MODULE_NAME;
    var templateUrl = 'js/shared/ui-router/breadcrumbs/uiBreadcrumbs.tpl.html';

    /**
     * Module
     */
    var module;
    try {
        module = angular.module(moduleName);
    } catch(err) {
        // named module does not exist, so create one
        module = angular.module(moduleName, ['ui.router']);
    }

    module.directive('uiRouterBreadcrumbs', ['$interpolate', '$state', function($interpolate, $state) {
        return {
            restrict: 'E',
            templateUrl: function(elem, attrs) {
                return attrs.templateUrl || templateUrl;
            },
            scope: {
                displaynameProperty: '@',
                abstractProxyProperty: '@?'
            },
            link: function($scope) {
                $scope.breadcrumbs = [];
                $scope.lastBreadcrumbs = [];
               /* if ($state.$current.name !== '') {
                    updateBreadcrumbsArray();
                }
                */
                $scope.$on('$stateChangeSuccess',function(event, toState, toParams, fromState, fromParams) {
                    //if the to State exists in the breadcrumb trail, reverse back to that, otherwise add it
                    if(toState.data.noBreadcrumb && toState.data.noBreadcrumb == true ){
                     //console.log('Skipping breadcrumb for ',toState)
                    }else {
                        updateBreadcrumbs(toState, toParams);
                    }
                //    console.log('crumbs ',$scope.breadcrumbs)
                });


                function updateLastBreadcrumbs(){
                        $scope.lastBreadcrumbs = $scope.breadcrumbs.slice(Math.max($scope.breadcrumbs.length - 2,0));
                    }

                function getBreadcrumbKey(state){
                    return state.name;
                }

                function getDisplayName(state) {
                    return state.data.displayName || state.name;
                }

                function isBreadcrumbRoot(state){
                    return state.data.breadcrumbRoot && state.data.breadcrumbRoot == true;
                }

                function addBreadcrumb(state, params){
                    var breadcrumbKey = getBreadcrumbKey(state);
                    var copyParams = {}
                    if(params ) {
                        angular.extend(copyParams, params);
                    }
                    var displayName = getDisplayName(state);
                    $scope.breadcrumbs.push({
                        key:breadcrumbKey,
                        displayName: displayName,
                        route: state.name,
                        params:copyParams
                    });

                    updateLastBreadcrumbs();
                }
                $scope.navigate = function(crumb) {
                    $state.go(crumb.route,crumb.params);
                }

                function getBreadcrumbIndex(state){
                    var breadcrumbKey = getBreadcrumbKey(state);
                    var matchingState = _.find($scope.breadcrumbs,function(breadcrumb){
                        return breadcrumb.key == breadcrumbKey;
                    });
                    if(matchingState){
                        return _.indexOf($scope.breadcrumbs,matchingState)
                    }
                    return -1;
                }
                function updateBreadcrumbs(state,params){
                    var index = getBreadcrumbIndex(state);
                    if(isBreadcrumbRoot(state)){
                        index = 0;
                    }
                    if(index == -1){
                        addBreadcrumb(state,params);
                    }
                    else {
                        //back track until we get to this index and then replace it with the incoming one
                        $scope.breadcrumbs =  $scope.breadcrumbs.slice(0,index);
                        addBreadcrumb(state,params);
                    }
                }
                /**
                 * Resolve the displayName of the specified state. Take the property specified by the `displayname-property`
                 * attribute and look up the corresponding property on the state's config object. The specified string can be interpolated against any resolved
                 * properties on the state config object, by using the usual {{ }} syntax.
                 * @param currentState
                 * @returns {*}
                 */
                function getDisplayName1(currentState) {
                    var interpolationContext;
                    var propertyReference;
                    var displayName;

                    if (!$scope.displaynameProperty) {
                        // if the displayname-property attribute was not specified, default to the state's name
                        return currentState.name;
                    }
                    propertyReference = getObjectValue($scope.displaynameProperty, currentState);

                    if (propertyReference === false) {
                        return false;
                    } else if (typeof propertyReference === 'undefined') {
                        return currentState.name;
                    } else {
                        // use the $interpolate service to handle any bindings in the propertyReference string.
                        interpolationContext =  (typeof currentState.locals !== 'undefined') ? currentState.locals.globals : currentState;
                        displayName = $interpolate(propertyReference)(interpolationContext);
                        return displayName;
                    }
                }

                /**
                 * Given a string of the type 'object.property.property', traverse the given context (eg the current $state object) and return the
                 * value found at that path.
                 *
                 * @param objectPath
                 * @param context
                 * @returns {*}
                 */
                function getObjectValue(objectPath, context) {
                    var i;
                    var propertyArray = objectPath.split('.');
                    var propertyReference = context;

                    for (i = 0; i < propertyArray.length; i ++) {
                        if (angular.isDefined(propertyReference[propertyArray[i]])) {
                            propertyReference = propertyReference[propertyArray[i]];
                        } else {
                            // if the specified property was not found, default to the state's name
                            return undefined;
                        }
                    }
                    return propertyReference;
                }

            }
        };
    }]);
})();
