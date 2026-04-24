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
package org.hibernate.tool.reveng.internal.descriptor;

import jakarta.persistence.FetchType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents metadata for a database foreign key relationship.
 * Supports both single-column and multi-column (composite) foreign keys.
 *
 * @author Koen Aers
 */
public class ForeignKeyDescriptor {
	private final String fieldName;
	private final List<JoinColumnPair> joinColumns = new ArrayList<>();
	private final String targetEntityClassName;
	private final String targetEntityPackage;
	private FetchType fetchType;
	private boolean optional;
	private boolean partOfCompositeKey;

	public record JoinColumnPair(String fkColumnName, String referencedColumnName) {}

	public ForeignKeyDescriptor(
			String fieldName,
			String foreignKeyColumnName,
			String targetEntityClassName,
			String targetEntityPackage) {
		this.fieldName = fieldName;
		this.joinColumns.add(new JoinColumnPair(foreignKeyColumnName, null));
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
		this.joinColumns.add(new JoinColumnPair(fkColumnName, referencedColumnName));
		return this;
	}

	public ForeignKeyDescriptor referencedColumnName(String referencedColumnName) {
		if (!joinColumns.isEmpty()) {
			JoinColumnPair first = joinColumns.get(0);
			joinColumns.set(0, new JoinColumnPair(first.fkColumnName(), referencedColumnName));
		}
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
	/** Returns the first FK column name (backward compatible). */
	public String getForeignKeyColumnName() {
		return joinColumns.isEmpty() ? null : joinColumns.get(0).fkColumnName();
	}
	/** Returns the first referenced column name (backward compatible). */
	public String getReferencedColumnName() {
		return joinColumns.isEmpty() ? null : joinColumns.get(0).referencedColumnName();
	}
	/** Returns all join column pairs. */
	public List<JoinColumnPair> getJoinColumns() {
		return Collections.unmodifiableList(joinColumns);
	}
	/** Returns all FK column names. */
	public List<String> getForeignKeyColumnNames() {
		return joinColumns.stream().map(JoinColumnPair::fkColumnName).toList();
	}
	public String getTargetEntityClassName() { return targetEntityClassName; }
	public String getTargetEntityPackage() { return targetEntityPackage; }
	public FetchType getFetchType() { return fetchType; }
	public boolean isOptional() { return optional; }
	public boolean isPartOfCompositeKey() { return partOfCompositeKey; }
}
