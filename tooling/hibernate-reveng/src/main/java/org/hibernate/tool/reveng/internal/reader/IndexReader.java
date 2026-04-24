/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.reader;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.tool.reveng.api.reveng.RevengDialect;
import org.hibernate.tool.reveng.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.IndexDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.TableDescriptor;

/**
 * Reads index information for a table from the database
 * via {@link RevengDialect#getIndexInfo(String, String, String)}
 * and populates the given {@link TableDescriptor} with
 * {@link IndexDescriptor} entries. For single-column unique indexes,
 * also marks the corresponding {@link ColumnDescriptor} as unique.
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
	void readIndexes(TableDescriptor tableMetadata, String catalog, String schema) {
		for (IndexDescriptor index : collectIndexDescriptors(unquote(tableMetadata.getTableName()), catalog, schema)) {
			tableMetadata.addIndex(index);

			// For single-column unique indexes, mark the column as unique
			if (index.isUnique() && index.getColumnNames().size() == 1) {
				String columnName = index.getColumnNames().get(0);
				for (ColumnDescriptor col : tableMetadata.getColumns()) {
					if (col.getColumnName().equals(columnName)) {
						col.unique(true);
						break;
					}
				}
			}
		}
	}

	private Collection<IndexDescriptor> collectIndexDescriptors(String tableName, String catalog, String schema) {
		Map<String, IndexDescriptor> indexesByName = new LinkedHashMap<>();

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

				IndexDescriptor index = indexesByName.get(indexName);
				if (index == null) {
					index = new IndexDescriptor(indexName, !nonUnique);
					indexesByName.put(indexName, index);
				}
				index.addColumn(columnName);
			}
		}
		finally {
			dialect.close(indexIterator);
		}

		return indexesByName.values();
	}

	private static String unquote(String name) {
		if (name != null && name.length() > 1
				&& name.charAt(0) == '`' && name.charAt(name.length() - 1) == '`') {
			return name.substring(1, name.length() - 1);
		}
		return name;
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
