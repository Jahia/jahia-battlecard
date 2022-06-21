<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<template:addResources type="css" resources="custom.css"/>

<c:set var="masterBattlecard" value="${currentNode.properties['masterBattlecard'].node}"/>
<c:set var="battlecard2" value="${currentNode.properties['battlecard2'].node}"/>

<div class="bluebg">
    <div class="container">
        <div class="battlecard simplebox">
            <div class="d-flex battlecard-title-sticky">
                <h1 class="battlecard-col">${masterBattlecard.properties['jcr:title'].string}</h1>
                <h1 class="battlecard-col">${battlecard2.properties['jcr:title'].string}</h1>
            </div>

            <c:forEach items="${jcr:getChildrenOfType(masterBattlecard, 'jcnt:battlecardCategory')}" var="category1">
                <c:if test="${not category1.properties['isMasterCategory'].boolean}">

                    <jcr:node var="category2" path="${battlecard2.path}/${category1.name}"/>

                    <div>
                        <div class="d-flex battlecard-section-sticky">
                            <h2 class="battlecard-col">${category1.properties['jcr:title'].string}</h2>
                            <h2 class="battlecard-col">${category2.properties['jcr:title'].string}</h2>
                        </div>
                        <c:forEach items="${jcr:getChildrenOfType(category1, 'jcnt:battlecardKeyValue')}"
                                   var="keyValue1">
                            <jcr:node var="keyValue2" path="${category2.path}/${keyValue1.name}"/>

                            <jcr:nodeProperty node="${keyValue1}" name="key" var="key1"/>
                            <jcr:nodeProperty node="${keyValue1}" name="value" var="value1"/>
                            <jcr:nodeProperty node="${keyValue2}" name="key" var="key2"/>
                            <jcr:nodeProperty node="${keyValue2}" name="value" var="value2"/>

                            <div class="battlecard-row d-flex">
                                <div class="battlecard-col">
                                    <dt><c:if test="${not empty key1 && key1 != ''}">${key1.string}: </c:if></dt>
                                    <dd>${value1.string}</dd>
                                </div>
                                <div class="battlecard-col">
                                    <dt><c:if test="${not empty key2 && key2 != ''}">${key2.string}: </c:if></dt>
                                    <dd>${value2.string}</dd>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:if>
            </c:forEach>


            <c:forEach items="${jcr:getChildrenOfType(masterBattlecard, 'jcnt:battlecardCategory')}"
                       var="masterCategory">

                <c:if test="${masterCategory.properties['isMasterCategory'].boolean}">
                    <div class="simplebox">
                        <h2>${masterCategory.properties['jcr:title'].string}</h2>
                        <ul>
                            <c:forEach items="${jcr:getChildrenOfType(masterCategory, 'jcnt:battlecardKeyValue')}"
                                       var="keyValue">
                                <jcr:nodeProperty node="${keyValue}" name="key" var="key"/>
                                <jcr:nodeProperty node="${keyValue}" name="value" var="value"/>
                                <li><c:if test="${not empty key && key != ''}">${key.string}: </c:if>
                                        ${value.string}</li>
                            </c:forEach>
                        </ul>
                    </div>
                </c:if>
            </c:forEach>
        </div>
    </div>
</div>
