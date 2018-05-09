<#import "/doc/common.ftl" as common>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
	<head>
		<title>Hibernate Mappings - Entity Summary</title>
		<link rel="stylesheet" type="text/css" href="${docFileManager.getRef(docFile, docFileManager.getCssStylesDocFile())}" title="Style"/>
	</head>
	<body>
	
		<@common.header selected="entities"/>

		<h1>Hibernate Mapping Documentation</h1>

		<#if graphsGenerated>
			<p>
				<img src="entitygraph.png" alt="Entity Graph" usemap="#entitygraph"/>
				<map name="entitygraph">
					${entitygrapharea}
				</map>
			</p>
		</#if>

		<table>
			<thead>
				<tr>
					<th class="MainTableHeading">
						Packages
					</th>
				</tr>
			</thead>
			<tbody>
				<#foreach package in packageList>
				<tr>
					<td>
						<a href="${docFileManager.getRef(docFile, docFileManager.getPackageSummaryDocFile(package))}" target="generalFrame">${package}</a>
					</td>
				</tr>
				</#foreach>
			</tbody>
		</table>

	</body>
</html>
