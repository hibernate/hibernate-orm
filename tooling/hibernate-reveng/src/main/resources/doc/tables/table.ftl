<#import "/doc/common.ftl" as common>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
	<head>
		<title>Hibernate Mappings - Table Info</title>
		<link rel="stylesheet" type="text/css" href="${docFileManager.getRef(docFile, docFileManager.getCssStylesDocFile())}" title="Style"/>
	</head>
	<body>

		<@common.header/>

		<h4>Schema ${dochelper.getQualifiedSchemaName(table)}</h4>
		<h2>Table ${table.name}</h2>

		<#if table.comment?exists>
			<p>${table.comment}</p>
		</#if>

		<table id="column_summary">
			<thead>
				<tr>
					<th class="MainTableHeading" colspan="7">
						Column Summary
					</th>
				</tr>
				<tr>
					<th style="width: 14%">
						Name
					</th>
					<th style="width: 14%">
						SqlType
					</th>
					<th style="width: 14%">
						Length
					</th>
					<th style="width: 14%">
						Precision
					</th>
					<th style="width: 14%">
						Scale
					</th>
					<th style="width: 14%">
						Nullable
					</th>
					<th style="width: 14%">
						Unique
					</th>
				</tr>
			</thead>
			<tbody>
				<#list table.columns as column>
					<tr>
						<td>
							<a href="#column_detail_${column.name}">
								${column.name}
							</a>
						</td>
						<td>
							${dochelper.getSQLTypeName(column)}
						</td>
						<td>
							${dochelper.getLength(column)}
						</td>
						<td>
							${dochelper.getPrecision(column)}
						</td>
						<td>
							${dochelper.getScale(column)}
						</td>
						<td>
							${column.nullable?string}
						</td>
						<td>
							${column.unique?string}
						</td>
					</tr>
				</#list>
			</tbody>
		</table>

		<#-- SHOW THE PRIMARY KEY -->
		<table id="primary_key">
			<thead>
				<tr>
					<th class="MainTableHeading" colspan="2">
						Primary Key
					</th>
				</tr>
				<tr>
					<th style="width: 50%">
						Name
					</th>
					<th style="width: 50%">
						Columns
					</th>
				</tr>
			</thead>
			<tbody>
				<#if table.hasPrimaryKey()>
					<tr>
						<td>
							<#if table.primaryKey.name?has_content>
								${table.primaryKey.name}
							<#else>
								Name not specified
							</#if>
						</td>
						<td>
							<#list dochelper.getPrimaryKeyColumnIterator(table) as column>
								<a href="#column_detail_${column.name}">
									${column.name}
								</a>
							</#list>
						</td>
					</tr>
				<#else>
					<tr>
						<td>
							No Primary Key
						</td>
					</tr>
				</#if>
			</tbody>
		</table>

		<#-- SHOW THE FOREIGN KEYS -->
		<#if !table.foreignKeys.isEmpty()>
			<table id="foreign_keys">
				<thead>
					<tr>
						<th class="MainTableHeading" colspan="3">
							Foreign Keys
						</th>
					</tr>
					<tr>
						<th style="width: 33%">
							Name
						</th>
						<th style="width: 33%">
							Referenced Table
						</th>
						<th style="width: 33%">
							Columns
						</th>
					</tr>
				</thead>
				<tbody>
					<#list table.foreignKeys.values() as foreignKey>
						<tr>
							<td>
								${foreignKey.name?default("Name not specified")}
							</td>
							<td>
								<a href="${docFileManager.getRef(docFile, docFileManager.getTableDocFile(foreignKey.referencedTable))}" target="generalFrame">
									${foreignKey.referencedTable.name}
								</a>
							</td>
							<td>
								<#list foreignKey.columns as column>
									<a href="#column_detail_${column.name}">
										${column.name}
									</a>
								</#list>
							</td>
						</tr>
					</#list>
				</tbody>
			</table>
		</#if>
			
		<#-- SHOW THE UNIQUE KEYS -->
		<#if !table.uniqueKeys.isEmpty()>
			<table id="unique_keys">
				<thead>
					<tr>
						<th class="MainTableHeading" colspan="2">
							Unique Keys
						</th>
					</tr>
					<tr>
						<th style="width: 50%">
							Name
						</th>
						<th style="width: 50%">
							Columns
						</th>
					</tr>
				</thead>
				<tbody>
					<#list table.uniqueKeys.values() as uniqueKey>
						<tr>
							<td>
								${uniqueKey.name?default("Name not specified")}
							</td>
							<td>
								<#list uniqueKey.columns as column>
									<a href="#column_detail_${column.name}">
										${column.name}
									</a>
								</#list>
							</td>
						</tr>
					</#list>
				</tbody>
			</table>
		</#if>
		
		<#-- SHOW THE TABLE INDEXES -->
		<#if !table.indexes.isEmpty()>
			<table id="indexes">
				<thead>
					<tr>
						<th class="MainTableHeading" colspan="2">
							Indexes
						</th>
					</tr>
					<tr>
						<th style="width: 50%">
							Name
						</th>
						<th style="width: 50%">
							Columns
						</th>
					</tr>
				</thead>
				<tbody>
					<#list table.indexes.values() as index>
						<tr>
							<td>
								${index.name?default("Name not specificed")}
							</td>
							<td>
								<#list index.columns as column>
									<a href="#column_detail_${column.name}">
										${column.name}
									</a>
								</#list>
							</td>
						</tr>
					</#list>
				</tbody>
			</table>
		</#if>
		
		<#if !table.columns.isEmpty()>
			<p id="column_detail" class="MainTableHeading">
				Column Detail
			</p>
				
			<#list table.columns as column>
				<h3 id="column_detail_${column.name}">${column.name}</h3>
				
				<ul>
					<li>
						<strong>Type:</strong> ${dochelper.getSQLTypeName(column)}
					</li>
					<li>
						<strong>Length:</strong> ${dochelper.getLength(column)}
					</li>
					<li>
						<strong>Precision:</strong> ${dochelper.getPrecision(column)}
					</li>
					<li>
						<strong>Scale:</strong> ${dochelper.getScale(column)}
					</li>
					<li>
						<strong>Nullable:</strong> ${column.nullable?string}
					</li>
					<li>
						<strong>Unique:</strong> ${column.unique?string}
					</li>
					<li>
						<strong>Comment:</strong> ${column.comment?if_exists}
					</li>
				</ul>

				<#--
					<H4>Mappings:</H4>

					<#list dochelper.getProperties(table, column) as property>
						property.persistentClass.className - property.name
					</#list>
				-->

				<hr/>
			</#list>
				
		</#if>

	</body>
</html>
