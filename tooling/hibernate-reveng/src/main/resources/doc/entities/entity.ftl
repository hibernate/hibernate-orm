<#import "/doc/common.ftl" as common>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
	<head>
		<title>Hibernate Mappings - Entity Info</title>
		<link rel="stylesheet" type="text/css" href="${docFileManager.getRef(docFile, docFileManager.getCssStylesDocFile())}" title="Style"/>
	</head>
	<body>

		<@common.header/>

		<h4>
			<#if class.packageName?length gt 0>
				${class.packageName}
			<#else>
				Default package
			</#if>
		</h4>
		<h2>Entity ${class.getShortName()}</h2>
		
		<ul class="EntityHierarchy">
			<li>
			    <#assign superClasses=dochelper.getInheritanceHierarchy(class)>
				<#list superClasses?reverse as superClass>
					<#-- whitespace is significant here -->
					<#if superClass_index gt 0><img src="${docFileManager.getRef(docFile, docFileManager.getExtendsImageDocFile())}" alt="extended by"/></#if><a href="${docFileManager.getRef(docFile, docFileManager.getEntityDocFileByDeclarationName(superClass))}">${superClass.qualifiedDeclarationName}</a>
					<ul <#if superClass_index == 0>class="first"</#if>>
						<li>
				</#list>
				<#-- whitespace is significant here -->
				<#if class.subclass><img src="${docFileManager.getRef(docFile, docFileManager.getExtendsImageDocFile())}" alt="extended by"/></#if>${class.qualifiedDeclarationName}
				<#list superClasses as superClass>
						</li>
					</ul>
				</#list>
			</li>
		</ul>

		<hr/>

		<#if class.getMetaAsString("class-description")?has_content>
			<p>
				${class.getMetaAsString("class-description")}
			</p>
			<hr/>
		</#if>
		
		<#if class.hasIdentifierProperty()>
			<#assign propertyIdentifier = class.getIdentifierProperty()>
			
			<table id="identifier_summary">
				<thead>
					<tr>
						<th class="MainTableHeading" colspan="4">
							Identifier Summary
						</th>
					</tr>
					<tr>
						<th style="width: 14%">
							Name
						</th>
						<th style="width: 14%">
							Column
						</th>
						<th style="width: 14%">
							Type
						</th>
						<th style="width: 58%">
							Description
						</th>
					</tr>
				</thead>

				<tbody>
					<#if dochelper.getComponentPOJO(propertyIdentifier)?exists>
						<#assign compoclass = dochelper.getComponentPOJO(propertyIdentifier)>
						<#foreach property in compoclass.allPropertiesIterator>
							<#assign columnIterator = property.getValue().columnIterator>
							<#assign rowspan = property.getValue().getColumnSpan()>
							<tr>
								<td <#if (rowspan>0)>rowspan="${rowspan}"</#if>>
									<a href="#identifier_detail_${property.name}">
										${property.name}
									</a>
								</td>

								<#if (rowspan > 0)>
									<#assign column = columnIterator.next()>
									<#if column.isFormula()>
										<td>
											&nbsp;
										</td>
									<#else>
										<td>
											<a href="#property_summary">
												${column.getName()}
											</a>
										</td>
									</#if>			
								<#else>
									<td>
										&nbsp;
									</td>
								</#if>
								
								<td <#if (rowspan > 0)>rowspan="${rowspan}"</#if>>
									<#if dochelper.getComponentPOJO(property)?exists>
										<a href="${docFileManager.getRef(docFile, docFileManager.getEntityDocFileByDeclarationName(dochelper.getComponentPOJO(property)))}" target="generalFrame">
											${compoclass.getJavaTypeName(property, jdk5)?html?default("&nbsp;")}
										</a>
									<#else>
										${compoclass.getJavaTypeName(property, jdk5)?html?default("&nbsp;")}
									</#if>
								</td>

								<td <#if (rowspan > 0)>rowspan="${rowspan}"</#if>>
									<#if compoclass.hasFieldJavaDoc(property)?exists>
										${compoclass.getFieldDescription(property)?default("&nbsp;")}
									<#else>
										&nbsp;
									</#if>
								</td>
							</tr>
							
							<#if (rowspan > 1)>
								<#foreach column in columnIterator>
									<tr>
										<td>
											<a href="#property_summary">
												${column.name}
											</a>
										</td>
									</tr>
								</#foreach>
							</#if>
						</#foreach>	
					<#else>
						<tr>
							<td>
								<a href="#identifier_detail_${propertyIdentifier.name}">
									${propertyIdentifier.name}
								</a>
							</td>
							<td>
								Column
							</td>
							<td>
								${class.getJavaTypeName(propertyIdentifier, jdk5)?html?default("&nbsp;")}
							</td>
							<td>
								<#if class.hasFieldJavaDoc(propertyIdentifier)>
									${class.getFieldDescription(propertyIdentifier)?default("&nbsp;")}
								<#else>
									&nbsp;
								</#if>
							</td>			
						</tr>
					</#if>
				</tbody>
			</table>
		<#else>
			<#list superClasses as superClass>
				<#if superClass.hasIdentifierProperty()>
					<#assign identifier = superClass.identifierProperty>
					<#assign superClassRef = docFileManager.getRef(docFile, docFileManager.getEntityDocFileByDeclarationName(superClass))>
					<p id="identifier_summary" class="MainTableHeading">
						Identifier Summary
					</p>
					<table id="identifier_inherited_from_entity_${superClass.shortName}">
						<tr>
							<th>
								Identifier inherited from entity <a href="${superClassRef}">${superClass.shortName}</a>
							</th>
						</tr>
						<tr>
							<td>
								<a href="${superClassRef}#identifier_detail_${identifier.name}">${identifier.name}</a>
							</td>
						</tr>
					</table>
				</#if>
			</#list>
		</#if>
		
		<#if class.hasVersionProperty()>
			<#assign version = class.versionProperty>
			
			<table id="version_summary">
				<thead>
					<tr>
						<th class="MainTableHeading" colspan="4">
							Version Summary
						</th>
					</tr>
					<tr>
						<th style="width: 14%">
							Name
						</th>
						<th style="width: 14%">
							Column
						</th>
						<th style="width: 14%">
							Type
						</th>
						<th style="width: 58%">
							Description
						</th>
					</tr>
				</thead>

				<tbody>
					<tr>
						<td>
							<a href="#version_detail_${version.name}">
								${version.name}
							</a>
						</td>
						<td>
							Column
						</td>
						<td>
							${class.getJavaTypeName(version, jdk5)?html?default("&nbsp;")}
						</td>
						<td>
							<#if class.hasFieldJavaDoc(version)>
								${class.getFieldDescription(version)?default("&nbsp;")}
							<#else>
								&nbsp;
							</#if>
						</td>			
					</tr>
				</tbody>
			</table>
		<#else>
			<#list superClasses as superClass>
				<#if superClass.hasVersionProperty()>
					<#assign version = superClass.versionProperty>
					<#assign superClassRef = docFileManager.getRef(docFile, docFileManager.getEntityDocFileByDeclarationName(superClass))>
					<p id="version_summary" class="MainTableHeading">
						Version Summary
					</p>
					<table id="version_inherited_from_entity_${superClass.shortName}">
						<tr>
							<th>
								Version inherited from entity <a href="${superClassRef}">${superClass.shortName}</a>
							</th>
						</tr>
						<tr>
							<td>
								<a href="${superClassRef}#version_detail_${version.name}">${version.name}</a>
							</td>
						</tr>
					</table>
				</#if>
			</#list>
		</#if>

		<#assign properties = dochelper.getOrderedSimpleProperties(class)>
		<#if !properties.empty>
			<table id="property_summary">
				<thead>
					<tr>
						<th class="MainTableHeading" colspan="5">
							Property Summary
						</th>
					</tr>
					<tr>
						<th style="width: 14%">
							Name
						</th>
						<th style="width: 14%">
							Column
						</th>
						<th style="width: 14%">
							Access
						</th>
						<th style="width: 14%">
							Type
						</th>
						<th style="width: 44%">
							Description
						</th>
					</tr>
				</thead>
				<tbody>
	
					<#foreach property in properties>
						<#assign columnIterator = property.getValue().columnIterator>
						<#assign rowspan = property.getValue().getColumnSpan()>
						<tr>
							<td <#if (rowspan > 0)>rowspan="${rowspan}"</#if>>
								<a href="#property_detail_${property.name}">
									${property.name}
								</a>
							</td>
	
							<#if (rowspan > 0)>
								<#assign column = columnIterator.next()>
								<#if column.isFormula()>
									<td>
										&nbsp;
									</td>
								<#else>
									<td>
										<a href="#property_summary">
											${column.getName()}
										</a>
									</td>
								</#if>
							<#else>
								<td>
									&nbsp;
								</td>
							</#if>
	
							<td <#if (rowspan > 0)>rowspan="${rowspan}"</#if>>
								${property.getPropertyAccessorName()} (<a href="#property_summary">get</a> / <a href="#property_summary">set</a>)
							</td>
	
							<td <#if (rowspan > 0)>rowspan="${rowspan}"</#if>>
								<#if dochelper.getComponentPOJO(property)?exists>
									<a href="${docFileManager.getRef(docFile, docFileManager.getEntityDocFileByDeclarationName(dochelper.getComponentPOJO(property)))}" target="generalFrame">
										${class.getJavaTypeName(property, jdk5)?html?default("&nbsp;")}
									</a>
								<#else>
									${class.getJavaTypeName(property, jdk5)?html?default("&nbsp;")}
								</#if>
							</td>
	
							<td <#if (rowspan > 0)>rowspan="${rowspan}"</#if>>
								<#if class.hasFieldJavaDoc(property)>
									${class.getFieldDescription(property)?default("&nbsp;")}
								<#else>
									&nbsp;
								</#if>
							</td>
						</tr>
						<#if (rowspan > 1)>
							<#foreach column in columnIterator>
								<tr>
									<td>
										<a href="#property_summary">
											${column.name}
										</a>
									</td>
								</tr>
							</#foreach>
						</#if>
					</#foreach>
	
				</tbody>
			</table>
		</#if>

		<#assign propertyHeader = properties.empty>
		<#list superClasses as superClass>
			<#assign superProperties = dochelper.getOrderedSimpleProperties(superClass)>
			<#if !superProperties.empty>
				<#assign superClassRef = docFileManager.getRef(docFile, docFileManager.getEntityDocFileByDeclarationName(superClass))>
				<#if propertyHeader>
					<#assign propertyHeader = false>
					<p id="properties_summary" class="MainTableHeading">
						Property Summary
					</p>
				</#if>
				<table id="properties_inherited_from_entity_${superClass.shortName}">
					<tr>
						<th>
							Properties inherited from entity <a href="${superClassRef}">${superClass.shortName}</a>
						</th>
					</tr>
					<tr>
						<td>
							<#list superProperties as property>
								<a href="${superClassRef}#property_detail_${property.name}">${property.name}</a><#if property_has_next>, </#if>
							</#list>
						</td>
					</tr>
				</table>
			</#if>
		</#list>

		<#if class.hasIdentifierProperty()>
			<#assign identifier = class.identifierProperty>
			<p id="identifier_detail" class="MainTableHeading">
				Identifier Detail
			</p>
			<#if dochelper.getComponentPOJO(identifier)?exists>
				<#assign identifierClass = dochelper.getComponentPOJO(identifier)>
				<#foreach property in identifierClass.allPropertiesIterator>
					<h3 id="identifier_detail_${property.name}">${property.name}</h3>
				</#foreach>
			<#else>
				<h3 id="identifier_detail_${identifier.name}">${identifier.name}</h3>
			</#if>
		</#if>

		<#if class.hasVersionProperty()>
			<#assign version = class.versionProperty>
			<p id="version_detail" class="MainTableHeading">
				Version Detail
			</p>
			<#assign version = class.versionProperty>
			<h3 id="version_detail_${version.name}">${version.name}</h3>
		</#if>

		<#assign properties = dochelper.getSimpleProperties(class)>
		<#if !properties.empty>
			<p id="property_detail" class="MainTableHeading">
				Property Detail
			</p>
			<#list properties as property>
				<h3 id="property_detail_${property.name}">${property.name}</h3>
				<hr/>
			</#list>
		</#if>

	</body>
</html>
