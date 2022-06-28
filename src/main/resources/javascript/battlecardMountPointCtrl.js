angular.module('battlecardMountPoint', [])
    .controller('battlecardMountPointCtrl', ['$controller', '$scope', '$location', '$anchorScroll', '$http', ($controller, $scope, $location, $anchorScroll, $http) => {
        $scope.adminUrl = '/';
        $scope.mountPoint = {};
        $scope.messages = {errors: [], warnings: [], infos: []};
        $scope.isEditing = false;
        $scope.hasMessage = false;
        $scope.$watch('hasMessage', (newValue, oldValue) => {
            if (newValue) {
                $location.hash('messages');
                $anchorScroll();
            }
        });

        const getUrlParameter = name => {
            const results = new RegExp('[\\?&]' + name.replace(/[\[]/, '\\[').replace(/[\]]/, '\\]') + '=([^&#]*)').exec(location.search);
            return results === null ? '' : decodeURIComponent(results[1].replace(/\+/g, ' '));
        };

        const gqlGetMountPoint = `query getMountPoint($id: String!) {
                      jcr(workspace: EDIT) {
                        nodeById(uuid: $id) {
                          name
                          credentials: property(name: "credentials") { value }
                          projectId: property(name: "projectId") { value }
                          spreadsheetId: property(name: "spreadsheetId") { value }
                          excludedSheets: property(name: "excludedSheets") { values }
                          cronExpression: property(name: "cronExpression") { value }
                          mountPoint: property(name: "mountPoint") { refNode { path } }
                        }
                      }
                    }`;
        const gqlGetIdentifier = `query getIdentifier($path: String!) {
                                      jcr(workspace: EDIT) {
                                        mountPointIdentifier: nodeByPath(path: $path) {
                                          uuid
                                        }
                                      }
                                    }`;
        const gqlCreateMountPoint = `mutation createMountPoint($name:String!, $credentials: String!, $projectId: String!, $spreadsheetId: String!, $excludedSheets: [String!], $cronExpression: String!, $mountPoint: String!) {
                              jcr(workspace: EDIT) {
                                addNode(
                                  parentPathOrId: "/mounts",
                                  name: $name,
                                  primaryNodeType: "jcnt:battlecardMountPoint",
                                  properties: [
                                    {name: "credentials", value: $credentials},
                                    {name: "projectId", value: $projectId},
                                    {name: "spreadsheetId", value: $spreadsheetId},
                                    {name: "excludedSheets", values: $excludedSheets},
                                    {name: "cronExpression", value: $cronExpression},
                                    {name: "mountPoint", value: $mountPoint}
                                  ]
                                ) {
                                  uuid
                                }
                              }
                            }`;
        const gqlEditMountPointProperties = `mutation editMountPoint($id:String!, $credentials: String!, $projectId: String!, $spreadsheetId: String!, $excludedSheets: [String!], $cronExpression: String!, $mountPoint: String!) {
                                  jcr(workspace: EDIT) {
                                    mutateNode(pathOrId: $id) {
                                      credentials: mutateProperty(name: "credentials") { setValue(value: $credentials, type: STRING) }
                                      projectId: mutateProperty(name: "projectId") { setValue(value: $projectId, type: STRING) }
                                      spreadsheetId: mutateProperty(name: "spreadsheetId") { setValue(value: $spreadsheetId, type: STRING) }
                                      excludedSheets: mutateProperty(name: "excludedSheets") { setValues(values: $excludedSheets, type: STRING) }
                                      cronExpression: mutateProperty(name: "cronExpression") { setValue(value: $cronExpression, type: STRING) }
                                      mountPoint: mutateProperty(name: "mountPoint") { setValue(value: $mountPoint, type: WEAKREFERENCE) }
                                    }
                                  }
                                }`;

        $scope.preinit = adminUrl => {
            $scope.adminUrl = adminUrl;
            const identifier = getUrlParameter('edit');
            if (identifier != null && identifier.length > 0) {
                $scope.isEditing = true;
                $http.post('/modules/graphql', JSON.stringify({
                    query: gqlGetMountPoint,
                    variables: {id: identifier}
                })).then(response => {
                    try {
                        if (response.data && response.data.data && response.data.data.jcr && response.data.data.jcr.nodeById) {
                            const properties = response.data.data.jcr.nodeById;
                            $scope.mountPoint = {
                                name: properties.name,
                                credentials: properties.credentials.value,
                                projectId: properties.projectId.value,
                                spreadsheetId: properties.spreadsheetId.value,
                                excludedSheets: properties.excludedSheets.values,
                                cronExpression: properties.cronExpression.value,
                                localPath: properties.mountPoint.refNode?.path
                            };
                        } else {
                            $.snackbar({content: 'Data not found', style: 'error'});
                        }
                    } catch (e) {
                        throw new Error(e);
                    }
                }).catch(e => $.snackbar({content: `Error ${e.message}`, style: 'error'}));
            }
        };

        $scope.cancel = () => {
            window.location = $scope.adminUrl;
        };

        const updateMountPointProperties = (identifier, mountPointIdentifier) => {
            $http.post('/modules/graphql', JSON.stringify({
                query: gqlEditMountPointProperties,
                variables: {
                    id: identifier,
                    credentials: $scope.mountPoint.credentials,
                    projectId: $scope.mountPoint.projectId,
                    spreadsheetId: $scope.mountPoint.spreadsheetId,
                    excludedSheets: $scope.mountPoint.excludedSheets,
                    cronExpression: $scope.mountPoint.cronExpression,
                    mountPoint: mountPointIdentifier
                }
            })).then(response => {
                if (response.data && response.data.data && response.data.data.jcr && response.data.data.jcr.mutateNode) {
                    $scope.cancel();
                } else {
                    $.snackbar({content: 'Data not found', style: 'error'});
                }
            }).catch(e => $.snackbar({content: `Error ${e.message}`, style: 'error'}));
        };

        $scope.save = () => {
            $http.post('/modules/graphql', JSON.stringify({
                query: gqlGetIdentifier,
                variables: {path: $scope.mountPoint.localPath}
            })).then(response => {
                if (response.data && response.data.data && response.data.data.jcr && response.data.data.jcr.mountPointIdentifier) {
                    const mountPointIdentifier = response.data.data.jcr.mountPointIdentifier.uuid;
                    const identifier = getUrlParameter('edit');
                    if (identifier == null || identifier.length === 0) {
                        $http.post('/modules/graphql', JSON.stringify({
                            query: gqlCreateMountPoint,
                            variables: {
                                name: $scope.mountPoint.name,
                                credentials: $scope.mountPoint.credentials,
                                projectId: $scope.mountPoint.projectId,
                                spreadsheetId: $scope.mountPoint.spreadsheetId,
                                excludedSheets: $scope.mountPoint.excludedSheets.split(','),
                                cronExpression: $scope.mountPoint.cronExpression,
                                mountPoint: mountPointIdentifier
                            }
                        })).then(response => {
                            if (response.data && response.data.data && response.data.data.jcr && response.data.data.jcr.addNode) {
                                $scope.cancel();
                            } else {
                                $.snackbar({content: 'Data not found', style: 'error'});
                            }
                        }).catch(e => $.snackbar({content: `Error ${e.message}`, style: 'error'}));
                    } else {
                        updateMountPointProperties(identifier, mountPointIdentifier);
                    }
                } else {
                    $.snackbar({content: 'Data not found', style: 'error'});
                }
            }).catch(e => $.snackbar({content: `Error ${e.message}`, style: 'error'}));
        };
    }]);
