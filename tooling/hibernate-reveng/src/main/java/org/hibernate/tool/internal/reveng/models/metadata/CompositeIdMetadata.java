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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents metadata for an @EmbeddedId composite primary key.
 * The ID class should be created separately via createEmbeddable().
 *
 * @author Koen Aers
 */
public class CompositeIdMetadata {
	private final String fieldName;
	private final String idClassName;
	private final String idClassPackage;
	private final List<AttributeOverrideMetadata> attributeOverrides = new ArrayList<>();
	private final List<KeyManyToOneMetadata> keyManyToOnes = new ArrayList<>();

	public CompositeIdMetadata(
			String fieldName,
			String idClassName,
			String idClassPackage) {
		this.fieldName = fieldName;
		this.idClassName = idClassName;
		this.idClassPackage = idClassPackage;
	}

	public CompositeIdMetadata addAttributeOverride(String fieldName, String columnName) {
		this.attributeOverrides.add(new AttributeOverrideMetadata(fieldName, columnName));
		return this;
	}

	public CompositeIdMetadata addKeyManyToOne(
			String fieldName, String columnName,
			String targetEntityClassName, String targetEntityPackage) {
		this.keyManyToOnes.add(new KeyManyToOneMetadata(
				fieldName, columnName, targetEntityClassName, targetEntityPackage));
		return this;
	}

	// Getters
	public String getFieldName() { return fieldName; }
	public String getIdClassName() { return idClassName; }
	public String getIdClassPackage() { return idClassPackage; }
	public List<AttributeOverrideMetadata> getAttributeOverrides() { return attributeOverrides; }
	public List<KeyManyToOneMetadata> getKeyManyToOnes() { return keyManyToOnes; }
}
