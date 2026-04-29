/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

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
