/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

/**
 * Represents metadata for an @AttributeOverride mapping.
 *
 * @author Koen Aers
 */
public class AttributeOverrideDescriptor {
	private final String fieldName;
	private final String columnName;
	private final Class<?> javaType;

	public AttributeOverrideDescriptor(String fieldName, String columnName) {
		this(fieldName, columnName, null);
	}

	public AttributeOverrideDescriptor(String fieldName, String columnName, Class<?> javaType) {
		this.fieldName = fieldName;
		this.columnName = columnName;
		this.javaType = javaType;
	}

	// Getters
	public String getFieldName() { return fieldName; }
	public String getColumnName() { return columnName; }
	public Class<?> getJavaType() { return javaType; }
}
