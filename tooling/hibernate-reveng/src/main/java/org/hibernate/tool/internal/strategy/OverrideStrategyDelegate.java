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

import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.tool.api.reveng.AssociationInfo;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.util.TypeHelper;
import org.hibernate.tool.internal.util.TableNameQualifier;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Named strategy delegate extracted from the anonymous class in
 * {@link OverrideRepository#getReverseEngineeringStrategy(RevengStrategy)}.
 *
 * @author Koen Aers
 */
class OverrideStrategyDelegate extends DelegatingStrategy {

	private static final Logger log = Logger.getLogger(OverrideStrategyDelegate.class);

	private final OverrideRepository repository;

	OverrideStrategyDelegate(OverrideRepository repository, RevengStrategy delegate) {
		super(delegate);
		this.repository = repository;
	}

	@Override
	public boolean excludeTable(TableIdentifier ti) {
		return repository.excludeTable(ti);
	}

	@Override
	public Map<String, MetaAttribute> tableToMetaAttributes(TableIdentifier tableIdentifier) {
		return repository.tableToMetaAttributes(tableIdentifier);
	}

	@Override
	public Map<String, MetaAttribute> columnToMetaAttributes(
			TableIdentifier tableIdentifier, String column) {
		return repository.columnToMetaAttributes(tableIdentifier, column);
	}

	@Override
	public boolean excludeColumn(TableIdentifier identifier, String columnName) {
		return repository.excludedColumns.contains(
				new TableColumnKey(identifier, columnName));
	}

	@Override
	public String tableToCompositeIdName(TableIdentifier identifier) {
		String result = repository.compositeIdNameForTable.get(identifier);
		return result != null ? result : super.tableToCompositeIdName(identifier);
	}

	@Override
	public List<SchemaSelection> getSchemaSelections() {
		return repository.schemaSelections.isEmpty()
				? super.getSchemaSelections()
				: repository.schemaSelections;
	}

	@Override
	public String columnToHibernateTypeName(
			TableIdentifier table, String columnName,
			int sqlType, int length, int precision, int scale,
			boolean nullable, boolean generatedIdentifier) {
		String result;
		String location = "";
		String info = " t:" + TypeHelper.getJDBCTypeName(sqlType)
				+ " l:" + length + " p:" + precision + " s:" + scale
				+ " n:" + nullable + " id:" + generatedIdentifier;
		if (table != null) {
			location = TableNameQualifier.qualify(
					table.getCatalog(), table.getSchema(), table.getName())
					+ "." + columnName;
		} else {
			location += " Column: " + columnName + info;
		}
		if (table != null && columnName != null) {
			result = repository.typeForColumn.get(
					new TableColumnKey(table, columnName));
			if (result != null) {
				log.debug("explicit column mapping found for ["
						+ location + "] to [" + result + "]");
				return result;
			}
		}
		result = repository.getPreferredHibernateType(
				sqlType, length, precision, scale, nullable);
		if (result == null) {
			return super.columnToHibernateTypeName(
					table, columnName, sqlType, length, precision,
					scale, nullable, generatedIdentifier);
		} else {
			log.debug("<type-mapping> found for ["
					+ location + info + "] to [" + result + "]");
			return result;
		}
	}

	@Override
	public String tableToClassName(TableIdentifier tableIdentifier) {
		String className = repository.tableToClassName.get(tableIdentifier);
		if (className != null) {
			if (className.contains(".")) {
				return className;
			} else {
				String packageName = repository.getPackageName(tableIdentifier);
				return packageName == null
						? className
						: StringHelper.qualify(packageName, className);
			}
		}
		String packageName = repository.getPackageName(tableIdentifier);
		if (packageName == null) {
			return super.tableToClassName(tableIdentifier);
		} else {
			String string = super.tableToClassName(tableIdentifier);
			if (string == null) return null;
			return StringHelper.qualify(packageName, StringHelper.unqualify(string));
		}
	}

	@Override
	public List<ForeignKey> getForeignKeys(TableIdentifier referencedTable) {
		List<ForeignKey> list = repository.foreignKeys.get(referencedTable);
		return list != null ? list : super.getForeignKeys(referencedTable);
	}

	@Override
	public String columnToPropertyName(TableIdentifier table, String column) {
		String result = repository.propertyNameForColumn.get(
				new TableColumnKey(table, column));
		return result != null ? result : super.columnToPropertyName(table, column);
	}

	@Override
	public String tableToIdentifierPropertyName(TableIdentifier tableIdentifier) {
		String result = repository.propertyNameForPrimaryKey.get(tableIdentifier);
		return result != null
				? result
				: super.tableToIdentifierPropertyName(tableIdentifier);
	}

	@Override
	public String getTableIdentifierStrategyName(TableIdentifier tableIdentifier) {
		String result = repository.identifierStrategyForTable.get(tableIdentifier);
		if (result == null) {
			return super.getTableIdentifierStrategyName(tableIdentifier);
		} else {
			log.debug("tableIdentifierStrategy for "
					+ tableIdentifier + " -> '" + result + "'");
			return result;
		}
	}

	@Override
	public Properties getTableIdentifierProperties(TableIdentifier tableIdentifier) {
		Properties result = repository.identifierPropertiesForTable.get(tableIdentifier);
		return result != null
				? result
				: super.getTableIdentifierProperties(tableIdentifier);
	}

	@Override
	public List<String> getPrimaryKeyColumnNames(TableIdentifier tableIdentifier) {
		List<String> result = repository.primaryKeyColumnsForTable.get(tableIdentifier);
		return result != null
				? result
				: super.getPrimaryKeyColumnNames(tableIdentifier);
	}

	@Override
	public String foreignKeyToEntityName(
			String keyname, TableIdentifier fromTable,
			List<?> fromColumnNames, TableIdentifier referencedTable,
			List<?> referencedColumnNames, boolean uniqueReference) {
		String property = repository.foreignKeyToOneName.get(keyname);
		return property != null
				? property
				: super.foreignKeyToEntityName(keyname, fromTable,
						fromColumnNames, referencedTable,
						referencedColumnNames, uniqueReference);
	}

	@Override
	public String foreignKeyToInverseEntityName(
			String keyname, TableIdentifier fromTable,
			List<?> fromColumnNames, TableIdentifier referencedTable,
			List<?> referencedColumnNames, boolean uniqueReference) {
		String property = repository.foreignKeyToInverseName.get(keyname);
		return property != null
				? property
				: super.foreignKeyToInverseEntityName(keyname, fromTable,
						fromColumnNames, referencedTable,
						referencedColumnNames, uniqueReference);
	}

	@Override
	public String foreignKeyToCollectionName(
			String keyname, TableIdentifier fromTable,
			List<?> fromColumns, TableIdentifier referencedTable,
			List<?> referencedColumns, boolean uniqueReference) {
		String property = repository.foreignKeyToInverseName.get(keyname);
		return property != null
				? property
				: super.foreignKeyToCollectionName(keyname, fromTable,
						fromColumns, referencedTable,
						referencedColumns, uniqueReference);
	}

	@Override
	public boolean excludeForeignKeyAsCollection(
			String keyname, TableIdentifier fromTable,
			List<?> fromColumns, TableIdentifier referencedTable,
			List<?> referencedColumns) {
		Boolean bool = repository.foreignKeyInverseExclude.get(keyname);
		return Objects.requireNonNullElseGet(bool,
				() -> super.excludeForeignKeyAsCollection(keyname, fromTable,
						fromColumns, referencedTable, referencedColumns));
	}

	@Override
	public boolean excludeForeignKeyAsManytoOne(
			String keyname, TableIdentifier fromTable,
			List<?> fromColumns, TableIdentifier referencedTable,
			List<?> referencedColumns) {
		Boolean bool = repository.foreignKeyToOneExclude.get(keyname);
		return Objects.requireNonNullElseGet(bool,
				() -> super.excludeForeignKeyAsManytoOne(keyname, fromTable,
						fromColumns, referencedTable, referencedColumns));
	}

	@Override
	public AssociationInfo foreignKeyToInverseAssociationInfo(ForeignKey foreignKey) {
		AssociationInfo fkei = repository.foreignKeyToInverseEntityInfo.get(
				foreignKey.getName());
		return fkei != null
				? fkei
				: super.foreignKeyToInverseAssociationInfo(foreignKey);
	}

	@Override
	public AssociationInfo foreignKeyToAssociationInfo(ForeignKey foreignKey) {
		AssociationInfo fkei = repository.foreignKeyToEntityInfo.get(
				foreignKey.getName());
		return fkei != null
				? fkei
				: super.foreignKeyToAssociationInfo(foreignKey);
	}
}
