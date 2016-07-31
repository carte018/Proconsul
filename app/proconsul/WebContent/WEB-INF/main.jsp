<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
<title>Insert title here</title>
<spring:url value="/resources/main.js" var="mainjs" />
<script src="${mainjs}"></script>
<spring:url value="/resources/jquery-1.12.0.js" var="jquery" />
<script src="${jquery}"></script>
<spring:url value="/resources/spin.min.js" var="spin" />
<script src="${spin}"></script>
<spring:url value="/resources/main.css" var="maincss" />
<link href="${maincss}" rel="stylesheet" />
<spring:url value="/resources/proconsul.css" var="proconsulcss" />
<link href="${proconsulcss}" rel="stylesheet" />
<script type="text/javascript">
$(document).ready(
	function() {
		$('.actionButton').click(function() { $('#main').spin("modal");})
		$('#hostName').prop('disabled',true);
		$.getJSON('fqdn/'+$('#orgUnit').val(),{ajax:'true'},
				function(data) {
					var initial = '<option value="">Hostname</option>';
					var ilist = data.hosts;
					for (var i in ilist) {
						initial += '<option value="'+ilist[i]+'">'+ilist[i]+'</option>';
					}
					initial += '</option>';
					$('#hostName').html(initial);
					$('#hostName').prop('disabled',false);
				});
		$('#orgUnit').change(
			function() {
				$('#hostName').prop('disabled',true);
				
				$.getJSON('fqdn/'+$(this).val(), {ajax:'true'},
			 function(data) {
					var html = '<option value="">Hostname</option>';
					var hlist = data.hosts;
					for (var i in hlist) {
						html += '<option value="' + hlist[i] +'">' + hlist[i] + '</option>';
					}
					html += '</option>';
					$('#hostName').html(html);
					$('#hostName').prop('disabled',false);
				});
				
			});
	});
</script>

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
<div id="errmessage">
${errMessage}
</div>
<div id="main">
<div id="resumediv" class="${resumeclass}">
<div id="disconnectedHeading"><h2>Resumable Sessions</h2></div>
<form:form method="POST" commandName="recon" action="/proconsul/reconnect">

<table class='fullWidthTable' id='disconnectedTable'>
	<tr>
		<td class="tdcell"><form:label path="targetFQDN">Available Disconnected Sessions:</form:label></td>
		<td class="tdcell"><form:select id="disconnectedSessions" path="targetFQDN" items="${sessionlist}" itemLabel="displayname" itemValue="fqdn" onMouseOver="document.getElementById('disconnectedSessions').title=document.getElementById('disconnectedSessions').value;"></form:select></td>
		<td class="tdcell"><input class="actionButton" type="Submit" value="Reconnect to this session"/></td>
	</tr>
</table>
</form:form>
</div>
<div id="dadiv" class="${daclass}">
<div id="availableHeading"><h2>Domain Admin Sessions</h2></div>
<form:form method="POST" commandName="domadmin" action="/proconsul/domainadmin">
<table class='fullWidthTable' id='availableTable'>
	<tr>
		<td class="tdcell"><form:label path="targetFQDN">Select a host to start a Domain Admin session</form:label>
		<br><form:select path="targetFQDN" items="${domainhosts}"></form:select></td>
		<td class="tdcell"><form:label path="exposePassword">Display Password?</form:label>
		<br><form:checkbox path="exposePassword"></form:checkbox></td>
		<td class="tdcell"><form:label path="displayName">Optional DisplayName:</form:label>
		<br><form:input path="displayName" maxLength="20"></form:input></td>
		<td class="tdcell"><input class="actionButton" type="Submit" value="Connect"/></td></tr></table>
</form:form>
</div>
<div id="delegateddiv" class="${delegatedclass}">
<div id="delegatedHeading"><h2>Delegated OU Admin Sessions</h2></div>
<form:form method="POST" commandName="deladmin" action="/proconsul/deladmin">
<table class="fullWidthTable">
	<tr>
		<td class="tdcell"><form:label path="orgUnit">Select an OrgUnit to Manage</form:label>
		<br><form:select path="orgUnit" id="orgUnit" items="${delegatedOUs}" itemLabel="name" itemValue="value"></form:select></td>
		<td class="tdcell"><form:select path="hostName" id="hostName"></form:select></td>
		<td class="tdcell"><form:radiobuttons path="roleGroup" id="roleGroup" items="${roleGroups}" itemLabel="name" itemValue="value" itemTitle="value" delimiter="<br>"></form:radiobuttons></td>
		<td class="tdcell"><form:label path="displayName">Optional DisplayName:</form:label>
		<br><form:input path="displayName" maxLength="20"></form:input></td>
		<td class="tdcell"><input class="actionButton" type="submit" value="Connect"/></td>
	</tr>
</table>
</form:form>
</div>
<div id="userdiv" class="${userclass}">
<div id="userHeading"><h2>Normal User Sessions</h2></div>
<form:form method="POST" commandName="usersession" action="/proconsul/usersession">
<table class="fullWidthTable">
	<tr>
		<td class="tdcell"><form:select path="targetFQDN" items="${userhosts}"></form:select></td>
		<td class="tdcell"><form:label path="displayName">Optional DisplayName:</form:label>
		<br><form:input path="displayName" maxLength="20"></form:input></td>
		<td class="tdcell"><input class="actionButton" type="submit" value="Connect"/></td>
	</tr>
</table>
</form:form>
</div></div>
</body>
</html>