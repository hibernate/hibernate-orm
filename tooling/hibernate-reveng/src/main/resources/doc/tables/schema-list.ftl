<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
	<head>
		<title>Hibernate Mappings - Schema List</title>
		<link rel="stylesheet" type="text/css" href="${docFileManager.getRef(docFile, docFileManager.getCssStylesDocFile())}" title="Style"/>
	</head>
	<body class="List">

		<p>
			<a href="${docFileManager.getRef(docFile, docFileManager.getAllTablesDocFile())}" target="tablesFrame">All Tables</a>
		</p>

		<p class="ListTitleFont">
			Schemas
		</p>

		<p>
			<#foreach schema in schemaList>
				<a href="${docFileManager.getRef(docFile, docFileManager.getSchemaTableListDocFile(schema))}" target="tablesFrame">${schema}</a><br/>
			</#foreach>
		</p>

	</body>
</html>
