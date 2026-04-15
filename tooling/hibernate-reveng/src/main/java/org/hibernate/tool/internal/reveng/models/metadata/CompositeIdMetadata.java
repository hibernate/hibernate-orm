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
	private final List<String> originalColumnOrder = new ArrayList<>();

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
		this.originalColumnOrder.add(columnName);
		return this;
	}

	public CompositeIdMetadata addAttributeOverride(String fieldName, String columnName, Class<?> javaType) {
		this.attributeOverrides.add(new AttributeOverrideMetadata(fieldName, columnName, javaType));
		this.originalColumnOrder.add(columnName);
		return this;
	}

	public CompositeIdMetadata addKeyManyToOne(
			String fieldName, String columnName,
			String targetEntityClassName, String targetEntityPackage) {
		this.keyManyToOnes.add(new KeyManyToOneMetadata(
				fieldName, columnName, targetEntityClassName, targetEntityPackage));
		return this;
	}

	public CompositeIdMetadata addKeyManyToOne(
			String fieldName, List<String> columnNames,
			String targetEntityClassName, String targetEntityPackage) {
		this.keyManyToOnes.add(new KeyManyToOneMetadata(
				fieldName, columnNames, targetEntityClassName, targetEntityPackage));
		return this;
	}

	// Getters
	public String getFieldName() { return fieldName; }
	public String getIdClassName() { return idClassName; }
	public String getIdClassPackage() { return idClassPackage; }
	public List<AttributeOverrideMetadata> getAttributeOverrides() { return attributeOverrides; }
	public List<KeyManyToOneMetadata> getKeyManyToOnes() { return keyManyToOnes; }
	public List<String> getOriginalColumnOrder() { return originalColumnOrder; }

	/**
	 * Returns an ordered list of entries (attribute overrides and key-many-to-ones)
	 * matching the original PK column order. Each entry is either an
	 * {@link AttributeOverrideMetadata} or a {@link KeyManyToOneMetadata}.
	 * Key-many-to-ones appear at the position of their first FK column.
	 */
	public List<Object> getOrderedEntries() {
		// If no original column order was recorded (e.g., metadata built directly
		// without addAttributeOverride), fall back to attribute overrides then
		// key-many-to-ones in their list order.
		if (originalColumnOrder.isEmpty()) {
			List<Object> result = new ArrayList<>();
			result.addAll(attributeOverrides);
			result.addAll(keyManyToOnes);
			return result;
		}
		List<Object> result = new ArrayList<>();
		java.util.Set<String> processedKeyManyToOnes = new java.util.HashSet<>();
		for (String columnName : originalColumnOrder) {
			// Check if this column belongs to a key-many-to-one
			KeyManyToOneMetadata matchedKm2o = null;
			for (KeyManyToOneMetadata km2o : keyManyToOnes) {
				if (km2o.getColumnNames().contains(columnName)) {
					matchedKm2o = km2o;
					break;
				}
			}
			if (matchedKm2o != null) {
				// Only add the key-many-to-one once (at its first column position)
				if (processedKeyManyToOnes.add(matchedKm2o.getFieldName())) {
					result.add(matchedKm2o);
				}
			} else {
				// Check if it's still in the attribute overrides (not removed)
				for (AttributeOverrideMetadata ao : attributeOverrides) {
					if (ao.getColumnName().equals(columnName)) {
						result.add(ao);
						break;
					}
				}
			}
		}
		// Append any key-many-to-ones not in the original column order
		// (e.g., added directly without corresponding attribute overrides)
		for (KeyManyToOneMetadata km2o : keyManyToOnes) {
			if (!processedKeyManyToOnes.contains(km2o.getFieldName())) {
				result.add(km2o);
			}
		}
		return result;
	}
}
