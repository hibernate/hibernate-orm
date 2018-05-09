<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
	<head>
		<title>Hibernate Mappings - Package List</title>
		<link rel="stylesheet" type="text/css" href="${docFileManager.getRef(docFile, docFileManager.getCssStylesDocFile())}" title="Style"/>
	</head>
	<body class="List">

		<p>
			<a href="${docFileManager.getRef(docFile, docFileManager.getAllEntitiesDocFile())}" target="entitiesFrame">All Entities</a>
		</p>

		<p class="ListTitleFont">
			Packages
		</p>
		<p>
			<#foreach package in packageList>
				<a href="${docFileManager.getRef(docFile, docFileManager.getPackageEntityListDocFile(package))}" target="entitiesFrame">${package}</a><br/>
			</#foreach>
		</p>
		
	</body>
</html>
