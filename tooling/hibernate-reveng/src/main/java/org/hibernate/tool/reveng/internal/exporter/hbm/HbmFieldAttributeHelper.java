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
package org.hibernate.tool.reveng.internal.exporter.hbm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Access;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import org.hibernate.tool.reveng.internal.util.TypeHelper;

import org.hibernate.models.spi.FieldDetails;

/**
 * Handles per-field HBM attributes for template generation: column,
 * type, generator, access, formula, insert/update, optimistic-lock.
 *
 * @author Koen Aers
 */
public class HbmFieldAttributeHelper {

	private final Map<String, Map<String, List<String>>> fieldMetaAttributes;

	HbmFieldAttributeHelper(Map<String, Map<String, List<String>>> fieldMetaAttributes) {
		this.fieldMetaAttributes = fieldMetaAttributes;
	}

	private Map<String, List<String>> getFieldMetaAttributeMap(FieldDetails field) {
		return FieldMetaUtil.forField(fieldMetaAttributes, field);
	}

	// --- Property-level attributes ---

	public String getFormula(FieldDetails field) {
		Formula formula = field.getDirectAnnotationUsage(Formula.class);
		return formula != null ? formula.value() : null;
	}

	public String getAccessType(FieldDetails field) {
		Access access = field.getDirectAnnotationUsage(Access.class);
		if (access == null) {
			return null;
		}
		return access.value().name().toLowerCase();
	}

	public String getFetchMode(FieldDetails field) {
		Fetch fetch = field.getDirectAnnotationUsage(Fetch.class);
		if (fetch == null) {
			return null;
		}
		return switch (fetch.value()) {
			case JOIN -> "join";
			case SELECT -> "select";
			case SUBSELECT -> "subselect";
		};
	}

	public String getNotFoundAction(FieldDetails field) {
		NotFound nf = field.getDirectAnnotationUsage(NotFound.class);
		if (nf == null || nf.action() == NotFoundAction.EXCEPTION) {
			return null;
		}
		return "ignore";
	}

	public boolean isTimestamp(FieldDetails field) {
		String className = field.getType().determineRawClass().getClassName();
		return "java.util.Date".equals(className)
				|| "java.sql.Timestamp".equals(className)
				|| "java.util.Calendar".equals(className)
				|| "java.time.Instant".equals(className)
				|| "java.time.LocalDateTime".equals(className);
	}

	public boolean isPropertyUpdatable(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col == null || col.updatable();
	}

	public boolean isPropertyInsertable(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col == null || col.insertable();
	}

	public boolean isPropertyLazy(FieldDetails field) {
		Basic basic = field.getDirectAnnotationUsage(Basic.class);
		return basic != null && basic.fetch() == FetchType.LAZY;
	}

	public boolean isOptimisticLockExcluded(FieldDetails field) {
		OptimisticLock ol = field.getDirectAnnotationUsage(OptimisticLock.class);
		return ol != null && ol.excluded();
	}

	// --- Generator ---

	public Map<String, String> getGeneratorParameters(FieldDetails field) {
		Map<String, String> params = new LinkedHashMap<>();
		SequenceGenerator sg = field.getDirectAnnotationUsage(SequenceGenerator.class);
		if (sg != null) {
			if (sg.sequenceName() != null && !sg.sequenceName().isEmpty()) {
				params.put("sequence", sg.sequenceName());
			}
			if (sg.allocationSize() != 50) {
				params.put("increment_size", String.valueOf(sg.allocationSize()));
			}
			if (sg.initialValue() != 1) {
				params.put("initial_value", String.valueOf(sg.initialValue()));
			}
			return params;
		}
		TableGenerator tg = field.getDirectAnnotationUsage(TableGenerator.class);
		if (tg != null) {
			if (tg.table() != null && !tg.table().isEmpty()) {
				params.put("table", tg.table());
			}
			if (tg.pkColumnName() != null && !tg.pkColumnName().isEmpty()) {
				params.put("segment_column_name", tg.pkColumnName());
			}
			if (tg.valueColumnName() != null && !tg.valueColumnName().isEmpty()) {
				params.put("value_column_name", tg.valueColumnName());
			}
			if (tg.pkColumnValue() != null && !tg.pkColumnValue().isEmpty()) {
				params.put("segment_value", tg.pkColumnValue());
			}
		}
		if (params.isEmpty()) {
			params = getGeneratorParametersFromMeta(field);
		}
		return params;
	}

	public String getGeneratorClass(FieldDetails field) {
		Map<String, List<String>> fieldMeta = getFieldMetaAttributeMap(field);
		List<String> genClassMeta = fieldMeta.get("hibernate.generator.class");
		if (genClassMeta != null && !genClassMeta.isEmpty()) {
			return genClassMeta.get(0);
		}
		GeneratedValue gv = field.getDirectAnnotationUsage(GeneratedValue.class);
		return toGeneratorClass(gv != null ? gv.strategy() : null);
	}

	public String toGeneratorClass(GenerationType generationType) {
		if (generationType == null) {
			return "assigned";
		}
		return switch (generationType) {
			case IDENTITY -> "identity";
			case SEQUENCE -> "sequence";
			case TABLE -> "table";
			case AUTO -> "native";
			case UUID -> "uuid2";
		};
	}

