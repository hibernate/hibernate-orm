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
package org.hibernate.tool.internal.strategy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiValuedMap;
import org.hibernate.MappingException;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.AssociationInfo;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.RevengStrategy.SchemaSelection;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.strategy.MetaAttributeHelper.SimpleMetaAttribute;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

public class OverrideRepository {

	private static final Logger log = Logger.getLogger(OverrideRepository.class);

	final Map<TypeMappingKey, List<SQLTypeMapping>> typeMappings;
	final List<TableFilter> tableFilters;
	final Map<TableIdentifier, List<ForeignKey>> foreignKeys;
	final Map<TableColumnKey, String> typeForColumn;
	final Map<TableColumnKey, String> propertyNameForColumn;
	final Map<TableIdentifier, String> identifierStrategyForTable;
	final Map<TableIdentifier, Properties> identifierPropertiesForTable;
	final Map<TableIdentifier, List<String>> primaryKeyColumnsForTable;
	final Set<TableColumnKey> excludedColumns;
	final TableToClassName tableToClassName;
	final List<SchemaSelection> schemaSelections;
	final Map<TableIdentifier, String> propertyNameForPrimaryKey;
	final Map<TableIdentifier, String> compositeIdNameForTable;
	final Map<String, String> foreignKeyToOneName;
	final Map<String, String> foreignKeyToInverseName;
	final Map<String, Boolean> foreignKeyInverseExclude;
	final Map<String, Boolean> foreignKeyToOneExclude;
	final Map<String, AssociationInfo> foreignKeyToEntityInfo;
	final Map<String, AssociationInfo> foreignKeyToInverseEntityInfo;
	final Map<TableIdentifier, MultiValuedMap<String, SimpleMetaAttribute>> tableMetaAttributes;
	final Map<TableColumnKey, MultiValuedMap<String, SimpleMetaAttribute>> columnMetaAttributes;

	public OverrideRepository() {
		typeMappings = new HashMap<>();
		tableFilters = new ArrayList<>();
		foreignKeys = new HashMap<>();
		typeForColumn = new HashMap<>();
		propertyNameForColumn = new HashMap<>();
		identifierStrategyForTable = new HashMap<>();
		identifierPropertiesForTable = new HashMap<>();
		primaryKeyColumnsForTable = new HashMap<>();
		propertyNameForPrimaryKey = new HashMap<>();
		tableToClassName = new TableToClassName();
		excludedColumns = new HashSet<>();
		schemaSelections = new ArrayList<>();
		compositeIdNameForTable = new HashMap<>();
		foreignKeyToOneName = new HashMap<>();
		foreignKeyToInverseName = new HashMap<>();
		foreignKeyInverseExclude = new HashMap<>();
		foreignKeyToOneExclude = new HashMap<>();
		tableMetaAttributes = new HashMap<>();
		columnMetaAttributes = new HashMap<>();
		foreignKeyToEntityInfo = new HashMap<>();
		foreignKeyToInverseEntityInfo = new HashMap<>();
	}

	// --- XML loading ---

	public void addFile(File xmlFile) {
		log.info("Override file: " + xmlFile.getPath());
		try {
			addInputStream(xmlFile);
		} catch (Exception e) {
			log.error("Could not configure overrides from file: "
					+ xmlFile.getPath(), e);
			throw new MappingException(
					"Could not configure overrides from file: "
					+ xmlFile.getPath(), e);
		}
	}

	public OverrideRepository addResource(String path) throws MappingException {
		log.info("Mapping resource: " + path);
		InputStream rsrc = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(path);
		if (rsrc == null) {
			rsrc = OverrideRepository.class.getClassLoader()
					.getResourceAsStream(path);
		}
		if (rsrc == null) {
			throw new MappingException("Resource: " + path + " not found");
		}
		try {
			return addInputStream(rsrc);
		} catch (MappingException me) {
			throw new MappingException("Error reading resource: " + path, me);
		}
	}

	public OverrideRepository addInputStream(InputStream xmlInputStream)
			throws MappingException {
		try {
			final List<SAXParseException> errors = new ArrayList<>();
			ErrorHandler errorHandler = new ErrorHandler() {
				@Override
				public void warning(SAXParseException exception) {
					log.warn("warning while parsing xml", exception);
				}
				@Override
				public void error(SAXParseException exception) {
					errors.add(exception);
				}
				@Override
				public void fatalError(SAXParseException exception) {
					error(exception);
				}
			};
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(
					"com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl",
					Thread.currentThread().getContextClassLoader());
			DocumentBuilder db = dbf.newDocumentBuilder();
			db.setErrorHandler(errorHandler);
			Document document = db.parse(xmlInputStream);
			if (!errors.isEmpty()) {
				throw new MappingException(
						"invalid override definition", errors.get(0));
			}
			OverrideBinder.bindRoot(this, document);
			return this;
		} catch (MappingException me) {
			throw me;
		} catch (Exception e) {
			log.error("Could not configure overrides from input stream", e);
			throw new MappingException(e);
		}
	}

