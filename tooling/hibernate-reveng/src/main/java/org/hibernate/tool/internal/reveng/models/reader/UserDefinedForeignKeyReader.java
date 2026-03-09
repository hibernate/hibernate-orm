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
package org.hibernate.tool.internal.reveng.models.reader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;

/**
 * Reads user-defined foreign keys from {@link RevengStrategy#getForeignKeys(TableIdentifier)}
 * and converts them to {@link RawForeignKeyInfo} records so they can be
 * merged into the standard FK processing pipeline.
 *
 * @author Koen Aers
 */
class UserDefinedForeignKeyReader {

	private final RevengStrategy strategy;
	private final String defaultCatalog;
	private final String defaultSchema;

	static UserDefinedForeignKeyReader create(RevengStrategy strategy,
			String defaultCatalog, String defaultSchema) {
		return new UserDefinedForeignKeyReader(strategy, defaultCatalog, defaultSchema);
	}

	private UserDefinedForeignKeyReader(RevengStrategy strategy,
			String defaultCatalog, String defaultSchema) {
		this.strategy = strategy;
		this.defaultCatalog = defaultCatalog;
		this.defaultSchema = defaultSchema;
	}

	/**
	 * Reads user-defined foreign keys for all discovered tables.
	 *
	 * @param tablesByName the discovered tables keyed by table name
	 * @return all user-defined foreign key info records
	 */
	List<RawForeignKeyInfo> readUserForeignKeys(Map<String, TableMetadata> tablesByName) {
		List<RawForeignKeyInfo> result = new ArrayList<>();
		for (Map.Entry<String, TableMetadata> entry : tablesByName.entrySet()) {
			TableMetadata tableMetadata = entry.getValue();
			String catalog = tableMetadata.getCatalog() != null
				? tableMetadata.getCatalog() : defaultCatalog;
			String schema = tableMetadata.getSchema() != null
				? tableMetadata.getSchema() : defaultSchema;

			TableIdentifier tableId = TableIdentifier.create(catalog, schema, tableMetadata.getTableName());
			List<ForeignKey> userFks = strategy.getForeignKeys(tableId);
			if (userFks != null) {
				for (ForeignKey fk : userFks) {
					result.addAll(convertForeignKey(fk));
				}
			}
		}
		return result;
	}

	private List<RawForeignKeyInfo> convertForeignKey(ForeignKey fk) {
		List<RawForeignKeyInfo> result = new ArrayList<>();
		String fkName = fk.getName();
		Table fkTable = fk.getTable();
		Table referencedTable = fk.getReferencedTable();

		String fkTableName = fkTable != null ? fkTable.getName() : null;
		String fkTableCatalog = fkTable != null ? fkTable.getCatalog() : null;
		String fkTableSchema = fkTable != null ? fkTable.getSchema() : null;

		String referencedTableName = referencedTable != null ? referencedTable.getName() : null;
		String referencedCatalog = referencedTable != null ? referencedTable.getCatalog() : null;
		String referencedSchema = referencedTable != null ? referencedTable.getSchema() : null;

		List<?> fkColumns = fk.getColumns();
		List<?> refColumns = fk.getReferencedColumns();

		for (int i = 0; i < fkColumns.size(); i++) {
			Column fkColumn = (Column) fkColumns.get(i);
			String pkColumnName = (refColumns != null && i < refColumns.size())
				? ((Column) refColumns.get(i)).getName()
				: null;

			result.add(new RawForeignKeyInfo(
				fkName,
				fkTableName, fkTableCatalog, fkTableSchema,
				fkColumn.getName(), pkColumnName,
				referencedTableName, referencedCatalog, referencedSchema,
				i + 1));
		}
		return result;
	}
}
