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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.GenerationType;

import org.hibernate.tool.api.reveng.RevengDialect;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;

/**
 * Reads primary key column names for a table from the database
 * metadata via {@link RevengDialect}, with a fallback to
 * {@link RevengStrategy#getPrimaryKeyColumnNames(TableIdentifier)}.
 *
 * @author Koen Aers
 */
class PrimaryKeyReader {

	private final RevengDialect dialect;
	private final RevengStrategy strategy;

	static PrimaryKeyReader create(RevengDialect dialect, RevengStrategy strategy) {
		return new PrimaryKeyReader(dialect, strategy);
	}

	private PrimaryKeyReader(RevengDialect dialect, RevengStrategy strategy) {
		this.dialect = dialect;
		this.strategy = strategy;
	}

	/**
	 * Returns the set of primary key column names for the given table.
	 * First reads from JDBC metadata; if empty, falls back to the strategy.
	 */
	Set<String> readPrimaryKeys(String catalog, String schema, String tableName, TableIdentifier tableId) {
		List<String> pkList = new ArrayList<>();

		Iterator<Map<String, Object>> pkIterator = dialect.getPrimaryKeys(catalog, schema, tableName);
		try {
			while (pkIterator.hasNext()) {
				pkList.add(quote((String) pkIterator.next().get("COLUMN_NAME"), dialect));
			}
		} finally {
			dialect.close(pkIterator);
		}

		if (pkList.isEmpty()) {
			List<String> strategyPks = strategy.getPrimaryKeyColumnNames(tableId);
			if (strategyPks != null) {
				pkList.addAll(strategyPks);
			}
		}

		return new HashSet<>(pkList);
	}

	/**
	 * Reads the identifier generation strategy for the given table.
	 * First checks if the strategy has a user-defined strategy name,
	 * then falls back to the dialect's suggested strategy.
	 *
	 * @return the strategy name, or null if none is configured
	 */
	String readIdentifierStrategy(String catalog, String schema,
			String tableName, TableIdentifier tableId) {
		String strategyName = strategy.getTableIdentifierStrategyName(tableId);
		if (strategyName != null) {
			return strategyName;
		}

		Iterator<Map<String, Object>> iterator = dialect.getSuggestedPrimaryKeyStrategyName(
			catalog, schema, tableName);
		try {
			if (iterator.hasNext()) {
				Map<String, Object> row = iterator.next();
				return (String) row.get("HIBERNATE_STRATEGY");
			}
		} finally {
			dialect.close(iterator);
		}

		return null;
	}

	/**
	 * Maps a strategy name string to a {@link GenerationType}.
	 *
	 * @param strategyName the strategy name (e.g., "identity", "sequence", "assigned")
	 * @return the corresponding GenerationType, or null for "assigned" or null input
	 */
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

	static GenerationType toGenerationType(String strategyName) {
		if (strategyName == null || "assigned".equals(strategyName)) {
			return null;
		}
		if ("identity".equals(strategyName)) {
			return GenerationType.IDENTITY;
		}
		if ("sequence".equals(strategyName)) {
			return GenerationType.SEQUENCE;
		}
		return GenerationType.AUTO;
	}
}
