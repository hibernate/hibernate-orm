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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents metadata for an embedded field on an entity.
 *
 * @author Koen Aers
 */
public class EmbeddedFieldDescriptor {
	private final String fieldName;
	private final String embeddableClassName;
	private final String embeddablePackage;
	private final List<AttributeOverrideDescriptor> attributeOverrides = new ArrayList<>();

	public EmbeddedFieldDescriptor(
			String fieldName,
			String embeddableClassName,
			String embeddablePackage) {
		this.fieldName = fieldName;
		this.embeddableClassName = embeddableClassName;
		this.embeddablePackage = embeddablePackage;
	}

	public EmbeddedFieldDescriptor addAttributeOverride(String fieldName, String columnName) {
		this.attributeOverrides.add(new AttributeOverrideDescriptor(fieldName, columnName));
		return this;
	}

	// Getters
	public String getFieldName() { return fieldName; }
	public String getEmbeddableClassName() { return embeddableClassName; }
	public String getEmbeddablePackage() { return embeddablePackage; }
	public List<AttributeOverrideDescriptor> getAttributeOverrides() { return attributeOverrides; }
}
