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

import jakarta.persistence.FetchType;

/**
 * Represents metadata for a database foreign key relationship.
 *
 * @author Koen Aers
 */
public class ForeignKeyMetadata {
	private final String fieldName;
	private final String foreignKeyColumnName;
	private String referencedColumnName;
	private final String targetEntityClassName;
	private final String targetEntityPackage;
	private FetchType fetchType;
	private boolean optional;

	public ForeignKeyMetadata(
			String fieldName,
			String foreignKeyColumnName,
			String targetEntityClassName,
			String targetEntityPackage) {
		this.fieldName = fieldName;
		this.foreignKeyColumnName = foreignKeyColumnName;
		this.targetEntityClassName = targetEntityClassName;
		this.targetEntityPackage = targetEntityPackage;
		this.optional = true;
	}

	public ForeignKeyMetadata referencedColumnName(String referencedColumnName) {
		this.referencedColumnName = referencedColumnName;
		return this;
	}

	public ForeignKeyMetadata fetchType(FetchType fetchType) {
		this.fetchType = fetchType;
		return this;
	}

	public ForeignKeyMetadata optional(boolean optional) {
		this.optional = optional;
		return this;
	}

	// Getters
	public String getFieldName() { return fieldName; }
	public String getForeignKeyColumnName() { return foreignKeyColumnName; }
	public String getReferencedColumnName() { return referencedColumnName; }
	public String getTargetEntityClassName() { return targetEntityClassName; }
	public String getTargetEntityPackage() { return targetEntityPackage; }
	public FetchType getFetchType() { return fetchType; }
	public boolean isOptional() { return optional; }
}