	public void addInputStream(File file) throws MappingException {
		try (InputStream xmlInputStream = new FileInputStream(file)) {
			try {
				addInputStream(xmlInputStream);
			} catch (Exception e) {
				log.error("Could not configure overrides from input stream", e);
				throw new MappingException(e);
			}
		} catch (IOException ioe) {
			log.error("could not close input stream", ioe);
		}
	}

	// --- Type mappings ---

	String getPreferredHibernateType(
			int sqlType, int length, int precision, int scale, boolean nullable) {
		List<SQLTypeMapping> l = typeMappings.get(
				new TypeMappingKey(sqlType, length));
		if (l == null) {
			l = typeMappings.get(
					new TypeMappingKey(sqlType, SQLTypeMapping.UNKNOWN_LENGTH));
		}
		return scanForMatch(sqlType, length, precision, scale, nullable, l);
	}

	private String scanForMatch(
			int sqlType, int length, int precision, int scale,
			boolean nullable, List<SQLTypeMapping> l) {
		if (l != null) {
			for (SQLTypeMapping element : l) {
				if (element.getJDBCType() != sqlType) return null;
				if (element.match(sqlType, length, precision, scale, nullable)) {
					return element.getHibernateType();
				}
			}
		}
		return null;
	}

	public void addTypeMapping(SQLTypeMapping sqltype) {
		TypeMappingKey key = new TypeMappingKey(sqltype);
		List<SQLTypeMapping> list = typeMappings.computeIfAbsent(
				key, k -> new ArrayList<>());
		list.add(sqltype);
	}

	// --- Table filters ---

