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
package org.hibernate.tool.internal.exporter.entity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.models.spi.FieldDetails;

class MetaAttributeSupport {

	private final Map<String, List<String>> classMetaAttributes;
	private final Map<String, Map<String, List<String>>> fieldMetaAttributes;

	MetaAttributeSupport(
			Map<String, List<String>> classMetaAttributes,
			Map<String, Map<String, List<String>>> fieldMetaAttributes) {
		this.classMetaAttributes = classMetaAttributes != null
				? classMetaAttributes : Collections.emptyMap();
		this.fieldMetaAttributes = fieldMetaAttributes != null
				? fieldMetaAttributes : Collections.emptyMap();
	}

	// --- Core lookups ---

	boolean hasClassMetaAttribute(String name) {
		return classMetaAttributes.containsKey(name);
	}

	String getClassMetaAttribute(String name) {
		List<String> values = classMetaAttributes.getOrDefault(
				name, Collections.emptyList());
		return values.isEmpty() ? "" : String.join("\n", values);
	}

	List<String> getClassMetaAttributeValues(String name) {
		return classMetaAttributes.getOrDefault(name, Collections.emptyList());
	}

	boolean hasFieldMetaAttribute(FieldDetails field, String name) {
		Map<String, List<String>> attrs = fieldMetaAttributes.getOrDefault(
				field.getName(), Collections.emptyMap());
		return attrs.containsKey(name);
	}

	boolean getFieldMetaAsBool(
			FieldDetails field, String name, boolean defaultValue) {
		Map<String, List<String>> attrs = fieldMetaAttributes.getOrDefault(
				field.getName(), Collections.emptyMap());
		List<String> values = attrs.getOrDefault(name, Collections.emptyList());
		if (values.isEmpty()) {
			return defaultValue;
		}
		return Boolean.parseBoolean(values.get(0));
	}

	String getFieldMetaAttribute(FieldDetails field, String name) {
		Map<String, List<String>> attrs = fieldMetaAttributes.getOrDefault(
				field.getName(), Collections.emptyMap());
		List<String> values = attrs.getOrDefault(name, Collections.emptyList());
		return values.isEmpty() ? "" : String.join("\n", values);
	}

	// --- Field modifiers ---

	String getFieldModifiers(FieldDetails field) {
		if (hasFieldMetaAttribute(field, "scope-field")) {
			return getFieldMetaAttribute(field, "scope-field");
		}
		return "private";
	}

	String getPropertyGetModifiers(FieldDetails field) {
		if (hasFieldMetaAttribute(field, "scope-get")) {
			return getFieldMetaAttribute(field, "scope-get");
		}
		return "public";
	}

	String getPropertySetModifiers(FieldDetails field) {
		if (hasFieldMetaAttribute(field, "scope-set")) {
			return getFieldMetaAttribute(field, "scope-set");
		}
		return "public";
	}

	// --- Class-level convenience ---

	boolean hasClassDescription() {
		return hasClassMetaAttribute("class-description");
	}

	String getClassDescription() {
		return getClassMetaAttribute("class-description");
	}

	boolean hasExtraClassCode() {
		return hasClassMetaAttribute("class-code");
	}

	String getExtraClassCode() {
		return getClassMetaAttribute("class-code");
	}

	// --- Field-level convenience ---

	boolean isGenProperty(FieldDetails field) {
		return getFieldMetaAsBool(field, "gen-property", true);
	}

	boolean hasFieldDescription(FieldDetails field) {
		return hasFieldMetaAttribute(field, "field-description");
	}

	String getFieldDescription(FieldDetails field) {
		return getFieldMetaAttribute(field, "field-description");
	}

	boolean hasFieldDefaultValue(FieldDetails field) {
		return hasFieldMetaAttribute(field, "default-value");
	}

	String getFieldDefaultValue(FieldDetails field) {
		return getFieldMetaAttribute(field, "default-value");
	}
}
