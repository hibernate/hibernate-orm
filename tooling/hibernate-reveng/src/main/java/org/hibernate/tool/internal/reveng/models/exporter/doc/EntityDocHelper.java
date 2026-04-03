/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.exporter.doc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Table;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.tool.internal.reveng.models.metadata.ForeignKeyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;

/**
 * Provides entity navigation and property helpers for the
 * documentation FreeMarker templates. Replaces the old
 * {@code DocHelper} by working with {@link ClassDetails} via
 * {@link EntityDocInfo} adapters.
 * <p>
 * Templates access this as {@code dochelper} in the FreeMarker context.
 *
 * @author Koen Aers
 */
public class EntityDocHelper {

	public static final String DEFAULT_NO_PACKAGE = "All Entities";
	public static final String DEFAULT_NO_SCHEMA = "default";

	private final Map<String, List<EntityDocInfo>> classesByPackage =
			new HashMap<>();
	private final List<EntityDocInfo> allClasses = new ArrayList<>();
	private final Map<String, EntityDocInfo> entityByQualifiedName =
			new HashMap<>();

	private final Map<String, List<TableDocInfo>> tablesBySchema =
			new LinkedHashMap<>();
	private final List<TableDocInfo> allTables = new ArrayList<>();
	private final Map<String, TableDocInfo> tablesByName = new HashMap<>();

	public EntityDocHelper(List<ClassDetails> entities) {
		this(entities, null);
	}

	public EntityDocHelper(List<ClassDetails> entities,
						   Map<String, TableMetadata> tableMetadataMap) {
		for (ClassDetails entity : entities) {
			EntityDocInfo info = new EntityDocInfo(entity);
			processClass(info);
			entityByQualifiedName.put(
					info.getQualifiedDeclarationName(), info);
		}
		buildTableInfo(entities, tableMetadataMap);
	}

	private void processClass(EntityDocInfo info) {
		allClasses.add(info);
		String packageName = info.getPackageName();
		if (packageName == null || packageName.isEmpty()) {
			packageName = DEFAULT_NO_PACKAGE;
		}
		classesByPackage
				.computeIfAbsent(packageName, k -> new ArrayList<>())
				.add(info);
	}

	public List<String> getPackages() {
		List<String> packages = new ArrayList<>(classesByPackage.keySet());
		Collections.sort(packages);
		return packages;
	}

	public List<EntityDocInfo> getClasses() {
		List<EntityDocInfo> sorted = new ArrayList<>(allClasses);
		sorted.sort(Comparator.comparing(EntityDocInfo::getDeclarationName));
		return sorted;
	}

	public List<EntityDocInfo> getClasses(String packageName) {
		List<EntityDocInfo> list = classesByPackage.get(packageName);
		if (list == null) {
			return Collections.emptyList();
		}
		List<EntityDocInfo> sorted = new ArrayList<>(list);
		sorted.sort(Comparator.comparing(EntityDocInfo::getDeclarationName));
		return sorted;
	}

	public List<EntityDocInfo> getInheritanceHierarchy(EntityDocInfo entity) {
		if (!entity.isSubclass()) {
			return Collections.emptyList();
		}
		List<EntityDocInfo> superClasses = new ArrayList<>();
		EntityDocInfo superClass = entity.getSuperClass();
		while (superClass != null) {
			EntityDocInfo registered = entityByQualifiedName.get(
					superClass.getQualifiedDeclarationName());
			superClasses.add(registered != null ? registered : superClass);
			superClass = superClass.getSuperClass();
		}
		return superClasses;
	}

	/**
	 * Returns an {@link EntityDocInfo} for the component type of an
	 * embedded property, or {@code null} if the property is not embedded.
	 * The component class must have been included in the entity list
	 * passed to the constructor.
	 */
	public EntityDocInfo getComponentPOJO(PropertyDocInfo property) {
		FieldDetails field = property.getFieldDetails();
		if (field.hasDirectAnnotationUsage(EmbeddedId.class)
				|| field.hasDirectAnnotationUsage(Embedded.class)) {
			String typeName =
					field.getType().determineRawClass().getClassName();
			return entityByQualifiedName.get(typeName);
		}
		return null;
	}

	public List<PropertyDocInfo> getSimpleProperties(EntityDocInfo entity) {
		List<PropertyDocInfo> result = new ArrayList<>();
		Iterator<PropertyDocInfo> it = entity.getAllPropertiesIterator();
		PropertyDocInfo id = entity.getIdentifierProperty();
		PropertyDocInfo version = entity.getVersionProperty();
		while (it.hasNext()) {
			PropertyDocInfo prop = it.next();
			if (prop != id && prop != version) {
				result.add(prop);
			}
		}
		return result;
	}

