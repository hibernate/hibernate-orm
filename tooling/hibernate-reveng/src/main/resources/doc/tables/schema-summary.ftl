<#import "/doc/common.ftl" as common>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
	<head>
		<title>Hibernate Mappings - Table Summary</title>
		<link rel="stylesheet" type="text/css" href="${docFileManager.getRef(docFile, docFileManager.getCssStylesDocFile())}" title="Style"/>
	</head>
	<body>

		<@common.header/>

		<h2>Schema ${schema}</h2>

		<table>
			<thead>
				<tr>
					<th class="MainTableHeading">
						Tables
					</th>
				</tr>
			</thead>
			<tbody>
				<#list dochelper.tablesBySchema.get(schema) as table>
					<tr>
						<td>
							<a href="${docFileManager.getRef(docFile, docFileManager.getTableDocFile(table))}" target="generalFrame">
								${table.name}
							</a>
						</td>
					</tr>
				</#list>
			</tbody>
		</table>

	</body>
</html>
