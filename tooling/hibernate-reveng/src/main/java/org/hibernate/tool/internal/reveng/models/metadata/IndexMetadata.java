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
package org.hibernate.tool.internal.reveng.models.metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents metadata for a database index.
 *
 * @author Koen Aers
 */
public class IndexMetadata {
	private final String indexName;
	private final boolean unique;
	private final List<String> columnNames = new ArrayList<>();

	public IndexMetadata(String indexName, boolean unique) {
		this.indexName = indexName;
		this.unique = unique;
	}

	public IndexMetadata addColumn(String columnName) {
		this.columnNames.add(columnName);
		return this;
	}

	public String getIndexName() { return indexName; }
	public boolean isUnique() { return unique; }
	public List<String> getColumnNames() { return columnNames; }
}
