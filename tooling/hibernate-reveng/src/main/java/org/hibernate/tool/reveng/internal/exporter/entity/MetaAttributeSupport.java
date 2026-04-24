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
package org.hibernate.tool.reveng.internal.exporter.entity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.models.spi.FieldDetails;

public class MetaAttributeSupport {

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

	public boolean hasClassMetaAttribute(String name) {
		return classMetaAttributes.containsKey(name);
	}

	public String getClassMetaAttribute(String name) {
		List<String> values = classMetaAttributes.getOrDefault(
				name, Collections.emptyList());
		return values.isEmpty() ? "" : String.join("\n", values);
	}

	public List<String> getClassMetaAttributeValues(String name) {
		return classMetaAttributes.getOrDefault(name, Collections.emptyList());
	}

	public boolean hasFieldMetaAttribute(FieldDetails field, String name) {
		Map<String, List<String>> attrs = fieldMetaAttributes.getOrDefault(
				field.getName(), Collections.emptyMap());
		return attrs.containsKey(name);
	}

	public boolean getFieldMetaAsBool(
			FieldDetails field, String name, boolean defaultValue) {
		Map<String, List<String>> attrs = fieldMetaAttributes.getOrDefault(
				field.getName(), Collections.emptyMap());
		List<String> values = attrs.getOrDefault(name, Collections.emptyList());
		if (values.isEmpty()) {
			return defaultValue;
		}
		return Boolean.parseBoolean(values.get(0));
	}

	public String getFieldMetaAttribute(FieldDetails field, String name) {
		Map<String, List<String>> attrs = fieldMetaAttributes.getOrDefault(
				field.getName(), Collections.emptyMap());
		List<String> values = attrs.getOrDefault(name, Collections.emptyList());
		return values.isEmpty() ? "" : String.join("\n", values);
	}

	// --- Field modifiers ---

	public String getFieldModifiers(FieldDetails field) {
		if (hasFieldMetaAttribute(field, "scope-field")) {
			return getFieldMetaAttribute(field, "scope-field");
		}
		return "private";
	}

	public String getPropertyGetModifiers(FieldDetails field) {
		if (hasFieldMetaAttribute(field, "scope-get")) {
			return getFieldMetaAttribute(field, "scope-get");
		}
		return "public";
	}

	public String getPropertySetModifiers(FieldDetails field) {
		if (hasFieldMetaAttribute(field, "scope-set")) {
			return getFieldMetaAttribute(field, "scope-set");
		}
		return "public";
	}

	// --- Class-level convenience ---

	public boolean hasClassDescription() {
		return hasClassMetaAttribute("class-description");
	}

	public String getClassDescription() {
		return getClassMetaAttribute("class-description");
	}

	public boolean hasExtraClassCode() {
		return hasClassMetaAttribute("class-code");
	}

	public String getExtraClassCode() {
		return getClassMetaAttribute("class-code");
	}

	// --- Field-level convenience ---

	public boolean isGenProperty(FieldDetails field) {
		return getFieldMetaAsBool(field, "gen-property", true);
	}

	public boolean hasFieldDescription(FieldDetails field) {
		return hasFieldMetaAttribute(field, "field-description");
	}

	public String getFieldDescription(FieldDetails field) {
		return getFieldMetaAttribute(field, "field-description");
	}

	public boolean hasFieldDefaultValue(FieldDetails field) {
		return hasFieldMetaAttribute(field, "default-value");
	}

	public String getFieldDefaultValue(FieldDetails field) {
		return getFieldMetaAttribute(field, "default-value");
	}
}
