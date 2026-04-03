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
package org.hibernate.tool.internal.reveng.models.exporter.doc;

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
