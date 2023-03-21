<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri = "http://java.sun.com/jsp/jstl/functions" prefix = "fn" %>

<%@ page import="java.util.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <title>Proconsul: Console access for the provinces</title>
    <spring:url value="/resources/style.css" var="stylecss"/>
    <link rel="stylesheet" media="all" href="${stylecss}" />
    <spring:url value="/resources/main.js" var="mainjs" />
    <script src="${mainjs}"></script>
    <spring:url value="/resources/jquery-1.12.0.js" var="jquery" />
    <script src="${jquery}"></script>
    <spring:url value="/resources/spin.min.js" var="spin" />
    <script src="${spin}"></script>
    <spring:url value="/resources/img/header_proconsul.png" var="header_proconsul" />
    
    <script type="text/javascript">
$(document).ready(
	function() {
		$('.actionButton').click(function() { 
			$('#main').spin("modal");
			if (this.id.startsWith("expire")) {
				var foo = this.id.replace("expire","");
				$("#expirekey"+foo).val("true");
			}
			if (this.id.startsWith("extend")) {
				var bar = this.id.replace("extend","");
				$("#extendkey"+bar).val("true");
			}
		})
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
    <div id="header">
      <img src="${header_proconsul}" alt="Duke Proconsul">
      <div id="user">
      	<a href="${logouturl}">sign out ${authenticatedUser.uid}</a>
      </div>
    </div>
    <div id="main">
      
      
      
      
      <table class="sessions">
        
        <tbody class="${userclass}">
          <tr><th colspan="7" class="banner">User Sessions</th></tr>
          <c:forEach items="${userSessionList}" var="userSession" varStatus="status">
          <form:form id="userReconForm${status.index}" method="POST" commandName="userRecon" action="/proconsul/reconnect">
          <form:hidden path="targetFQDN" value="${userSession.fqdn}"/>
          <form:hidden path="csrfToken" value="${userRecon.csrfToken}"/>
          <tr>
            <td>${userSession.fqdn}</td>
            <td>${userSession.displayname}<br>
            <div class="session-option" style="display:inline"><form:radiobutton path="rresolution" value="default"/>Default (1024x768)</div><div class="session-option" style="display:inline"><form:radiobutton path="rresolution" value="large"/>Large (1920x1200)</div></td>     
            <td colspan="5"><button class="actionButton" type="submit">Reconnect</button></td>
          </tr>
          </form:form>
          </c:forEach>
          <form:form id="userConnectForm" method="POST" commandName="usersession" action="/proconsul/usersession">
          <tr>
            <td><div class="session-option"><label>Host Name:</label><form:select path="targetFQDN" items="${userhosts}"></form:select></div><br>
            	<div class="session-option" style="display:inline"><form:radiobutton path="resolution" value="default"/>Default (1024x768)</div><div class="session-option" style="display:inline"><form:radiobutton path="resolution" value="large"/>Large (1920x1200)</div></td>
            <td><div class="session-option"><label>Session Display Name:</label><form:input path="displayName" maxLength="20"></form:input></div></td>
            <td colspan="5"><div class="session-option"><label>&nbsp;</label><button class="actionButton" type="submit">Connect</button></div></td>
          </tr>
          <form:hidden path="csrfToken" value="${usersession.csrfToken}"/>
          </form:form>
        </tbody>
        
        <tbody class="${staticclass}">
        	<tr><th colspan="7" class="banner">Static User Sessions</th></tr>
        	<c:forEach items="${staticSessionList}" var="staticSession" varStatus="status">
        		<form:form id="staticReconForm${status.index}" method="POST" commandName="staticRecon" action="/proconsul/reconnect">
        		<form:hidden path="targetFQDN" value="${staticSession.fqdn}"/>
        		<form:hidden path="csrfToken" value="${staticRecon.csrfToken}"/>
        		<tr>
        			<td>${staticSession.fqdn}</td>
        			<td>${staticSession.displayname}<br>
					<div class="session-option" style="display:inline"><form:radiobutton path="rresolution" value="default"/>Default (1024x768)</div><div class="session-option" style="display:inline"><form:radiobutton path="rresolution" value="large"/>Large (1920x1200)</div><!--<br>-->
						<!--<div class="session-option" style="display:inline"><form:radiobutton path="rresolution" value="vncdefault"/>VNC small (1024x768)</div><div class="session-option" style="display:inline"><form:radiobutton path="rresolution" value="vnclarge"/>VNC large (1920x1200)</div>--></td>
        			<td colspan="4"><button class="actionButton" type="submit">Reconnect</button></td>
        		</tr>
        		</form:form>
        	</c:forEach>
        	<form:form id="statiConnectForm" method="POST" commandName="staticsession" action="/proconsul/staticsession">
        	<tr>
        		<td><div class="session-option"><label>Host Name:</label><form:select path="targetFQDN" items="${statichosts}"></form:select></div><br>
				<div class="session-option" style="display:inline"><form:radiobutton path="resolution" value="default"/>Default (1024x768)</div><div class="session-option" style="display:inline"><form:radiobutton path="resolution" value="large"/>Large (1920x1200)</div><!--<br>-->
						<!--<div class="session-option" style="display:inline"><form:radiobutton path="resolution" value="vncdefault"/>VNC small (1024x768)</div><div class="session-option" style="display:inline"><form:radiobutton path="resolution" value="vnclarge"/>VNC large (1920x1200)</div>--></td>
        		<td><div class="session-option"><label>Session Display Name:</label><form:input path="displayName" maxLength="20"></form:input></div></td>
        		<td colspan="5"><div class="session-option"><label>&nbsp;</label><button class="actionButton" type="submit">Connect</button></div></td>
        	</tr>
        	<form:hidden path="csrfToken" value="${staticsession.csrfToken}"/>
        	</form:form>
        </tbody>
        </tbody>
        
        <tbody class="${checkedoutclass}">
        	<tr><th colspan="7" class="banner">Checked-Out Credentials</th></tr>
        	<c:forEach items="${checkedoutcreds}" var="checkedoutcred" varStatus="status">
        	<form:form id="checkedOutCred${status.index}" method="POST" commandName="updateCheckOut" action="/proconsul/updateCheckOut">
        	<form:hidden path="targetHost" value="${checkedoutcred.targetHost}"/>
        	<form:hidden path="csrfToken" value="${updateCheckOut.csrfToken}"/>
        	<input type="hidden" name="targetUser" value="${checkedoutcred.targetUser.sAMAccountName}"/>
			<input type="hidden" name="owner" value="${checkedoutcred.authUser.uid}" />
        	<input type="hidden" name="expireNow" id="expirekey${status.index}" value="false"/>
        	<input type="hidden" name="extendNow" id="extendkey${status.index}" value="false"/>
        	<tr>
        		<td>${checkedoutcred.targetUser.sAMAccountName}</td>
        		<td>${checkedoutcred.expirationDate}</td>
        		<td>${checkedoutcred.targetHost}</td>
        		<td>${checkedoutcred.reason}</td>
        		<td><button id="expire${status.index}" class="actionButton">Expire Now</button></td>
        		<td><button id="extend${status.index}" class="actionButton">Extend 2Hrs</button></td>
        	</tr>
        	</form:form>
        	</c:forEach>
        	<form:form id="checkOutCredForm" method="POST" commandName="checkreq" action="/proconsul/checkOut">
        		<tr>
        			<td><div class="session-option"><label>Target Host FQDN:</label><form:input path="targetHost"></form:input></div></td>
        			<td><div class="session-option"><label>Reason:</label><form:input path="reason"></form:input></div></td>
        			<td><div class="session-option"><label>Expiration (hours, 2-12):</label><form:input path="lifetime"></form:input></div></td>
        		    <td><div class="session-option"><label>Groups:</label><c:forEach items="${allowedgroups}" var="allowedgroup" varStatus="status"><c:set var="g1" value="${allowedgroup}"/><c:set var="g2" value="${fn:replace(g1,',','|')}"/><div><form:checkbox path="groups" value="${g2}"></form:checkbox>${allowedgroup}</div></c:forEach></div></td>
        		    <td colspan="3"><div class="session-option"><label>&nbsp;</label><button class="actionButton" type="submit">CheckOut</button></div></td>
        		
        		</tr>
        		<form:hidden path="csrfToken" value="${checkreq.csrfToken}"/>
        	</form:form>
        </tbody>
        
        <tbody class="${daclass}">
          <tr><th colspan="7" class="banner">Domain Admin Sessions</th></tr>
          <c:forEach items="${domainSessionList}" var="domainSession" varStatus="status">
          <form:form id="domainReconForm${status.index}" method="POST" commandName="domainRecon" action="/proconsul/reconnect">
          <form:hidden path="targetFQDN" value="${domainSession.fqdn}"/>
          <form:hidden path="csrfToken" value="${domainRecon.csrfToken}"/>
          <tr>
            <td>${domainSession.fqdn}</td>
            <td>${domainSession.displayname}<br>
            <div class="session-option" style="display:inline"><form:radiobutton path="rdaresolution" value="default"/>Default (1024x768)</div><div class="session-option" style="display:inline"><form:radiobutton path="rdaresolution" value="large"/>Large (1920x1200)</div></td>
            
            <td colspan="5"><button class="actionButton" type="submit">Reconnect</button></td>
          </tr>
          </form:form>
          </c:forEach>
          <form:form id="domainConnectForm" method="POST" commandName="domadmin" action="/proconsul/domainadmin">
          <tr>
            <td><div class="session-option"><label>Host Name:</label><form:select path="targetFQDN" items="${domainhosts}"></form:select></div>
            	<!--<div class="session-option">Display password? <form:checkbox path="exposePassword" disabled="true"/></div><br>-->
            	<div class="session-option" style="display:inline"><form:radiobutton path="daresolution" value="default"/>Default (1024x768)</div><div class="session-option" style="display:inline"><form:radiobutton path="daresolution" value="large"/>Large (1920x1200)</div></td>
            <td><div class="session-option"><label>Session Display Name:</label><form:input path="displayName" maxLength="20"></form:input></div></td>
            <td colspan="5"><div class="session-option"><label>&nbsp;</label><button class="actionButton" type="submit">Connect</button></div></td>
          </tr>
          <form:hidden path="csrfToken" value="${domadmin.csrfToken}"/>
          </form:form>
        </tbody>
        
        <tbody class="${delegatedclass}">
          <tr><th colspan="7" class="banner">Delegated OU Admin Sessions</th></tr>
          <c:forEach items="${delegatedSessionList}" var="delegatedSession" varStatus="status">
          <form:form id = "delegatedReconForm${status.index}" method="POST" commandName="delegatedRecon" action="/proconsul/reconnect">
            <form:hidden path="targetFQDN" value="${delegatedSession.fqdn}"/>
            <form:hidden path="csrfToken" value="${delegatedRecon.csrfToken}"/>
          <tr>
          	<td><div class="session-option"><label>Org Unit: ${delegatedSession.delegatedOU}</label></div>
          		<div class="session-option"><label>Host Name: ${delegatedSession.fqdn}</label></div>
          		<div class="session-option"><label>Role: ${delegatedSession.delegatedRole}</label></div>
          	</td>
            <td>${delegatedSession.displayname}</td>
            <td colspan="5"><button class="actionButton" type="submit">Reconnect</button></td>
          </tr>
          </form:form>
          </c:forEach>
          <form:form id="delegatedConnectForm" method="POST" commandName="deladmin" action="/proconsul/deladmin">
          <tr>
            <td><div class="session-option">
                  <label>Org Unit:</label>
                  <form:select path="orgUnit" id="orgUnit" items="${delegatedOUs}" itemLabel="name" itemValue="value"></form:select>
                </div>
                <div class="session-option">
                  <label>Host Name:</label>
                  <form:select path="hostName" id="hostName"></form:select>
                </div>
                <div class="session-option">
                  <label>Role:</label>
                  <form:radiobuttons class="ou_role" path="roleGroup" name="roleGroup" items="${roleGroups}" itemLabel="name" itemValue="value"></form:radiobuttons> 
                </div>
            </td>
            <td><div class="session-option"><label>Session Display Name:</label><form:input path="displayName" maxLength="20"></form:input></div></td>
            <td colspan="5"><div class="session-option"><label>&nbsp;</label><button class="actionButton" type="submit">Connect</button></div></td>
          </tr>
          <form:hidden path="csrfToken" value="${deladmin.csrfToken}"/>
          </form:form>
        </tbody>
      </table>

    </div>
  </body>
</html>




