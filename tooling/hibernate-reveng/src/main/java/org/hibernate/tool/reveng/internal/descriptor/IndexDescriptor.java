/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents metadata for a database index.
 *
 * @author Koen Aers
 */
public class IndexDescriptor {
	private final String indexName;
	private final boolean unique;
	private final List<String> columnNames = new ArrayList<>();

	public IndexDescriptor(String indexName, boolean unique) {
		this.indexName = indexName;
		this.unique = unique;
	}

	public IndexDescriptor addColumn(String columnName) {
		this.columnNames.add(columnName);
		return this;
	}

	public String getIndexName() { return indexName; }
	public boolean isUnique() { return unique; }
	public List<String> getColumnNames() { return columnNames; }
}
