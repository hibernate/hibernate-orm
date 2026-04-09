/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.builder.hbm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.MutableClassDetailsRegistry;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassLoading;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.GenerationType;

/**
 * Shared context for hbm.xml → ClassDetails building.
 * Holds the {@link ModelsContext}, type resolution logic,
 * class name helpers, and registry operations used by all
 * Hbm*Builder classes.
 *
 * @author Koen Aers
 */
public class HbmBuildContext {

	private static final Map<String, String> HIBERNATE_TYPE_MAP = new HashMap<>();

	static {
		HIBERNATE_TYPE_MAP.put("string", "java.lang.String");
		HIBERNATE_TYPE_MAP.put("long", "java.lang.Long");
		HIBERNATE_TYPE_MAP.put("int", "java.lang.Integer");
		HIBERNATE_TYPE_MAP.put("integer", "java.lang.Integer");
		HIBERNATE_TYPE_MAP.put("short", "java.lang.Short");
		HIBERNATE_TYPE_MAP.put("byte", "java.lang.Byte");
		HIBERNATE_TYPE_MAP.put("float", "java.lang.Float");
		HIBERNATE_TYPE_MAP.put("double", "java.lang.Double");
		HIBERNATE_TYPE_MAP.put("boolean", "java.lang.Boolean");
		HIBERNATE_TYPE_MAP.put("yes_no", "java.lang.Boolean");
		HIBERNATE_TYPE_MAP.put("true_false", "java.lang.Boolean");
		HIBERNATE_TYPE_MAP.put("big_decimal", "java.math.BigDecimal");
		HIBERNATE_TYPE_MAP.put("big_integer", "java.math.BigInteger");
		HIBERNATE_TYPE_MAP.put("character", "java.lang.Character");
		HIBERNATE_TYPE_MAP.put("char", "java.lang.Character");
		HIBERNATE_TYPE_MAP.put("date", "java.util.Date");
		HIBERNATE_TYPE_MAP.put("time", "java.util.Date");
		HIBERNATE_TYPE_MAP.put("timestamp", "java.util.Date");
		HIBERNATE_TYPE_MAP.put("calendar", "java.util.Calendar");
		HIBERNATE_TYPE_MAP.put("calendar_date", "java.util.Calendar");
		HIBERNATE_TYPE_MAP.put("binary", "byte[]");
		HIBERNATE_TYPE_MAP.put("text", "java.lang.String");
		HIBERNATE_TYPE_MAP.put("clob", "java.sql.Clob");
		HIBERNATE_TYPE_MAP.put("blob", "java.sql.Blob");
		HIBERNATE_TYPE_MAP.put("serializable", "java.io.Serializable");
	}

	private final ModelsContext modelsContext;
	private String defaultPackage;

	public HbmBuildContext() {
		ClassLoading classLoading = SimpleClassLoading.SIMPLE_CLASS_LOADING;
		this.modelsContext = new BasicModelsContextImpl(classLoading, false, null);
	}

	public ModelsContext getModelsContext() {
		return modelsContext;
	}

	public String getDefaultPackage() {
		return defaultPackage;
	}

	public void setDefaultPackage(String defaultPackage) {
		this.defaultPackage = defaultPackage;
	}

	// --- Type resolution ---

	public String resolveJavaType(String hibernateType) {
		if (hibernateType == null || hibernateType.isEmpty()) {
			return "java.lang.String";
		}
		String mapped = HIBERNATE_TYPE_MAP.get(hibernateType.toLowerCase());
		if (mapped != null) {
			return mapped;
		}
		if (hibernateType.contains(".")) {
			return hibernateType;
		}
		return "java.lang.String";
	}

	public GenerationType mapGeneratorClass(String generatorClass) {
		if (generatorClass == null || generatorClass.isEmpty()) {
			return null;
		}
		return switch (generatorClass) {
			case "identity", "native" -> GenerationType.IDENTITY;
			case "sequence", "seqhilo",
				 "enhanced-sequence", "org.hibernate.id.enhanced.SequenceStyleGenerator"
					-> GenerationType.SEQUENCE;
			case "enhanced-table", "org.hibernate.id.enhanced.TableGenerator"
					-> GenerationType.TABLE;
			case "uuid", "uuid2", "guid" -> GenerationType.UUID;
			case "assigned" -> null;
			default -> GenerationType.AUTO;
		};
	}

	// --- Class name resolution ---

	public String resolveClassName(String name) {
		return resolveClassName(name, defaultPackage);
	}

	public static String resolveClassName(String name, String defaultPackage) {
		if (name == null || name.isEmpty()) {
			return name;
		}
		if (name.contains(".")) {
			return name;
		}
		if (defaultPackage != null && !defaultPackage.isEmpty()) {
			return defaultPackage + "." + name;
		}
		return name;
	}

	public static String simpleName(String fullName) {
		int lastDot = fullName.lastIndexOf('.');
		return lastDot > 0 ? fullName.substring(lastDot + 1) : fullName;
	}

