/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents metadata for a OneToMany (inverse) relationship.
 *
 * @author Koen Aers
 */
public class OneToManyDescriptor {
	private final String fieldName;
	private final String mappedBy;
	private final String elementEntityClassName;
	private final String elementEntityPackage;
	private final List<String> fkColumnNames = new ArrayList<>();
	private FetchType fetchType;
	private CascadeType[] cascadeTypes;
	private boolean orphanRemoval;

	public OneToManyDescriptor(
			String fieldName,
			String mappedBy,
			String elementEntityClassName,
			String elementEntityPackage) {
		this.fieldName = fieldName;
		this.mappedBy = mappedBy;
		this.elementEntityClassName = elementEntityClassName;
		this.elementEntityPackage = elementEntityPackage;
	}

	public OneToManyDescriptor fetchType(FetchType fetchType) {
		this.fetchType = fetchType;
		return this;
	}

	public OneToManyDescriptor cascade(CascadeType... cascadeTypes) {
		this.cascadeTypes = cascadeTypes;
		return this;
	}

	public OneToManyDescriptor orphanRemoval(boolean orphanRemoval) {
		this.orphanRemoval = orphanRemoval;
		return this;
	}

	public OneToManyDescriptor fkColumnNames(List<String> columnNames) {
		this.fkColumnNames.addAll(columnNames);
		return this;
	}

	// Getters
	public String getFieldName() { return fieldName; }
	public String getMappedBy() { return mappedBy; }
	public String getElementEntityClassName() { return elementEntityClassName; }
	public String getElementEntityPackage() { return elementEntityPackage; }
	public List<String> getFkColumnNames() { return Collections.unmodifiableList(fkColumnNames); }
	public FetchType getFetchType() { return fetchType; }
	public CascadeType[] getCascadeTypes() { return cascadeTypes; }
	public boolean isOrphanRemoval() { return orphanRemoval; }
}
