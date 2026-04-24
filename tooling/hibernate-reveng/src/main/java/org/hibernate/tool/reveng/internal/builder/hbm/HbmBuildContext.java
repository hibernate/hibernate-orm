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
package org.hibernate.tool.reveng.internal.builder.hbm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmToolingHintType;
import org.hibernate.boot.jaxb.hbm.spi.ToolingHintContainer;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.CommentAnnotation;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.PrimitiveTypeDetailsImpl;
import org.hibernate.models.internal.ParameterizedTypeDetailsImpl;
import org.hibernate.models.internal.MutableClassDetailsRegistry;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassLoading;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.GenerationType;

import static org.hibernate.tool.reveng.internal.builder.hbm.HbmTypeResolver.isPrimitiveType;

/**
 * Shared context for hbm.xml → ClassDetails building.
 * Holds the {@link ModelsContext}, type resolution logic,
 * class name helpers, and registry operations used by all
 * Hbm*Builder classes.
 *
 * @author Koen Aers
 */
public class HbmBuildContext {

	private final ModelsContext modelsContext;
	private final List<ClassDetails> embeddableClassDetails = new ArrayList<>();
	private final List<ClassDetails> subclassEntityDetails = new ArrayList<>();
	private String defaultPackage;

	// Meta attributes from hbm.xml <meta> elements, keyed by fully qualified class name
	private final Map<String, Map<String, List<String>>> classMetaAttributes = new HashMap<>();
	private final Map<String, Map<String, Map<String, List<String>>>> fieldMetaAttributes = new HashMap<>();
	// Tracks meta attributes marked inherit="false", keyed by class name + meta name
	private final Set<String> nonInheritableClassMetas = new HashSet<>();

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

	// --- Type resolution (delegates to HbmTypeResolver) ---

	public String resolveJavaType(String hibernateType) {
		return HbmTypeResolver.resolveJavaType(hibernateType);
	}

	public GenerationType mapGeneratorClass(String generatorClass) {
		return HbmTypeResolver.mapGeneratorClass(generatorClass);
	}

	public String resolveClassName(String name) {
		return HbmTypeResolver.resolveClassName(name, defaultPackage);
	}

	// --- ClassDetails registry helpers ---

	public DynamicFieldDetails createField(DynamicClassDetails entityClass,
										   String fieldName, String javaType) {
		// For array types like "byte[]", resolve the component type and
		// use an ArrayTypeDetailsImpl to represent the array.
		if (javaType.endsWith("[]")) {
			String componentType = javaType.substring(0, javaType.length() - 2);
			ClassDetails componentClass = modelsContext.getClassDetailsRegistry()
					.resolveClassDetails(componentType);
			TypeDetails componentTypeDetails = new ClassTypeDetailsImpl(
					componentClass, TypeDetails.Kind.CLASS);
			// Create/resolve a DynamicClassDetails for the array type name
			ClassDetails arrayClassDetails = resolveOrCreateClassDetails(
					javaType, javaType);
			TypeDetails arrayType = new org.hibernate.models.internal.ArrayTypeDetailsImpl(
					arrayClassDetails, componentTypeDetails);
			return entityClass.applyAttribute(
					fieldName, arrayType, false, false, modelsContext);
		}
		ClassDetails fieldTypeClass = modelsContext.getClassDetailsRegistry()
				.resolveClassDetails(javaType);
		TypeDetails fieldType;
		if (isPrimitiveType(javaType)) {
			fieldType = new PrimitiveTypeDetailsImpl(fieldTypeClass);
		} else {
			fieldType = new ClassTypeDetailsImpl(
					fieldTypeClass, TypeDetails.Kind.CLASS);
		}
		return entityClass.applyAttribute(
				fieldName, fieldType, false, false, modelsContext);
	}

	public DynamicFieldDetails createCollectionField(DynamicClassDetails entityClass,
													 String fieldName,
													 ClassDetails elementClass) {
		return createCollectionField(entityClass, fieldName, elementClass, "java.util.Set");
	}

	public DynamicFieldDetails createCollectionField(DynamicClassDetails entityClass,
													 String fieldName,
													 ClassDetails elementClass,
													 String collectionInterfaceName) {
		ClassDetails collectionClass = modelsContext.getClassDetailsRegistry()
				.resolveClassDetails(collectionInterfaceName);
		TypeDetails elementType = new ClassTypeDetailsImpl(
				elementClass, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				collectionClass, List.of(elementType), null);
		return entityClass.applyAttribute(
				fieldName, fieldType, false, true, modelsContext);
	}

