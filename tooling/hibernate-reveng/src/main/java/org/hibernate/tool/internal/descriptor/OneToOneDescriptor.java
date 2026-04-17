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

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;

import java.util.ArrayList;
import java.util.Collections;
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
	private final List<JoinColumnPair> joinColumns = new ArrayList<>();
	private String mappedBy;
	private FetchType fetchType;
	private CascadeType[] cascadeTypes;
	private boolean optional;
	private boolean orphanRemoval;
	private boolean constrained;

	public record JoinColumnPair(String fkColumnName, String referencedColumnName) {}

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
		this.joinColumns.add(new JoinColumnPair(foreignKeyColumnName, null));
		return this;
	}

	public OneToOneDescriptor addJoinColumn(String fkColumnName, String referencedColumnName) {
		this.joinColumns.add(new JoinColumnPair(fkColumnName, referencedColumnName));
		return this;
	}

	public OneToOneDescriptor referencedColumnName(String referencedColumnName) {
		if (!joinColumns.isEmpty()) {
			JoinColumnPair first = joinColumns.get(0);
			joinColumns.set(0, new JoinColumnPair(first.fkColumnName(), referencedColumnName));
		}
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
	/** Returns the first FK column name, or null if no join columns. */
	public String getForeignKeyColumnName() {
		return joinColumns.isEmpty() ? null : joinColumns.get(0).fkColumnName();
	}
	/** Returns the first referenced column name, or null. */
	public String getReferencedColumnName() {
		return joinColumns.isEmpty() ? null : joinColumns.get(0).referencedColumnName();
	}
	public List<JoinColumnPair> getJoinColumns() {
		return Collections.unmodifiableList(joinColumns);
	}
	public String getMappedBy() { return mappedBy; }
	public FetchType getFetchType() { return fetchType; }
	public CascadeType[] getCascadeTypes() { return cascadeTypes; }
	public boolean isOptional() { return optional; }
	public boolean isOrphanRemoval() { return orphanRemoval; }
	public boolean isConstrained() { return constrained; }
	/** Returns all FK column names in this OneToOne relationship. */
	public List<String> getForeignKeyColumnNames() {
		return joinColumns.stream().map(JoinColumnPair::fkColumnName).toList();
	}
}
