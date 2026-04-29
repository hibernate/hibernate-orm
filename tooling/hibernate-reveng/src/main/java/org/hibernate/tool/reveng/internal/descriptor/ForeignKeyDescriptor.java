/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

import jakarta.persistence.FetchType;

import java.util.List;

/**
 * Represents metadata for a database foreign key relationship.
 * Supports both single-column and multi-column (composite) foreign keys.
 *
 * @author Koen Aers
 */
public class ForeignKeyDescriptor {
	private final String fieldName;
	private final JoinColumnList joinColumns = new JoinColumnList();
	private final String targetEntityClassName;
	private final String targetEntityPackage;
	private FetchType fetchType;
	private boolean optional;
	private boolean partOfCompositeKey;

	public ForeignKeyDescriptor(
			String fieldName,
			String foreignKeyColumnName,
			String targetEntityClassName,
			String targetEntityPackage) {
		this.fieldName = fieldName;
		this.joinColumns.add(foreignKeyColumnName, null);
		this.targetEntityClassName = targetEntityClassName;
		this.targetEntityPackage = targetEntityPackage;
		this.optional = true;
	}

	public ForeignKeyDescriptor(
			String fieldName,
			String targetEntityClassName,
			String targetEntityPackage) {
		this.fieldName = fieldName;
		this.targetEntityClassName = targetEntityClassName;
		this.targetEntityPackage = targetEntityPackage;
		this.optional = true;
	}

	public ForeignKeyDescriptor addJoinColumn(String fkColumnName, String referencedColumnName) {
		this.joinColumns.add(fkColumnName, referencedColumnName);
		return this;
	}

	public ForeignKeyDescriptor referencedColumnName(String referencedColumnName) {
		joinColumns.updateFirstReferencedColumn(referencedColumnName);
		return this;
	}

	public ForeignKeyDescriptor fetchType(FetchType fetchType) {
		this.fetchType = fetchType;
		return this;
	}

	public ForeignKeyDescriptor optional(boolean optional) {
		this.optional = optional;
		return this;
	}

	public ForeignKeyDescriptor partOfCompositeKey(boolean partOfCompositeKey) {
		this.partOfCompositeKey = partOfCompositeKey;
		return this;
	}

	// Getters
	public String getFieldName() { return fieldName; }
	public String getForeignKeyColumnName() { return joinColumns.firstForeignKeyColumnName(); }
	public String getReferencedColumnName() { return joinColumns.firstReferencedColumnName(); }
	public List<JoinColumnPair> getJoinColumns() { return joinColumns.asList(); }
	public List<String> getForeignKeyColumnNames() { return joinColumns.foreignKeyColumnNames(); }
	public String getTargetEntityClassName() { return targetEntityClassName; }
	public String getTargetEntityPackage() { return targetEntityPackage; }
	public FetchType getFetchType() { return fetchType; }
	public boolean isOptional() { return optional; }
	public boolean isPartOfCompositeKey() { return partOfCompositeKey; }
}
