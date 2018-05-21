<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
<spring:url value="/resources/main.js" var="mainjs" />
<script src="${mainjs}"></script>
<spring:url value="/resources/jquery-1.12.0.js" var="jquery" />
<script src="${jquery}"></script>
<spring:url value="/resources/main.css" var="maincss" />
<link href="${maincss}" rel="stylesheet" />
<spring:url value="/resources/proconsul.css" var="proconsulcss" />
<link href="${proconsulcss}" rel="stylesheet" />
<title>Proconsul Access Error</title>
</head>
<body>
<div>
 <header id="branding">
 <div id="logo">
 <p id="logop">
   <a href="http://www.duke.edu" tabindex="1" accesskey="1">
    <img src="/proconsul/resources/logo.png" width="141" height="64" alt="Duke University">
   </a></p>
 <h1 id="appname"><a href="/proconsul/" class="white" rel="home" tabindex="2" accesskey="2">Proconsul</a></h1>
 <span class="logout"><a href="${logouturl}">Logout</a></span>
</div>
</header>
</div>
<h3>Access Denied or Service Not Available</h3>
<p>
${message}
</p>
</body>
</html>