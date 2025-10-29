/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.reveng.internal.core.strategy;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.hibernate.MappingException;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.core.AssociationInfo;
import org.hibernate.tool.reveng.api.core.RevengStrategy.SchemaSelection;
import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.hibernate.tool.reveng.internal.core.strategy.MetaAttributeHelper.SimpleMetaAttribute;
import org.hibernate.tool.reveng.internal.core.util.RevengUtils;
import org.hibernate.tool.reveng.internal.util.JdbcToHibernateTypeHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class OverrideBinder {

	public static void bindRoot(OverrideRepository repository, Document doc) {		
		Element rootElement = doc.getDocumentElement();	
		bindSchemaSelections(getChildElements(rootElement, "schema-selection"), repository);		
		bindTypeMappings(getChildElements(rootElement, "type-mapping"), repository);
		bindTableFilters(getChildElements(rootElement, "table-filter"), repository);
		bindTables(getChildElements(rootElement, "table"), repository);
	}
	
	private static void bindSchemaSelections(
			ArrayList<Element> schemaSelections, 
			OverrideRepository repository) {	
		for (Element schemaSelection : schemaSelections) {
			bindSchemaSelection(schemaSelection, repository);
		}
	}
	
	private static void bindTypeMappings(
			ArrayList<Element> typeMappings, 
			OverrideRepository repository) {	
		if ( !typeMappings.isEmpty() ) {
			bindTypeMapping(typeMappings.get(0), repository);
		}
	}
	
	private static void bindTableFilters(
			ArrayList<Element> tableFilters, 
			OverrideRepository repository) {		
		for (Element element : tableFilters) {
			TableFilter tableFilter = new TableFilter();
			tableFilter.setMatchCatalog(getAttribute(element, "match-catalog"));
			tableFilter.setMatchSchema(getAttribute(element, "match-schema"));
			tableFilter.setMatchName(getAttribute(element, "match-name"));
			tableFilter.setExclude(Boolean.valueOf(getAttribute(element, "exclude")));
			tableFilter.setPackage(getAttribute(element, "package"));
			MultiValuedMap<String, SimpleMetaAttribute> map = 
					MetaAttributeHelper.loadAndMergeMetaMap(
							element, 
							new HashSetValuedHashMap<String, MetaAttributeHelper.SimpleMetaAttribute>());
			if ( !map.isEmpty() ) {
				tableFilter.setMetaAttributes(map);
			} else {
				tableFilter.setMetaAttributes(null);
			}
			repository.addTableFilter(tableFilter);
		}
	}
		
	private static void bindTables(
			ArrayList<Element> tables, 
			OverrideRepository repository) {	
		for (Element element : tables) {
			Table table = new Table("Hibernate Tools");
			table.setCatalog(getAttribute(element, "catalog"));
			table.setSchema(getAttribute(element, "schema"));
			table.setName(getAttribute(element, "name"));
			ArrayList<Element> primaryKeys = getChildElements(element, "primary-key");
			if ( !primaryKeys.isEmpty() ) {
				bindPrimaryKey(primaryKeys.get(0), table, repository);
			}
			bindColumns(getChildElements(element, "column"), table, repository);
			bindForeignKeys(getChildElements(element, "foreign-key"), table, repository);
			bindMetaAttributes(element, table, repository);
			repository.addTable(table, getAttribute(element, "class"));
		}	
	}
	
	private static void bindPrimaryKey(
			Element element, 
			Table table, 
			OverrideRepository repository) {
		String propertyName = getAttribute(element, "property");
		String compositeIdName = getAttribute(element, "id-class");
		ArrayList<Element> generators = getChildElements(element, "generator");
		if ( !generators.isEmpty() ) {
			Element generator = generators.get(0);
			String identifierClass = getAttribute(generator, "class");
			Properties params = new Properties();
			ArrayList<Element> parameterList = getChildElements(generator, "param");
			for ( Element parameter : parameterList ) {
				params.setProperty( getAttribute( parameter, "name" ), parameter.getTextContent() );
			}
			repository.addTableIdentifierStrategy(table, identifierClass, params);
		}
		List<String> boundColumnNames = bindColumns(getChildElements(element, "key-column"), table, repository);
		repository.addPrimaryKeyNamesForTable(table, boundColumnNames, propertyName, compositeIdName);
	}
	
	private static List<String> bindColumns(
			ArrayList<Element> columns, 
			Table table, 
			OverrideRepository repository) {
		List<String> columnNames = new ArrayList<String>();
		for (Element element : columns) {
			Column column = new Column();
			column.setName(getAttribute(element, "name"));
			String attributeValue = getAttribute(element, "jdbc-type");
			if (StringHelper.isNotEmpty(attributeValue)) {
				column.setSqlTypeCode( JdbcToHibernateTypeHelper.getJDBCType( attributeValue ) );
			}
			TableIdentifier tableIdentifier = TableIdentifier.create(table);
			if (table.getColumn(column) != null) {
				throw new MappingException("Column " + column.getName() + " already exists in table " + tableIdentifier );
			}
			MultiValuedMap<String, SimpleMetaAttribute> map = 
					MetaAttributeHelper.loadAndMergeMetaMap(
							element, 
							new HashSetValuedHashMap<String, MetaAttributeHelper.SimpleMetaAttribute>());
			if( !map.isEmpty() ) {
				repository.addMetaAttributeInfo( tableIdentifier, column.getName(), map);
			} 
			table.addColumn(column);
			columnNames.add(column.getName());
			repository.setTypeNameForColumn(
					tableIdentifier, 
					column.getName(), 
					getAttribute(element, "type"));
			repository.setPropertyNameForColumn(
					tableIdentifier, 
					column.getName(), 
					getAttribute(element, "property"));
			boolean excluded = Boolean.parseBoolean(element.getAttribute("exclude") );
			if(excluded) {
				repository.setExcludedColumn(tableIdentifier, column.getName());
			}
			if (element.hasAttribute("foreign-table")) {
				String foreignTableName = element.getAttribute("foreign-table");
				List<Column> localColumns = new ArrayList<Column>();
				localColumns.add(column);
				List<Column> foreignColumns = new ArrayList<Column>();				
				Table foreignTable = new Table("Hibernate Tools");
				foreignTable.setName(foreignTableName);
				foreignTable.setCatalog(
						element.hasAttribute("foreign-catalog") ?
						element.getAttribute("foreign-catalog") :
						table.getCatalog());
				foreignTable.setSchema(
						element.hasAttribute("foreign-schema") ?
						element.getAttribute("foreign-schema") : 
						table.getSchema());				
				if (element.hasAttribute("foreign-column")) {
					String foreignColumnName = element.getAttribute("foreign-column");
					Column foreignColumn = new Column();
					foreignColumn.setName(foreignColumnName);
					foreignColumns.add(foreignColumn);
				} else {
					throw new MappingException("foreign-column is required when foreign-table is specified on " + column);
				}
				ForeignKey key = table.createForeignKey(
						null, 
						localColumns, 
						foreignTableName, 
						null, 
						null,
						foreignColumns);
				key.setReferencedTable(foreignTable); // only possible if foreignColumns is explicitly specified (workaround on aligncolumns)
			}
		}
		return columnNames;
	}
	
	private static void bindForeignKeys(
			ArrayList<Element> foreignKeys, 
			Table table, 
			OverrideRepository repository) {
		for (Element element : foreignKeys) {
			String constraintName = getAttribute(element, "constraint-name");
			String foreignTableName = getAttribute(element, "foreign-table");
			if (foreignTableName != null) {
				Table foreignTable = new Table("hibernate tools");
				foreignTable.setName(foreignTableName);
				foreignTable.setCatalog(
						element.hasAttribute("foreign-catalog") ? 
						element.getAttribute("foreign-catalog") : 
						table.getCatalog());
				foreignTable.setSchema(
						element.hasAttribute("foreign-schema") ? 
						element.getAttribute("foreign-schema") : 
						table.getSchema());
				List<Column> localColumns = new ArrayList<Column>();
				List<Column> foreignColumns = new ArrayList<Column>();
				ArrayList<Element> columnRefs = getChildElements(element, "column-ref");
				for (Element columnRef : columnRefs) {
					localColumns.add(new Column(columnRef.getAttribute("local-column")));
					foreignColumns.add(new Column(columnRef.getAttribute("foreign-column")));
				}
				ForeignKey key = table.createForeignKey(
						constraintName, 
						localColumns, 
						foreignTableName, 
						null, 
						null,
						foreignColumns);
				key.setReferencedTable(foreignTable); // only possible if foreignColumns is explicitly specified (workaround on aligncolumns)				
			}
			if (StringHelper.isNotEmpty(constraintName)) {
				if (!validateFkAssociations(element)) {
					throw new IllegalArgumentException("you can't mix <many-to-one/> or <set/> with <(inverse-)one-to-one/> ");
				}
				if (!bindManyToOneAndCollection(element, constraintName, repository)) {
					bindOneToOne(element, constraintName, repository);
				}
			}
			
		}
	}
	
	private static void bindOneToOne(Element element, String constraintName,
			OverrideRepository repository) {
		String oneToOneProperty = null;
		Boolean excludeOneToOne = null;
		ArrayList<Element> oneToOnes = getChildElements(element, "one-to-one");
		Element oneToOne = null;
		AssociationInfo associationInfo = null;
		if( !oneToOnes.isEmpty() ) {
			oneToOne = oneToOnes.get(0);
			oneToOneProperty = getAttribute(oneToOne, "property");
			excludeOneToOne = Boolean.valueOf(oneToOne.getAttribute("exclude"));
			associationInfo = extractAssociationInfo(oneToOne);										
		}
		
		String inverseOneToOneProperty = null;
		Boolean excludeInverseOneToOne = null;
		ArrayList<Element> inverseOneToOnes = getChildElements(element, "inverse-one-to-one");
		Element inverseOneToOne = null;
		AssociationInfo inverseAssociationInfo = null;
		if( !inverseOneToOnes.isEmpty() ) {
			inverseOneToOne = inverseOneToOnes.get(0);
			inverseOneToOneProperty = getAttribute(inverseOneToOne, "property");
			excludeInverseOneToOne = Boolean.valueOf(inverseOneToOne.getAttribute("exclude"));
			inverseAssociationInfo = extractAssociationInfo(inverseOneToOne);
		}		
		// having oneToOne = null and inverseOneToOne != null doesn't make sense
		// we cannot have the inverse side without the owning side in this case		
		if ( (oneToOne!=null) ) {
			repository.addForeignKeyInfo(
					constraintName, 
					oneToOneProperty, 
					excludeOneToOne, 
					inverseOneToOneProperty, 
					excludeInverseOneToOne, 
					associationInfo, 
					inverseAssociationInfo);
		}
	}

	private static boolean bindManyToOneAndCollection(
			Element element, 
			String constraintName, 
			OverrideRepository repository) {
		String manyToOneProperty = null;
		Boolean excludeManyToOne = null;
		AssociationInfo associationInfo = null;
		AssociationInfo inverseAssociationInfo = null;
		ArrayList<Element> manyToOnes = getChildElements(element, "many-to-one");
		Element manyToOne = null;
		if ( !manyToOnes.isEmpty() ) {
			manyToOne = manyToOnes.get(0);
			manyToOneProperty = getAttribute(manyToOne, "property");
			excludeManyToOne = Boolean.valueOf(manyToOne.getAttribute("exclude"));
			associationInfo = extractAssociationInfo(manyToOne);
		}
		String collectionProperty = null;
		Boolean excludeCollection = null;
		ArrayList<Element> sets = getChildElements(element, "set");
		Element set = null;
		if ( !sets.isEmpty() ) {
			set = sets.get(0);
			collectionProperty = getAttribute(set, "property");
			excludeCollection = Boolean.valueOf(set.getAttribute("exclude"));
			inverseAssociationInfo = extractAssociationInfo(set);
		}
		if ( (manyToOne!=null) || (set!=null) ) {
			repository.addForeignKeyInfo(
					constraintName, 
					manyToOneProperty, 
					excludeManyToOne, 
					collectionProperty, 
					excludeCollection, 
					associationInfo, 
					inverseAssociationInfo);
			return true;
		} else {
			return false;
		}
	}
	
	private static AssociationInfo extractAssociationInfo(Element manyToOne) {
		return RevengUtils.createAssociationInfo(
				manyToOne.hasAttribute("cascade") ? manyToOne.getAttribute("cascade") : null, 
				manyToOne.hasAttribute("fetch") ? manyToOne.getAttribute("fetch") : null, 
				manyToOne.hasAttribute("insert") ? 
					Boolean.parseBoolean(manyToOne.getAttribute("insert")) : null, 
				manyToOne.hasAttribute("update") ? 
					Boolean.parseBoolean(manyToOne.getAttribute("update")) : null);
	}

	private static boolean validateFkAssociations(Element element){
		ArrayList<Element> manyToOnes = getChildElements(element, "many-to-one");
		ArrayList<Element> oneToOnes = getChildElements(element, "one-to-one");
		ArrayList<Element> sets = getChildElements(element, "set");
		ArrayList<Element> inverseOneToOnes = getChildElements(element, "inverse-one-to-one");
		if ( !manyToOnes.isEmpty() &&
			 (!oneToOnes.isEmpty() || !inverseOneToOnes.isEmpty())) {
			return false;
		}
		if ( !oneToOnes.isEmpty() && !sets.isEmpty() ) {
			return false;
		}
		return inverseOneToOnes.isEmpty() || sets.isEmpty();
	}
	
	private static void bindMetaAttributes(
			Element element, 
			Table table, 
			OverrideRepository repository) {
		MultiValuedMap<String, SimpleMetaAttribute> map = 
				MetaAttributeHelper.loadAndMergeMetaMap( 
						element, 
						new HashSetValuedHashMap<String, MetaAttributeHelper.SimpleMetaAttribute>());
		if( !map.isEmpty() ) {
			repository.addMetaAttributeInfo( table, map);
		} 
	}
		
	private static void bindSchemaSelection(
			Element schemaSelectionElement, 
			OverrideRepository repository) {
		repository.addSchemaSelection(
				new SchemaSelection() {
					@Override
					public String getMatchCatalog() {
						return getAttribute(schemaSelectionElement, "match-catalog");
					}
					@Override
					public String getMatchSchema() {
						return getAttribute(schemaSelectionElement, "match-schema");
					}
					@Override
					public String getMatchTable() {
						return getAttribute(schemaSelectionElement, "match-table");
					}
					
				});
	}
	
	private static void bindTypeMapping(
			Element typeMapping, 
			OverrideRepository repository) {	
		ArrayList<Element> sqlTypes = getChildElements(typeMapping, "sql-type");
		for ( Element sqlType : sqlTypes ) {
			bindSqlType( sqlType, repository );
		}
	}
	
	private static void bindSqlType(Element sqlType, OverrideRepository repository) {
		int jdbcType = JdbcToHibernateTypeHelper.getJDBCType(
				getAttribute(sqlType, "jdbc-type"));
		SQLTypeMapping sqlTypeMapping = new SQLTypeMapping(jdbcType);
		sqlTypeMapping.setHibernateType(getHibernateType(sqlType));
		sqlTypeMapping.setLength(getInteger(
				getAttribute(sqlType, "length"),
				SQLTypeMapping.UNKNOWN_LENGTH));
		sqlTypeMapping.setPrecision(getInteger(
				getAttribute(sqlType, "precision"),
				SQLTypeMapping.UNKNOWN_PRECISION));
		sqlTypeMapping.setScale(getInteger(
				getAttribute(sqlType, "scale"),
				SQLTypeMapping.UNKNOWN_SCALE));
		String notNull = getAttribute(sqlType, "not-null");
		if (StringHelper.isEmpty(notNull)) {
			sqlTypeMapping.setNullable(null);
		} else {
			sqlTypeMapping.setNullable(notNull.equals("false"));
		}
		if (StringHelper.isEmpty(sqlTypeMapping.getHibernateType())) {
			throw new MappingException(
					"No hibernate-type specified for " + 
					sqlType.getAttribute("jdbc-type") + 
					" at " + 
					sqlType.getTagName()); 
		}
		repository.addTypeMapping(sqlTypeMapping);		
	}
	
	private static String getHibernateType(Element element) {
		String attributeValue = getAttribute(element, "hibernate-type");
		if(StringHelper.isEmpty(attributeValue)) {
			ArrayList<Element> hibernateTypes = getChildElements(element, "hibernate-type");
			if ( !hibernateTypes.isEmpty() ) {
				Element hibernateType = hibernateTypes.get(0);
				if (hibernateType.hasAttribute("name")) {
					return hibernateType.getAttribute("name");
				}
			} else {
				return null;
			}
		}
		return attributeValue;
	}

	private static int getInteger(String string, int defaultValue) {
		if(string==null) {
			return defaultValue;
		} 
		else {
			try {
				return Integer.parseInt(string);
			} catch (NumberFormatException e) {
				throw new RuntimeException(e);
			}
		}		
	}
	
	private static ArrayList<Element> getChildElements(Element parent, String tagName) {
		ArrayList<Element> result = new ArrayList<Element>();
		NodeList nodeList = parent.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node instanceof Element) {
				if (tagName.equals(((Element)node).getTagName())) {
					result.add((Element)node);
				}
			}
		}
		return result;
	}
	
	private static String getAttribute(Element element, String attributeName) {
		String result = null;
		if (element.hasAttribute(attributeName)) {
			result = element.getAttribute(attributeName);
		}
		return result;
	}

}
