/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.doc;

import java.util.List;

/**
 * Represents a foreign key for table documentation templates.
 * Templates access {@code foreignKey.name},
 * {@code foreignKey.referencedTable}, and {@code foreignKey.columns}.
 *
 * @author Koen Aers
 */
public class ForeignKeyDocInfo {

	private final String name;
	private final TableDocInfo referencedTable;
	private final List<TableColumnDocInfo> columns;

	public ForeignKeyDocInfo(String name, TableDocInfo referencedTable,
							List<TableColumnDocInfo> columns) {
		this.name = name;
		this.referencedTable = referencedTable;
		this.columns = columns;
	}

	public String getName() {
		return name;
	}

	public TableDocInfo getReferencedTable() {
		return referencedTable;
	}

	public List<TableColumnDocInfo> getColumns() {
		return columns;
	}
}
