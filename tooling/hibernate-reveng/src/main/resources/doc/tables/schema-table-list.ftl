<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
	<head>
		<title>Hibernate Mappings - Table List</title>
		<link rel="stylesheet" type="text/css" href="${docFileManager.getRef(docFile, docFileManager.getCssStylesDocFile())}" title="Style"/>
	</head>
	<body class="List">

		<p class="ListTitleFont">
			<a href="${docFileManager.getRef(docFile, docFileManager.getSchemaSummaryDocFile(title))}" target="generalFrame">${title}</a>
		</p>
		
		<p>
			<#foreach table in tableList>
				<a href="${docFileManager.getRef(docFile, docFileManager.getTableDocFile(table))}" target="generalFrame">${table.name}</a><br/>
			</#foreach>
		</p>

	</body>
</html>