	public List<PropertyDocInfo> getOrderedSimpleProperties(
			EntityDocInfo entity) {
		List<PropertyDocInfo> result = getSimpleProperties(entity);
		result.sort(Comparator.comparing(PropertyDocInfo::getName));
		return result;
	}

	// ---- Table-side methods ----

	private void buildTableInfo(List<ClassDetails> entities,
								Map<String, TableMetadata> tableMetadataMap) {
		// First pass: build TableDocInfo for each entity
		for (ClassDetails entity : entities) {
			String className = entity.getClassName();
			TableDocInfo tableInfo;
			if (tableMetadataMap != null
					&& tableMetadataMap.containsKey(className)) {
				tableInfo = TableDocInfo.buildFromTableMetadata(
						tableMetadataMap.get(className));
			}
			else {
				tableInfo = TableDocInfo.buildFromClassDetails(entity);
			}
			allTables.add(tableInfo);
			tablesByName.put(tableInfo.getName(), tableInfo);

			String schema = tableInfo.getSchema();
			if (schema == null || schema.isEmpty()) {
				schema = DEFAULT_NO_SCHEMA;
			}
			tablesBySchema
					.computeIfAbsent(schema, k -> new ArrayList<>())
					.add(tableInfo);
		}

		// Build entity-class-name → table-name map for FK resolution
		Map<String, String> entityClassToTableName = new HashMap<>();
		if (tableMetadataMap != null) {
			for (TableMetadata tm : tableMetadataMap.values()) {
				String fqn = tm.getEntityPackage() + "."
						+ tm.getEntityClassName();
				entityClassToTableName.put(fqn, tm.getTableName());
			}
		}

		// Second pass: resolve foreign key cross-references
		if (tableMetadataMap != null) {
			for (TableMetadata tableMeta : tableMetadataMap.values()) {
				TableDocInfo tableInfo =
						tablesByName.get(tableMeta.getTableName());
				if (tableInfo == null) {
					continue;
				}
				for (ForeignKeyMetadata fk : tableMeta.getForeignKeys()) {
					String targetFqn = fk.getTargetEntityPackage() + "."
							+ fk.getTargetEntityClassName();
					String targetTableName =
							entityClassToTableName.get(targetFqn);
					if (targetTableName == null) {
						continue;
					}
					TableDocInfo referencedTable =
							tablesByName.get(targetTableName);
					if (referencedTable != null) {
						String fkName = "FK_" + tableInfo.getName() + "_"
								+ fk.getForeignKeyColumnName();
						List<TableColumnDocInfo> fkCols = new ArrayList<>();
						for (TableColumnDocInfo col :
								tableInfo.getColumns()) {
							if (col.getName().equals(
									fk.getForeignKeyColumnName())) {
								fkCols.add(col);
								break;
							}
						}
						tableInfo.addForeignKey(fkName,
								new ForeignKeyDocInfo(fkName,
										referencedTable, fkCols));
					}
				}
			}
		}
	}

	/**
	 * Returns tables grouped by schema. Used by table summary and
	 * schema-summary templates as {@code dochelper.tablesBySchema}.
	 */
	public Map<String, List<TableDocInfo>> getTablesBySchema() {
		return tablesBySchema;
	}

	public List<String> getSchemas() {
		List<String> schemas = new ArrayList<>(tablesBySchema.keySet());
		Collections.sort(schemas);
		return schemas;
	}

	public List<TableDocInfo> getTables() {
		List<TableDocInfo> sorted = new ArrayList<>(allTables);
		sorted.sort(Comparator.comparing(TableDocInfo::getName));
		return sorted;
	}

	public List<TableDocInfo> getTables(String schema) {
		List<TableDocInfo> list = tablesBySchema.get(schema);
		if (list == null) {
			return Collections.emptyList();
		}
		List<TableDocInfo> sorted = new ArrayList<>(list);
		sorted.sort(Comparator.comparing(TableDocInfo::getName));
		return sorted;
	}

	public String getQualifiedSchemaName(TableDocInfo table) {
		String schema = table.getSchema();
		String catalog = table.getCatalog();
		if (catalog != null && !catalog.isEmpty()) {
			if (schema != null && !schema.isEmpty()) {
				return catalog + "." + schema;
			}
			return catalog;
		}
		if (schema != null && !schema.isEmpty()) {
			return schema;
		}
		return DEFAULT_NO_SCHEMA;
	}

	public String getSQLTypeName(TableColumnDocInfo column) {
		return column.getJavaTypeName();
	}

	public int getLength(TableColumnDocInfo column) {
		return column.getLength();
	}

	public int getPrecision(TableColumnDocInfo column) {
		return column.getPrecision();
	}

	public int getScale(TableColumnDocInfo column) {
		return column.getScale();
	}

	public Iterator<TableColumnDocInfo> getPrimaryKeyColumnIterator(
			TableDocInfo table) {
		return table.getPrimaryKeyColumnIterator();
	}
}
