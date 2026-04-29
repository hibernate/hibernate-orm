/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;

import java.util.List;

/**
 * Represents metadata for a OneToOne relationship.
 * Supports both the owning side (with join columns) and
 * the inverse side (with mappedBy).
 *
 * @author Koen Aers
 */
public class OneToOneDescriptor {
	private final String fieldName;
	private final String targetEntityClassName;
	private final String targetEntityPackage;
	private final JoinColumnList joinColumns = new JoinColumnList();
	private String mappedBy;
	private FetchType fetchType;
	private CascadeType[] cascadeTypes;
	private boolean optional;
	private boolean orphanRemoval;
	private boolean constrained;

	public OneToOneDescriptor(
			String fieldName,
			String targetEntityClassName,
			String targetEntityPackage) {
		this.fieldName = fieldName;
		this.targetEntityClassName = targetEntityClassName;
		this.targetEntityPackage = targetEntityPackage;
		this.optional = true;
	}

	public OneToOneDescriptor foreignKeyColumnName(String foreignKeyColumnName) {
		this.joinColumns.add(foreignKeyColumnName, null);
		return this;
	}

	public OneToOneDescriptor addJoinColumn(String fkColumnName, String referencedColumnName) {
		this.joinColumns.add(fkColumnName, referencedColumnName);
		return this;
	}

	public OneToOneDescriptor referencedColumnName(String referencedColumnName) {
		joinColumns.updateFirstReferencedColumn(referencedColumnName);
		return this;
	}

	public OneToOneDescriptor mappedBy(String mappedBy) {
		this.mappedBy = mappedBy;
		return this;
	}

	public OneToOneDescriptor fetchType(FetchType fetchType) {
		this.fetchType = fetchType;
		return this;
	}

	public OneToOneDescriptor cascade(CascadeType... cascadeTypes) {
		this.cascadeTypes = cascadeTypes;
		return this;
	}

	public OneToOneDescriptor optional(boolean optional) {
		this.optional = optional;
		return this;
	}

	public OneToOneDescriptor orphanRemoval(boolean orphanRemoval) {
		this.orphanRemoval = orphanRemoval;
		return this;
	}

	public OneToOneDescriptor constrained(boolean constrained) {
		this.constrained = constrained;
		return this;
	}

	// Getters
	public String getFieldName() { return fieldName; }
	public String getTargetEntityClassName() { return targetEntityClassName; }
	public String getTargetEntityPackage() { return targetEntityPackage; }
	public String getForeignKeyColumnName() { return joinColumns.firstForeignKeyColumnName(); }
	public String getReferencedColumnName() { return joinColumns.firstReferencedColumnName(); }
	public List<JoinColumnPair> getJoinColumns() { return joinColumns.asList(); }
	public String getMappedBy() { return mappedBy; }
	public FetchType getFetchType() { return fetchType; }
	public CascadeType[] getCascadeTypes() { return cascadeTypes; }
	public boolean isOptional() { return optional; }
	public boolean isOrphanRemoval() { return orphanRemoval; }
	public boolean isConstrained() { return constrained; }
	public List<String> getForeignKeyColumnNames() { return joinColumns.foreignKeyColumnNames(); }
}
