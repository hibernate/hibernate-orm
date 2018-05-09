<#import "/doc/common.ftl" as common>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
	<head>
		<title>Hibernate Mappings - Table Summary</title>
		<link rel="stylesheet" type="text/css" href="${docFileManager.getRef(docFile, docFileManager.getCssStylesDocFile())}" title="Style"/>
	</head>
	<body>

		<@common.header selected="tables"/>

		<h1>Hibernate Mapping Documentation</h1>

		<#if graphsGenerated>
			<p>
				<img src="tablegraph.png" alt="Table Graph" usemap="#tablegraph"/>
				<map name="tablegraph">
					${tablegrapharea}
				</map>
			</p>
		</#if>

		<table>
			<thead>
				<tr>
					<th class="MainTableHeading">
						Schemas
					</th>
				</tr>
			</thead>
			<tbody>
				<#foreach schema in dochelper.tablesBySchema.keySet()>
					<tr>
						<td>
							<a href="${docFileManager.getRef(docFile, docFileManager.getSchemaSummaryDocFile(schema))}" target="generalFrame">
								${schema}
							</a>
						</td>
					</tr>
				</#foreach>
			</tbody>
		</table>
		
	</body>
</html>