	public DynamicFieldDetails createMapField(DynamicClassDetails entityClass,
												String fieldName,
												ClassDetails keyClass,
												ClassDetails valueClass) {
		ClassDetails mapClass = modelsContext.getClassDetailsRegistry()
				.resolveClassDetails("java.util.Map");
		TypeDetails keyType = new ClassTypeDetailsImpl(keyClass, TypeDetails.Kind.CLASS);
		TypeDetails valueType = new ClassTypeDetailsImpl(valueClass, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				mapClass, List.of(keyType, valueType), null);
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

	public void addEmbeddableClassDetails(ClassDetails classDetails) {
		embeddableClassDetails.add(classDetails);
	}

	public List<ClassDetails> getEmbeddableClassDetails() {
		return embeddableClassDetails;
	}

	public void addSubclassEntityDetails(ClassDetails classDetails) {
		subclassEntityDetails.add(classDetails);
	}

	public List<ClassDetails> getSubclassEntityDetails() {
		return subclassEntityDetails;
	}

	// --- Meta attribute storage ---

	public void addClassMetaAttribute(String className, String name, String value) {
		classMetaAttributes
				.computeIfAbsent(className, k -> new HashMap<>())
				.computeIfAbsent(name, k -> new ArrayList<>())
				.add(value);
	}

	public Map<String, List<String>> getClassMetaAttributes(String className) {
		return classMetaAttributes.getOrDefault(className, Collections.emptyMap());
	}

	public void addFieldMetaAttribute(String className, String fieldName,
									  String name, String value) {
		fieldMetaAttributes
				.computeIfAbsent(className, k -> new HashMap<>())
				.computeIfAbsent(fieldName, k -> new HashMap<>())
				.computeIfAbsent(name, k -> new ArrayList<>())
				.add(value);
	}

	public Map<String, Map<String, List<String>>> getFieldMetaAttributes(String className) {
		return fieldMetaAttributes.getOrDefault(className, Collections.emptyMap());
	}

	public Map<String, Map<String, List<String>>> getAllClassMetaAttributes() {
		return classMetaAttributes;
	}

	public Map<String, Map<String, Map<String, List<String>>>> getAllFieldMetaAttributes() {
		return fieldMetaAttributes;
	}

	/**
	 * Extracts tooling hints from a {@link ToolingHintContainer} and stores
	 * them as class-level meta attributes.
	 */
	public void extractClassMetaAttributes(String className, ToolingHintContainer container) {
		if (container == null) return;
		List<JaxbHbmToolingHintType> hints = container.getToolingHints();
		if (hints == null) return;
		for (JaxbHbmToolingHintType hint : hints) {
			addClassMetaAttribute(className, hint.getName(), hint.getValue());
			if (!hint.isInheritable()) {
				nonInheritableClassMetas.add(className + "#" + hint.getName());
			}
		}
	}

	/**
	 * Propagates inheritable meta attributes from a parent class to a
	 * subclass.  Only meta attributes that were not marked
	 * {@code inherit="false"} are propagated, and only when the subclass
	 * does not already define the same meta attribute.
	 */
	public void inheritClassMetaAttributes(String childClassName, String parentClassName) {
		Map<String, List<String>> parentMeta = classMetaAttributes.get(parentClassName);
		if (parentMeta == null) return;
		for (Map.Entry<String, List<String>> entry : parentMeta.entrySet()) {
			String metaName = entry.getKey();
			if (nonInheritableClassMetas.contains(parentClassName + "#" + metaName)) {
				continue;
			}
			// Don't overwrite child's own values
			Map<String, List<String>> childMeta = classMetaAttributes.get(childClassName);
			if (childMeta != null && childMeta.containsKey(metaName)) {
				continue;
			}
			for (String value : entry.getValue()) {
				addClassMetaAttribute(childClassName, metaName, value);
			}
		}
	}

	/**
	 * Extracts tooling hints from a {@link ToolingHintContainer} and stores
	 * them as field-level meta attributes.
	 */
	public void extractFieldMetaAttributes(String className, String fieldName,
										   ToolingHintContainer container) {
		if (container == null) return;
		List<JaxbHbmToolingHintType> hints = container.getToolingHints();
		if (hints == null) return;
		for (JaxbHbmToolingHintType hint : hints) {
			addFieldMetaAttribute(className, fieldName, hint.getName(), hint.getValue());
		}
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
			if (col.getComment() != null && !col.getComment().isEmpty()) {
				applyColumnComment(field, col.getComment());
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
					if (col.getComment() != null && !col.getComment().isEmpty()) {
						applyColumnComment(field, col.getComment());
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

	private void applyColumnComment(DynamicFieldDetails field, String comment) {
		CommentAnnotation commentAnnotation =
				HibernateAnnotations.COMMENT.createUsage(modelsContext);
		commentAnnotation.value(comment);
		field.addAnnotationUsage(commentAnnotation);
	}
}
