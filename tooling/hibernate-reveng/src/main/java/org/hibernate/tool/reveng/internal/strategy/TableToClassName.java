/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.strategy;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.tool.reveng.api.reveng.TableIdentifier;

class TableToClassName {

	private final Map<String, TableMapper> map = new HashMap<>();

	String get(TableIdentifier tableIdentifier) {
		TableMapper mapper = map.get(tableIdentifier.getName());
		if (mapper != null) {
			if (mapper.catalog == null || tableIdentifier.getCatalog() == null
					|| mapper.catalog.equals(tableIdentifier.getCatalog())) {
				if (mapper.schema == null || tableIdentifier.getSchema() == null
						|| mapper.schema.equals(tableIdentifier.getSchema())) {
					if (mapper.packageName.isEmpty()) {
						return mapper.className;
					}
		else {
						return mapper.packageName + "." + mapper.className;
					}
				}
			}
		}
		return null;
	}

	void put(TableIdentifier tableIdentifier, String wantedClassName) {
		TableMapper tableMapper = new TableMapper(
				tableIdentifier.getCatalog(),
				tableIdentifier.getSchema(),
				wantedClassName);
		map.put(tableIdentifier.getName(), tableMapper);
	}

	private static class TableMapper {
		final String catalog;
		final String schema;
		final String className;
		final String packageName;

		TableMapper(String catalog, String schema, String wantedClassName) {
			this.catalog = catalog;
			this.schema = schema;
			if (wantedClassName.contains(".")) {
				int nameStartPos = wantedClassName.lastIndexOf(".");
				this.className = wantedClassName.substring(nameStartPos + 1);
				this.packageName = wantedClassName.substring(0, nameStartPos);
			}
		else {
				this.className = wantedClassName;
				this.packageName = "";
			}
		}
	}
}
