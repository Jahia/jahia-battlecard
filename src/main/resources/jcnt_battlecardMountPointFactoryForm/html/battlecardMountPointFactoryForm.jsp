<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<template:addResources type="javascript" resources="admin/angular.min.js,jquery.min.js,settings/snackbar.min.js"/>
<template:addResources type="javascript" resources="battlecardMountPointCtrl.js"/>

<div class="page-header">
    <h2><fmt:message key="jcnt_battlecardMountPoint"/></h2>
</div>

<c:url value="/cms/adminframe/default/${renderContext.mainResourceLocale}/settings.manageMountPoints.html"
       var="adminUrl"/>

<div ng-app="battlecardMountPoint" ng-controller="battlecardMountPointCtrl" ng-init='preinit("${adminUrl}")'>
    <div class="panel panel-default">
        <div id="messages" class="panel-heading" ng-if="hasMessage">
            <div class="alert alert-warning" ng-repeat="message in messages.warning track by $index">
                <button type="button" class="close" data-dismiss="alert">&times;</button>
                {{message}}
            </div>
            <div class="alert alert-success" ng-repeat="message in messages.infos track by $index">
                <button type="button" class="close" data-dismiss="alert">&times;</button>
                {{message}}
            </div>
            <div class="alert alert-error alert-danger" ng-repeat="message in messages.errors track by $index">
                <button type="button" class="close" data-dismiss="alert">&times;</button>
                {{message}}
            </div>
        </div>

        <div class="panel-body">
            <div class="box-1 container-fluid">
                <form name="pimForm" method="post" class="form">
                    <fieldset title="local">
                        <div class="row">
                            <div class="form-group label-floating col-md-4">
                                <label for="name" class="control-label"><fmt:message key="jcnt_battlecardMountPoint.name"/> <span style="color:red">*</span></label>
                                <input id="name" ng-model="mountPoint.name" class="form-control" required="required"
                                       ng-readonly="isEditing"/>
                            </div>
                            <div class="form-group label-floating col-md-4">
                                <label for="localPath" class="control-label"><fmt:message key="jcnt_battlecardMountPoint.localPath"/> <span style="color:
                                red">*</span></label>
                                <input id="localPath" ng-model="mountPoint.localPath" class="form-control" required="required"/>
                            </div>
                            <div class="form-group label-floating col-md-4">
                                <label for="cronExpression" class="control-label"><fmt:message key="jcnt_battlecardMountPoint.cronExpression"/> <span style="color:
                                red">*</span></label>
                                <input id="cronExpression" ng-model="mountPoint.cronExpression" class="form-control" required="required"/>
                            </div>
                        </div>
                        <div class="row">
                            <div class="form-group label-floating col-md-4">
                                <label for="projectId" class="control-label"><fmt:message key="jcnt_battlecardMountPoint.projectId"/> <span style="color:
                                red">*</span></label>
                                <input id="projectId" ng-model="mountPoint.projectId" class="form-control" required="required"/>
                            </div>
                            <div class="form-group label-floating col-md-4">
                                <label for="spreadsheetId" class="control-label"><fmt:message key="jcnt_battlecardMountPoint.spreadsheetId"/> <span style="color:
                                red">*</span></label>
                                <input id="spreadsheetId" ng-model="mountPoint.spreadsheetId" class="form-control" required="required"/>
                            </div>
                            <div class="form-group label-floating col-md-4">
                                <label for="excludedSheets" class="control-label"><fmt:message key="jcnt_battlecardMountPoint.excludedSheets"/> <span style="color:
                                red">*</span></label>
                                <input id="excludedSheets" ng-model="mountPoint.excludedSheets" class="form-control" required="required"/>
                            </div>
                        </div>
                        <div class="row">
                            <div class="form-group label-floating col-md-12">
                                <label for="credentials" class="control-label"><fmt:message key="jcnt_battlecardMountPoint.credentials"/> <span style="color:red">*</span></label>
                                <textarea id="credentials" ng-model="mountPoint.credentials" class="form-control" required="required" rows="5"></textarea>
                            </div>
                        </div>
                    </fieldset>
                    <fieldset>
                        <div class="col-md-12">
                            <button class="btn btn-primary btn-raised pull-right" type="submit"
                                    ng-disabled="!pimForm.$valid" ng-click="save()">
                                <span><fmt:message key="label.save"/></span>
                            </button>
                            <button class="btn btn-default pull-right" type="reset" ng-click="cancel()">
                                <span><fmt:message key="label.cancel"/></span>
                            </button>
                        </div>
                    </fieldset>
                </form>
            </div>
        </div>
    </div>
</div>
