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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.NaturalId;
import org.hibernate.tool.reveng.internal.util.TypeHelper;

import org.hibernate.models.spi.FieldDetails;

public class EqualsHashCodeHelper {

	private final TemplateHelper templateHelper;
	private final ImportContext importContext;

	EqualsHashCodeHelper(TemplateHelper templateHelper, ImportContext importContext) {
		this.templateHelper = templateHelper;
		this.importContext = importContext;
	}

	public boolean hasCompositeId() {
		return templateHelper.getCompositeIdField() != null;
	}

	public boolean needsEqualsHashCode() {
		if (templateHelper.isEmbeddable()) return true;
		if (hasNaturalId()) return true;
		if (hasExplicitEqualsColumns()) return true;
		return hasCompositeId() || !getIdentifierFields().isEmpty();
	}

	public boolean hasNaturalId() {
		return templateHelper.getBasicFields().stream()
				.anyMatch(f -> templateHelper.fieldHasAnnotation(f, NaturalId.class));
	}

	public List<FieldDetails> getNaturalIdFields() {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : templateHelper.getBasicFields()) {
			if (templateHelper.fieldHasAnnotation(field, NaturalId.class)) {
				result.add(field);
			}
		}
		return result;
	}

	public boolean hasExplicitEqualsColumns() {
		return getEqualsFields().size() > 0;
	}

	public List<FieldDetails> getEqualsFields() {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : templateHelper.getBasicFields()) {
			if (templateHelper.getFieldMetaAsBool(field, "use-in-equals", false)) {
				result.add(field);
			}
		}
		return result;
	}

	public List<FieldDetails> getIdentifierFields() {
		if (templateHelper.isEmbeddable()) {
			return templateHelper.getBasicFields();
		}
		List<FieldDetails> naturalIdFields = getNaturalIdFields();
		if (!naturalIdFields.isEmpty()) {
			return naturalIdFields;
		}
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : templateHelper.getBasicFields()) {
			if (templateHelper.isPrimaryKey(field)) {
				result.add(field);
			}
		}
		return result;
	}

	public String generateEqualsExpression(FieldDetails field) {
		String getter = templateHelper.getGetterName(field) + "()";
		String typeName = field.getType().determineRawClass().getClassName();
		if (TypeHelper.isPrimitiveType(typeName)) {
			return "this." + getter + " == castOther." + getter;
		}
		return "((this." + getter + " == castOther." + getter + ") || "
				+ "(this." + getter + " != null && castOther." + getter + " != null && "
				+ "this." + getter + ".equals(castOther." + getter + ")))";
	}

	public String generateHashCodeExpression(FieldDetails field) {
		String getter = "this." + templateHelper.getGetterName(field) + "()";
		String typeName = field.getType().determineRawClass().getClassName();
		return switch (typeName) {
			case "int", "char", "short", "byte" -> getter;
			case "boolean" -> "(" + getter + " ? 1 : 0)";
			case "long" -> "(int) " + getter;
			case "float" -> {
				importContext.importType("java.lang.Float");
				yield "Float.floatToIntBits(" + getter + ")";
			}
			case "double" -> {
				importContext.importType("java.lang.Double");
				yield "(int) Double.doubleToLongBits(" + getter + ")";
			}
			default -> "(" + getter + " == null ? 0 : " + getter + ".hashCode())";
		};
	}

}
