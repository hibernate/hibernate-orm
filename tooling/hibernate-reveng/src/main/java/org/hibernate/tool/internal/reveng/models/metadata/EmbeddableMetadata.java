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
 * Represents metadata for an embeddable class.
 *
 * @author Koen Aers
 */
public class EmbeddableMetadata {
	private final String className;
	private final String packageName;
	private final List<ColumnMetadata> columns = new ArrayList<>();
	private boolean idClass;

	public EmbeddableMetadata(String className, String packageName) {
		this.className = className;
		this.packageName = packageName;
	}

	public EmbeddableMetadata addColumn(ColumnMetadata column) {
		this.columns.add(column);
		return this;
	}

	public EmbeddableMetadata idClass(boolean idClass) {
		this.idClass = idClass;
		return this;
	}

	// Getters
	public String getClassName() { return className; }
	public String getPackageName() { return packageName; }
	public List<ColumnMetadata> getColumns() { return columns; }
	public boolean isIdClass() { return idClass; }
}
