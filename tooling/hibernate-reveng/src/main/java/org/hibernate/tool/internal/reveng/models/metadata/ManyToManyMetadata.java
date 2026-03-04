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
 * Represents metadata for a ManyToMany relationship.
 * Supports both the owning side (with joinTable) and
 * the inverse side (with mappedBy).
 *
 * @author Koen Aers
 */
public class ManyToManyMetadata {
	private final String fieldName;
	private final String targetEntityClassName;
	private final String targetEntityPackage;
	private String mappedBy;
	private String joinTableName;
	private String joinColumnName;
	private String inverseJoinColumnName;
	private FetchType fetchType;
	private CascadeType[] cascadeTypes;

	public ManyToManyMetadata(
			String fieldName,
			String targetEntityClassName,
			String targetEntityPackage) {
		this.fieldName = fieldName;
		this.targetEntityClassName = targetEntityClassName;
		this.targetEntityPackage = targetEntityPackage;
	}

	public ManyToManyMetadata mappedBy(String mappedBy) {
		this.mappedBy = mappedBy;
		return this;
	}

	public ManyToManyMetadata joinTable(String joinTableName, String joinColumnName, String inverseJoinColumnName) {
		this.joinTableName = joinTableName;
		this.joinColumnName = joinColumnName;
		this.inverseJoinColumnName = inverseJoinColumnName;
		return this;
	}

	public ManyToManyMetadata fetchType(FetchType fetchType) {
		this.fetchType = fetchType;
		return this;
	}

	public ManyToManyMetadata cascade(CascadeType... cascadeTypes) {
		this.cascadeTypes = cascadeTypes;
		return this;
	}

	// Getters
	public String getFieldName() { return fieldName; }
	public String getTargetEntityClassName() { return targetEntityClassName; }
	public String getTargetEntityPackage() { return targetEntityPackage; }
	public String getMappedBy() { return mappedBy; }
	public String getJoinTableName() { return joinTableName; }
	public String getJoinColumnName() { return joinColumnName; }
	public String getInverseJoinColumnName() { return inverseJoinColumnName; }
	public FetchType getFetchType() { return fetchType; }
	public CascadeType[] getCascadeTypes() { return cascadeTypes; }
}
