<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
	<head>
		<title>Hibernate Mappings - Entity List</title>
		<link rel="stylesheet" type="text/css" href="${docFileManager.getRef(docFile, docFileManager.getCssStylesDocFile())}" title="Style"/>
	</head>
	<body class="List">

		<p class="ListTitleFont">
			<a href="${docFileManager.getRef(docFile, docFileManager.getPackageSummaryDocFile(title))}" target="generalFrame">${title}</a>
		</p>
		
		<#if (classList.size() > 0)>
			<p>
				Entities<br/>
				<#foreach class in classList>
					<a href="${docFileManager.getRef(docFile, docFileManager.getEntityDocFile(class))}" target="generalFrame">${class.declarationName}</a><br/>
				</#foreach>
			</p>
		</#if>

	</body>
</html>
