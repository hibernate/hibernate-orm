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
package org.hibernate.tool.internal.descriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents metadata for a many-to-one relationship within a composite ID.
 * These become {@code <key-many-to-one>} elements in HBM XML.
 * Supports multi-column foreign keys (e.g., referencing composite PKs).
 *
 * @author Koen Aers
 */
public class KeyManyToOneDescriptor {
	private final String fieldName;
	private final List<String> columnNames = new ArrayList<>();
	private final String targetEntityClassName;
	private final String targetEntityPackage;

	public KeyManyToOneDescriptor(
			String fieldName,
			String columnName,
			String targetEntityClassName,
			String targetEntityPackage) {
		this.fieldName = fieldName;
		this.columnNames.add(columnName);
		this.targetEntityClassName = targetEntityClassName;
		this.targetEntityPackage = targetEntityPackage;
	}

	public KeyManyToOneDescriptor(
			String fieldName,
			List<String> columnNames,
			String targetEntityClassName,
			String targetEntityPackage) {
		this.fieldName = fieldName;
		this.columnNames.addAll(columnNames);
		this.targetEntityClassName = targetEntityClassName;
		this.targetEntityPackage = targetEntityPackage;
	}

	// Getters
	public String getFieldName() { return fieldName; }
	/** Returns the first column name (backward compatible). */
	public String getColumnName() {
		return columnNames.isEmpty() ? null : columnNames.get(0);
	}
	/** Returns all column names for this key-many-to-one. */
	public List<String> getColumnNames() {
		return Collections.unmodifiableList(columnNames);
	}
	public String getTargetEntityClassName() { return targetEntityClassName; }
	public String getTargetEntityPackage() { return targetEntityPackage; }
}
