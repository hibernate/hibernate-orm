<#import "/doc/common.ftl" as common>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
	<head>
		<title>Hibernate Mappings - Entity Summary</title>
		<link rel="stylesheet" type="text/css" href="${docFileManager.getRef(docFile, docFileManager.getCssStylesDocFile())}" title="Style"/>
	</head>
	<body>

		<@common.header/>

		<h2>Package ${package}</h2>

		<#if (classList.size() > 0)>
			<table>
				<thead>
					<tr>
						<th class="MainTableHeading" colspan="2">
							Entities Summary
						</th>
					</tr>
				</thead>
				<tbody>
					<#foreach class in classList>
						<tr>
							<td style="width: 15%">
								<a href="${docFileManager.getRef(docFile, docFileManager.getEntityDocFile(class))}" target="generalFrame">
									${class.declarationName}
								</a>
							</td>
							<td>
								${class.getMetaAsString("class-description")?default("&nbsp;")}
							</td>
						</tr>
					</#foreach>
				</tbody>
			</table>
		</#if>
		
	</body>
</html>
