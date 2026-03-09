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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.tool.api.reveng.RevengDialect;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.IndexMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;

/**
 * Reads index information for a table from the database
 * via {@link RevengDialect#getIndexInfo(String, String, String)}
 * and populates the given {@link TableMetadata} with
 * {@link IndexMetadata} entries. For single-column unique indexes,
 * also marks the corresponding {@link ColumnMetadata} as unique.
 *
 * @author Koen Aers
 */
class IndexReader {

	private final RevengDialect dialect;

	static IndexReader create(RevengDialect dialect) {
		return new IndexReader(dialect);
	}

	private IndexReader(RevengDialect dialect) {
		this.dialect = dialect;
	}

	/**
	 * Reads indexes for the given table and populates the table metadata.
	 */
	void readIndexes(TableMetadata tableMetadata, String catalog, String schema) {
		for (IndexMetadata index : collectIndexMetadatas(tableMetadata.getTableName(), catalog, schema)) {
			tableMetadata.addIndex(index);

			// For single-column unique indexes, mark the column as unique
			if (index.isUnique() && index.getColumnNames().size() == 1) {
				String columnName = index.getColumnNames().get(0);
				for (ColumnMetadata col : tableMetadata.getColumns()) {
					if (col.getColumnName().equals(columnName)) {
						col.unique(true);
						break;
					}
				}
			}
		}
	}

	private Collection<IndexMetadata> collectIndexMetadatas(String tableName, String catalog, String schema) {
		Map<String, IndexMetadata> indexesByName = new LinkedHashMap<>();

		Iterator<Map<String, Object>> indexIterator = dialect.getIndexInfo(
			catalog, schema, tableName);
		try {
			while (indexIterator.hasNext()) {
				Map<String, Object> row = indexIterator.next();
				String indexName = (String) row.get("INDEX_NAME");
				String columnName = quote((String) row.get("COLUMN_NAME"), dialect);

				// Skip statistical indexes (null column or index name)
				if (indexName == null || columnName == null) {
					continue;
				}

				boolean nonUnique = row.get("NON_UNIQUE") instanceof Boolean
					? (Boolean) row.get("NON_UNIQUE")
					: true;

				IndexMetadata index = indexesByName.get(indexName);
				if (index == null) {
					index = new IndexMetadata(indexName, !nonUnique);
					indexesByName.put(indexName, index);
				}
				index.addColumn(columnName);
			}
		} finally {
			dialect.close(indexIterator);
		}

		return indexesByName.values();
	}

	private static String quote(String name, RevengDialect dialect) {
		if (name == null) return null;
		if (dialect.needQuote(name)) {
			if (name.length() > 1 && name.charAt(0) == '`'
					&& name.charAt(name.length() - 1) == '`') {
				return name;
			}
			return "`" + name + "`";
		}
		return name;
	}
}