	// --- Column / type attributes ---

	public String getColumnName(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null ? col.name() : field.getName();
	}

	public String getHibernateTypeName(FieldDetails field) {
		Type typeAnn = field.getDirectAnnotationUsage(Type.class);
		if (typeAnn != null) {
			return typeAnn.value().getName();
		}
		Map<String, List<String>> fieldMeta = getFieldMetaAttributeMap(field);
		List<String> typeName = fieldMeta.get("hibernate.type.name");
		if (typeName != null && !typeName.isEmpty()) {
			return typeName.get(0);
		}
		String className = field.getType().determineRawClass().getClassName();
		return TypeHelper.toHibernateType(className);
	}

	public boolean hasTypeParameters(FieldDetails field) {
		Type typeAnn = field.getDirectAnnotationUsage(Type.class);
		if (typeAnn != null && typeAnn.parameters() != null && typeAnn.parameters().length > 0) {
			return true;
		}
		return !getTypeParametersFromMeta(field).isEmpty();
	}

	public Map<String, String> getTypeParameters(FieldDetails field) {
		Type typeAnn = field.getDirectAnnotationUsage(Type.class);
		if (typeAnn != null && typeAnn.parameters() != null && typeAnn.parameters().length > 0) {
			Map<String, String> params = new LinkedHashMap<>();
			for (Parameter param : typeAnn.parameters()) {
				params.put(param.name(), param.value());
			}
			return params;
		}
		return getTypeParametersFromMeta(field);
	}

	public String getColumnAttributes(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		StringBuilder sb = new StringBuilder();
		if (col != null && !col.nullable()) {
			sb.append("not-null=\"true\" ");
		}
		if (col != null && col.unique()) {
			sb.append("unique=\"true\" ");
		}
		if (col != null && col.length() != 255 && col.length() > 0) {
			sb.append("length=\"").append(col.length()).append("\" ");
		}
		if (col != null && col.precision() > 0) {
			sb.append("precision=\"").append(col.precision()).append("\" ");
		}
		if (col != null && col.scale() > 0) {
			sb.append("scale=\"").append(col.scale()).append("\" ");
		}
		return sb.toString().stripTrailing();
	}

	public String getColumnComment(FieldDetails field) {
		Comment comment = field.getDirectAnnotationUsage(Comment.class);
		return comment != null && !comment.value().isEmpty() ? comment.value() : null;
	}

	// --- Private helpers ---

	private Map<String, String> getTypeParametersFromMeta(FieldDetails field) {
		Map<String, List<String>> fieldMeta = getFieldMetaAttributeMap(field);
		Map<String, String> params = new LinkedHashMap<>();
		String prefix = "hibernate.type.param:";
		for (Map.Entry<String, List<String>> entry : fieldMeta.entrySet()) {
			if (entry.getKey().startsWith(prefix)) {
				String paramName = entry.getKey().substring(prefix.length());
				String paramValue = entry.getValue().isEmpty() ? "" : entry.getValue().get(0);
				params.put(paramName, paramValue);
			}
		}
		return params;
	}

	private Map<String, String> getGeneratorParametersFromMeta(FieldDetails field) {
		Map<String, List<String>> fieldMeta = getFieldMetaAttributeMap(field);
		Map<String, String> params = new LinkedHashMap<>();
		String prefix = "hibernate.generator.param:";
		for (Map.Entry<String, List<String>> entry : fieldMeta.entrySet()) {
			if (entry.getKey().startsWith(prefix)) {
				String paramName = entry.getKey().substring(prefix.length());
				String paramValue = entry.getValue().isEmpty() ? "" : entry.getValue().get(0);
				params.put(paramName, paramValue);
			}
		}
		return params;
	}

	// --- Field meta attributes (for template access) ---

	public Map<String, List<String>> getFieldMetaAttributes(FieldDetails field) {
		Map<String, List<String>> all = fieldMetaAttributes.getOrDefault(
				field.getName(), Collections.emptyMap());
		Map<String, List<String>> result = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : all.entrySet()) {
			if (!entry.getKey().startsWith("hibernate.type.")
					&& !entry.getKey().startsWith("hibernate.generator.")
					&& !entry.getKey().startsWith("hibernate.any.")
					&& !entry.getKey().startsWith("hibernate.collection.")
					&& !entry.getKey().startsWith("hibernate.array.")
					&& !entry.getKey().startsWith("hibernate.dynamic-component")
					&& !entry.getKey().equals("hibernate.cascade")
					&& !entry.getKey().equals("hibernate.formula")
					&& !entry.getKey().equals("hibernate.properties-group")) {
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return result;
	}

	public List<String> getFieldMetaAttribute(FieldDetails field, String name) {
		Map<String, List<String>> attrs = fieldMetaAttributes.getOrDefault(
				field.getName(), Collections.emptyMap());
		return attrs.getOrDefault(name, Collections.emptyList());
	}

	// --- Target entity / class ---

	public String getTargetEntityName(FieldDetails field) {
		return field.getType().determineRawClass().getClassName();
	}

	public String getEmbeddableClassName(FieldDetails field) {
		return field.getType().determineRawClass().getClassName();
	}
}
