/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.doc;

import java.util.List;

/**
 * Represents a primary key for table documentation templates.
 * Templates access {@code table.primaryKey.name}.
 *
 * @author Koen Aers
 */
public class PrimaryKeyDocInfo {

	private final String name;
	private final List<TableColumnDocInfo> columns;

	public PrimaryKeyDocInfo(String name, List<TableColumnDocInfo> columns) {
		this.name = name;
		this.columns = columns;
	}

	public String getName() {
		return name;
	}

	public List<TableColumnDocInfo> getColumns() {
		return columns;
	}
}