	// --- ClassDetails registry helpers ---

	public DynamicFieldDetails createField(DynamicClassDetails entityClass,
										   String fieldName, String javaType) {
		ClassDetails fieldTypeClass = modelsContext.getClassDetailsRegistry()
				.resolveClassDetails(javaType);
		TypeDetails fieldType = new ClassTypeDetailsImpl(
				fieldTypeClass, TypeDetails.Kind.CLASS);
		return entityClass.applyAttribute(
				fieldName, fieldType, false, false, modelsContext);
	}

	public DynamicFieldDetails createCollectionField(DynamicClassDetails entityClass,
													 String fieldName,
													 ClassDetails elementClass) {
		TypeDetails fieldType = new ClassTypeDetailsImpl(
				elementClass, TypeDetails.Kind.CLASS);
		return entityClass.applyAttribute(
				fieldName, fieldType, false, true, modelsContext);
	}

	public ClassDetails resolveOrCreateClassDetails(String simpleName, String className) {
		MutableClassDetailsRegistry registry =
				(MutableClassDetailsRegistry) modelsContext.getClassDetailsRegistry();
		return registry.resolveClassDetails(
				className,
				name -> new DynamicClassDetails(
						simpleName, name, Object.class,
						false, null, null, modelsContext));
	}

	public void registerClassDetails(ClassDetails classDetails) {
		MutableClassDetailsRegistry registry =
				(MutableClassDetailsRegistry) modelsContext.getClassDetailsRegistry();
		registry.addClassDetails(classDetails);
	}

	// --- Column annotation helpers ---

	public void addColumnAnnotation(DynamicFieldDetails field,
									List<JaxbHbmColumnType> columns,
									String columnAttribute,
									String defaultColumnName) {
		String columnName = columnAttribute;
		boolean notNull = false;
		int length = 0;
		int precision = 0;
		int scale = 0;
		boolean unique = false;

		if (columns != null && !columns.isEmpty()) {
			JaxbHbmColumnType col = columns.get(0);
			if (col.getName() != null) {
				columnName = col.getName();
			}
			if (Boolean.TRUE.equals(col.isNotNull())) {
				notNull = true;
			}
			if (col.getLength() != null) {
				length = col.getLength();
			}
			if (col.getPrecision() != null) {
				precision = col.getPrecision();
			}
			if (col.getScale() != null) {
				scale = col.getScale();
			}
			if (Boolean.TRUE.equals(col.isUnique())) {
				unique = true;
			}
		}

		if (columnName == null || columnName.isEmpty()) {
			columnName = defaultColumnName;
		}

		applyColumnAnnotation(field, columnName, !notNull, length, precision, scale, unique);
	}

	public void addColumnAnnotationFromBasicAttr(
			DynamicFieldDetails field,
			String columnAttribute,
			Boolean isNotNull,
			Integer attrLength,
			String precisionStr,
			String scaleStr,
			boolean isUnique,
			List<? extends Serializable> columnOrFormula,
			String defaultColumnName) {

		String columnName = columnAttribute;
		boolean notNull = Boolean.TRUE.equals(isNotNull);
		int length = attrLength != null ? attrLength : 0;
		int precision = precisionStr != null && !precisionStr.isEmpty()
				? Integer.parseInt(precisionStr) : 0;
		int scale = scaleStr != null && !scaleStr.isEmpty()
				? Integer.parseInt(scaleStr) : 0;
		boolean unique = isUnique;

		if (columnOrFormula != null) {
			for (Serializable colOrFormula : columnOrFormula) {
				if (colOrFormula instanceof JaxbHbmColumnType col) {
					if (col.getName() != null) {
						columnName = col.getName();
					}
					if (Boolean.TRUE.equals(col.isNotNull())) {
						notNull = true;
					}
					if (col.getLength() != null) {
						length = col.getLength();
					}
					if (col.getPrecision() != null) {
						precision = col.getPrecision();
					}
					if (col.getScale() != null) {
						scale = col.getScale();
					}
					if (Boolean.TRUE.equals(col.isUnique())) {
						unique = true;
					}
					break;
				}
			}
		}

		if (columnName == null || columnName.isEmpty()) {
			columnName = defaultColumnName;
		}

		applyColumnAnnotation(field, columnName, !notNull, length, precision, scale, unique);
	}

	private void applyColumnAnnotation(DynamicFieldDetails field, String columnName,
									   boolean nullable, int length, int precision,
									   int scale, boolean unique) {
		ColumnJpaAnnotation columnAnnotation = JpaAnnotations.COLUMN.createUsage(modelsContext);
		columnAnnotation.name(columnName);
		columnAnnotation.nullable(nullable);
		if (length > 0) {
			columnAnnotation.length(length);
		}
		if (precision > 0) {
			columnAnnotation.precision(precision);
		}
		if (scale > 0) {
			columnAnnotation.scale(scale);
		}
		if (unique) {
			columnAnnotation.unique(true);
		}
		field.addAnnotationUsage(columnAnnotation);
	}
}
