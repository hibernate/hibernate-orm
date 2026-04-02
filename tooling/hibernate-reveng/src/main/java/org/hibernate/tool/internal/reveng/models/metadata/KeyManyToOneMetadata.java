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

/**
 * Represents metadata for a many-to-one relationship within a composite ID.
 * These become {@code <key-many-to-one>} elements in HBM XML.
 *
 * @author Koen Aers
 */
public class KeyManyToOneMetadata {
	private final String fieldName;
	private final String columnName;
	private final String targetEntityClassName;
	private final String targetEntityPackage;

	public KeyManyToOneMetadata(
			String fieldName,
			String columnName,
			String targetEntityClassName,
			String targetEntityPackage) {
		this.fieldName = fieldName;
		this.columnName = columnName;
		this.targetEntityClassName = targetEntityClassName;
		this.targetEntityPackage = targetEntityPackage;
	}

	// Getters
	public String getFieldName() { return fieldName; }
	public String getColumnName() { return columnName; }
	public String getTargetEntityClassName() { return targetEntityClassName; }
	public String getTargetEntityPackage() { return targetEntityPackage; }
}
