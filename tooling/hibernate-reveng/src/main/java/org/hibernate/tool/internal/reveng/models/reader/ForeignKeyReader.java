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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.tool.api.reveng.RevengDialect;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;

/**
 * Reads exported foreign key information for all discovered tables
 * via {@link RevengDialect#getExportedKeys(String, String, String)}
 * and produces a list of {@link RawForeignKeyInfo} records.
 *
 * @author Koen Aers
 */
class ForeignKeyReader {

	private final RevengDialect dialect;
	private final String defaultCatalog;
	private final String defaultSchema;
	private int nullFkNameCounter = 0;

	static ForeignKeyReader create(RevengDialect dialect, String defaultCatalog, String defaultSchema) {
		return new ForeignKeyReader(dialect, defaultCatalog, defaultSchema);
	}

	private ForeignKeyReader(RevengDialect dialect, String defaultCatalog, String defaultSchema) {
		this.dialect = dialect;
		this.defaultCatalog = defaultCatalog;
		this.defaultSchema = defaultSchema;
	}

	/**
	 * Reads all exported foreign keys for the given tables.
	 *
	 * @param tablesByName the discovered tables keyed by table name
	 * @return all foreign key info records
	 */
	List<RawForeignKeyInfo> readForeignKeys(Map<String, TableMetadata> tablesByName) {
		List<RawForeignKeyInfo> allFks = new ArrayList<>();
		for (Map.Entry<String, TableMetadata> entry : tablesByName.entrySet()) {
			TableMetadata table = entry.getValue();
			String catalog = table.getCatalog() != null ? table.getCatalog() : defaultCatalog;
			String schema = table.getSchema() != null ? table.getSchema() : defaultSchema;

			Iterator<Map<String, Object>> fkIterator = dialect.getExportedKeys(
				catalog, schema, table.getTableName());
			try {
				while (fkIterator.hasNext()) {
					allFks.add(readForeignKey(fkIterator.next(), table.getTableName(), catalog, schema));
				}
			} finally {
				dialect.close(fkIterator);
			}
		}
		return allFks;
	}

	private RawForeignKeyInfo readForeignKey(Map<String, Object> fkRow,
			String referencedTableName, String referencedCatalog, String referencedSchema) {
		String fkName = (String) fkRow.get("FK_NAME");
		if (fkName == null) {
			fkName = "FK_UNNAMED_" + nullFkNameCounter++;
		}
		String fkTableName = (String) fkRow.get("FKTABLE_NAME");
		String fkTableCatalog = (String) fkRow.get("FKTABLE_CAT");
		String fkTableSchema = (String) fkRow.get("FKTABLE_SCHEM");
		String fkColumnName = (String) fkRow.get("FKCOLUMN_NAME");
		String pkColumnName = (String) fkRow.get("PKCOLUMN_NAME");
		int keySeq = fkRow.get("KEY_SEQ") != null
			? ((Number) fkRow.get("KEY_SEQ")).intValue() : 1;

		return new RawForeignKeyInfo(
			fkName,
			fkTableName, fkTableCatalog, fkTableSchema,
			fkColumnName, pkColumnName,
			referencedTableName, referencedCatalog, referencedSchema,
			keySeq);
	}
}
