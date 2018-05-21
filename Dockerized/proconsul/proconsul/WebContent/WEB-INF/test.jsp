<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
<title>Insert title here</title>
<script type="text/javascript">
function foo() {
	document.getElementById("bodydiv").innerHTML = "orgunit is ${testvalue}";
}
</script>
</head>
<body>
<div id="bodydiv" onClick="foo();">Click Here</div>
</body>
</html>