	protected String getPackageName(TableIdentifier identifier) {
		for (TableFilter tf : tableFilters) {
			String value = tf.getPackage(identifier);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	protected boolean excludeTable(TableIdentifier identifier) {
		Iterator<TableFilter> iterator = tableFilters.iterator();
		boolean hasInclude = false;
		while (iterator.hasNext()) {
			TableFilter tf = iterator.next();
			Boolean value = tf.exclude(identifier);
			if (value != null) {
				return value;
			}
			if (!tf.getExclude()) {
				hasInclude = true;
			}
		}
		return hasInclude;
	}

	public void addTableFilter(TableFilter filter) {
		tableFilters.add(filter);
	}

	// --- Strategy creation ---

	public RevengStrategy getReverseEngineeringStrategy(RevengStrategy delegate) {
		return new OverrideStrategyDelegate(this, delegate);
	}

	/**
	 * @deprecated Use {@link #getReverseEngineeringStrategy(RevengStrategy)}
	 *     with {@code delegate=null} to explicitly ignore the delegate.
	 */
	@Deprecated
	public RevengStrategy getReverseEngineeringStrategy() {
		return getReverseEngineeringStrategy(null);
	}

	// --- Meta attributes ---

	protected Map<String, MetaAttribute> columnToMetaAttributes(
			TableIdentifier tableIdentifier, String column) {
		MultiValuedMap<String, SimpleMetaAttribute> specific =
				columnMetaAttributes.get(new TableColumnKey(tableIdentifier, column));
		if (specific != null && !specific.isEmpty()) {
			return toMetaAttributes(specific);
		}
		return null;
	}

	protected Map<String, MetaAttribute> tableToMetaAttributes(
			TableIdentifier identifier) {
		MultiValuedMap<String, SimpleMetaAttribute> specific =
				tableMetaAttributes.get(identifier);
		if (specific != null && !specific.isEmpty()) {
			return toMetaAttributes(specific);
		}
		MultiValuedMap<String, SimpleMetaAttribute> general =
				findGeneralAttributes(identifier);
		if (general != null && !general.isEmpty()) {
			return toMetaAttributes(general);
		}
		return null;
	}

	private MultiValuedMap<String, SimpleMetaAttribute> findGeneralAttributes(
			TableIdentifier identifier) {
		for (TableFilter tf : tableFilters) {
			MultiValuedMap<String, SimpleMetaAttribute> value =
					tf.getMetaAttributes(identifier);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	private Map<String, MetaAttribute> toMetaAttributes(
			MultiValuedMap<String, SimpleMetaAttribute> mvm) {
		Map<String, MetaAttribute> result = new HashMap<>();
		for (MapIterator<String, SimpleMetaAttribute> iter =
				mvm.mapIterator(); iter.hasNext(); ) {
			String key = iter.next();
			Collection<SimpleMetaAttribute> values = mvm.get(key);
			result.put(key, MetaAttributeHelper.toRealMetaAttribute(key, values));
		}
		return result;
	}

	// --- Table / column configuration ---

	public void addTable(Table table, String wantedClassName) {
		for (ForeignKey fk : table.getForeignKeyCollection()) {
			TableIdentifier identifier = TableIdentifier.create(
					fk.getReferencedTable());
			List<ForeignKey> existing = foreignKeys.computeIfAbsent(
					identifier, k -> new ArrayList<>());
			existing.add(fk);
		}
		if (StringHelper.isNotEmpty(wantedClassName)) {
			TableIdentifier tableIdentifier = TableIdentifier.create(table);
			String className = wantedClassName;
			if (!wantedClassName.contains(".")) {
				String packageName = getPackageName(tableIdentifier);
				if (packageName != null && !packageName.isBlank()) {
					className = packageName + "." + wantedClassName;
				}
			}
			tableToClassName.put(tableIdentifier, className);
		}
	}

	public void setTypeNameForColumn(
			TableIdentifier identifier, String columnName, String type) {
		if (StringHelper.isNotEmpty(type)) {
			typeForColumn.put(
					new TableColumnKey(identifier, columnName), type);
		}
	}

	public void setExcludedColumn(
			TableIdentifier tableIdentifier, String columnName) {
		excludedColumns.add(new TableColumnKey(tableIdentifier, columnName));
	}

	public void setPropertyNameForColumn(
			TableIdentifier identifier, String columnName, String property) {
		if (StringHelper.isNotEmpty(property)) {
			propertyNameForColumn.put(
					new TableColumnKey(identifier, columnName), property);
		}
	}

	public void addTableIdentifierStrategy(
			Table table, String identifierClass, Properties params) {
		if (identifierClass != null) {
			final TableIdentifier tid = TableIdentifier.create(table);
			identifierStrategyForTable.put(tid, identifierClass);
			identifierPropertiesForTable.put(tid, params);
		}
	}

	public void addPrimaryKeyNamesForTable(
			Table table, List<String> boundColumnNames,
			String propertyName, String compositeIdName) {
		TableIdentifier tableIdentifier = TableIdentifier.create(table);
		if (boundColumnNames != null && !boundColumnNames.isEmpty()) {
			primaryKeyColumnsForTable.put(tableIdentifier, boundColumnNames);
		}
		if (StringHelper.isNotEmpty(propertyName)) {
			propertyNameForPrimaryKey.put(tableIdentifier, propertyName);
		}
		if (StringHelper.isNotEmpty(compositeIdName)) {
			compositeIdNameForTable.put(tableIdentifier, compositeIdName);
		}
	}

	public void addSchemaSelection(SchemaSelection schemaSelection) {
		schemaSelections.add(schemaSelection);
	}

	public void addForeignKeyInfo(
			String constraintName, String toOneProperty,
			Boolean excludeToOne, String inverseProperty,
			Boolean excludeInverse, AssociationInfo associationInfo,
			AssociationInfo inverseAssociationInfo) {
		if (StringHelper.isNotEmpty(toOneProperty)) {
			foreignKeyToOneName.put(constraintName, toOneProperty);
		}
		if (StringHelper.isNotEmpty(inverseProperty)) {
			foreignKeyToInverseName.put(constraintName, inverseProperty);
		}
		if (excludeInverse != null) {
			foreignKeyInverseExclude.put(constraintName, excludeInverse);
		}
		if (excludeToOne != null) {
			foreignKeyToOneExclude.put(constraintName, excludeToOne);
		}
		if (associationInfo != null) {
			foreignKeyToEntityInfo.put(constraintName, associationInfo);
		}
		if (inverseAssociationInfo != null) {
			foreignKeyToInverseEntityInfo.put(constraintName,
					inverseAssociationInfo);
		}
	}

	public void addMetaAttributeInfo(
			Table table, MultiValuedMap<String, SimpleMetaAttribute> map) {
		if (map != null && !map.isEmpty()) {
			tableMetaAttributes.put(TableIdentifier.create(table), map);
		}
	}

	public void addMetaAttributeInfo(
			TableIdentifier tableIdentifier, String name,
			MultiValuedMap<String, SimpleMetaAttribute> map) {
		if (map != null && !map.isEmpty()) {
			columnMetaAttributes.put(
					new TableColumnKey(tableIdentifier, name), map);
		}
	}
}
