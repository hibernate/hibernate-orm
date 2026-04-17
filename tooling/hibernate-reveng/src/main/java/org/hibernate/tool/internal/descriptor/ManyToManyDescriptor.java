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

import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;

/**
 * Represents metadata for a ManyToMany relationship.
 * Supports both the owning side (with joinTable) and
 * the inverse side (with mappedBy).
 *
 * @author Koen Aers
 */
public class ManyToManyDescriptor {
	private final String fieldName;
	private final String targetEntityClassName;
	private final String targetEntityPackage;
	private String mappedBy;
	private String joinTableName;
	private String joinTableSchema;
	private String joinTableCatalog;
	private String joinColumnName;
	private String inverseJoinColumnName;
	private List<String> joinColumnNames;
	private List<String> inverseJoinColumnNames;
	private FetchType fetchType;
	private CascadeType[] cascadeTypes;

	public ManyToManyDescriptor(
			String fieldName,
			String targetEntityClassName,
			String targetEntityPackage) {
		this.fieldName = fieldName;
		this.targetEntityClassName = targetEntityClassName;
		this.targetEntityPackage = targetEntityPackage;
	}

	public ManyToManyDescriptor mappedBy(String mappedBy) {
		this.mappedBy = mappedBy;
		return this;
	}

	public ManyToManyDescriptor joinTableSchema(String schema) {
		this.joinTableSchema = schema;
		return this;
	}

	public ManyToManyDescriptor joinTableCatalog(String catalog) {
		this.joinTableCatalog = catalog;
		return this;
	}

	public ManyToManyDescriptor joinTable(String joinTableName, String joinColumnName, String inverseJoinColumnName) {
		this.joinTableName = joinTableName;
		this.joinColumnName = joinColumnName;
		this.inverseJoinColumnName = inverseJoinColumnName;
		return this;
	}

	public ManyToManyDescriptor joinTable(String joinTableName, List<String> joinColumnNames, List<String> inverseJoinColumnNames) {
		this.joinTableName = joinTableName;
		this.joinColumnNames = joinColumnNames;
		this.inverseJoinColumnNames = inverseJoinColumnNames;
		if (!joinColumnNames.isEmpty()) {
			this.joinColumnName = joinColumnNames.get(0);
		}
		if (!inverseJoinColumnNames.isEmpty()) {
			this.inverseJoinColumnName = inverseJoinColumnNames.get(0);
		}
		return this;
	}

	public ManyToManyDescriptor fetchType(FetchType fetchType) {
		this.fetchType = fetchType;
		return this;
	}

	public ManyToManyDescriptor cascade(CascadeType... cascadeTypes) {
		this.cascadeTypes = cascadeTypes;
		return this;
	}

	// Getters
	public String getFieldName() { return fieldName; }
	public String getTargetEntityClassName() { return targetEntityClassName; }
	public String getTargetEntityPackage() { return targetEntityPackage; }
	public String getMappedBy() { return mappedBy; }
	public String getJoinTableName() { return joinTableName; }
	public String getJoinTableSchema() { return joinTableSchema; }
	public String getJoinTableCatalog() { return joinTableCatalog; }
	public String getJoinColumnName() { return joinColumnName; }
	public String getInverseJoinColumnName() { return inverseJoinColumnName; }
	public List<String> getJoinColumnNames() {
		if (joinColumnNames != null) { return joinColumnNames; }
		if (joinColumnName != null) { return Collections.singletonList(joinColumnName); }
		return Collections.emptyList();
	}
	public List<String> getInverseJoinColumnNames() {
		if (inverseJoinColumnNames != null) { return inverseJoinColumnNames; }
		if (inverseJoinColumnName != null) { return Collections.singletonList(inverseJoinColumnName); }
		return Collections.emptyList();
	}
	public FetchType getFetchType() { return fetchType; }
	public CascadeType[] getCascadeTypes() { return cascadeTypes; }
}
