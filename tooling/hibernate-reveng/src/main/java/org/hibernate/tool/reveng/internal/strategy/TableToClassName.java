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
					} else {
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
			} else {
				this.className = wantedClassName;
				this.packageName = "";
			}
		}
	}
}
