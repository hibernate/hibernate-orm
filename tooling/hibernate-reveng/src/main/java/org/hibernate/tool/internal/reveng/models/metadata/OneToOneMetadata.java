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

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;

/**
 * Represents metadata for a OneToOne relationship.
 * Supports both the owning side (with foreignKeyColumnName) and
 * the inverse side (with mappedBy).
 *
 * @author Koen Aers
 */
public class OneToOneMetadata {
	private final String fieldName;
	private final String targetEntityClassName;
	private final String targetEntityPackage;
	private String foreignKeyColumnName;
	private String referencedColumnName;
	private String mappedBy;
	private FetchType fetchType;
	private CascadeType[] cascadeTypes;
	private boolean optional;
	private boolean orphanRemoval;

	public OneToOneMetadata(
			String fieldName,
			String targetEntityClassName,
			String targetEntityPackage) {
		this.fieldName = fieldName;
		this.targetEntityClassName = targetEntityClassName;
		this.targetEntityPackage = targetEntityPackage;
		this.optional = true;
	}

	public OneToOneMetadata foreignKeyColumnName(String foreignKeyColumnName) {
		this.foreignKeyColumnName = foreignKeyColumnName;
		return this;
	}

	public OneToOneMetadata referencedColumnName(String referencedColumnName) {
		this.referencedColumnName = referencedColumnName;
		return this;
	}

	public OneToOneMetadata mappedBy(String mappedBy) {
		this.mappedBy = mappedBy;
		return this;
	}

	public OneToOneMetadata fetchType(FetchType fetchType) {
		this.fetchType = fetchType;
		return this;
	}

	public OneToOneMetadata cascade(CascadeType... cascadeTypes) {
		this.cascadeTypes = cascadeTypes;
		return this;
	}

	public OneToOneMetadata optional(boolean optional) {
		this.optional = optional;
		return this;
	}

	public OneToOneMetadata orphanRemoval(boolean orphanRemoval) {
		this.orphanRemoval = orphanRemoval;
		return this;
	}

	// Getters
	public String getFieldName() { return fieldName; }
	public String getTargetEntityClassName() { return targetEntityClassName; }
	public String getTargetEntityPackage() { return targetEntityPackage; }
	public String getForeignKeyColumnName() { return foreignKeyColumnName; }
	public String getReferencedColumnName() { return referencedColumnName; }
	public String getMappedBy() { return mappedBy; }
	public FetchType getFetchType() { return fetchType; }
	public CascadeType[] getCascadeTypes() { return cascadeTypes; }
	public boolean isOptional() { return optional; }
	public boolean isOrphanRemoval() { return orphanRemoval; }
}
