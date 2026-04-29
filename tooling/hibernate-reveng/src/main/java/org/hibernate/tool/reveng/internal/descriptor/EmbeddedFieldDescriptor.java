/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

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
