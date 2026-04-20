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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import org.hibernate.tool.internal.util.NameConverter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.Version;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeletes;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLInserts;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.SQLUpdates;

import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.ParameterizedTypeDetails;
import org.hibernate.models.spi.TypeDetails;

/**
 * Wraps a {@link ClassDetails} and provides template-friendly methods
 * for generating JPA-annotated Java entity source code.
 *
 * @author Koen Aers
 */
public class TemplateHelper {

	private final ClassDetails classDetails;
	private final ModelsContext modelsContext;
	private final ImportContext importContext;
	private final boolean annotated;
	private final boolean useGenerics;
	private final Map<String, List<String>> classMetaAttributes;
	private final Map<String, Map<String, List<String>>> fieldMetaAttributes;
	// All class meta attributes keyed by fully-qualified class name — used
	// to check meta attributes of the superclass (e.g. whether it is an interface)
	private final Map<String, Map<String, List<String>>> allClassMetaAttributes;
	// Maps field name → getter MethodDetails (for property-access entities where
	// annotations live on methods rather than fields)
	private final Map<String, MethodDetails> getterByFieldName;

	public TemplateHelper(ClassDetails classDetails, ModelsContext modelsContext,
				   ImportContext importContext, boolean annotated) {
		this(classDetails, modelsContext, importContext, annotated, true,
				Collections.emptyMap(), Collections.emptyMap());
	}

	TemplateHelper(ClassDetails classDetails, ModelsContext modelsContext,
				   ImportContext importContext, boolean annotated,
				   boolean useGenerics) {
		this(classDetails, modelsContext, importContext, annotated, useGenerics,
				Collections.emptyMap(), Collections.emptyMap());
	}

	public TemplateHelper(ClassDetails classDetails, ModelsContext modelsContext,
				   ImportContext importContext, boolean annotated,
				   Map<String, List<String>> classMetaAttributes,
				   Map<String, Map<String, List<String>>> fieldMetaAttributes) {
		this(classDetails, modelsContext, importContext, annotated, true,
				classMetaAttributes, fieldMetaAttributes);
	}

	TemplateHelper(ClassDetails classDetails, ModelsContext modelsContext,
				   ImportContext importContext, boolean annotated,
				   boolean useGenerics,
				   Map<String, List<String>> classMetaAttributes,
				   Map<String, Map<String, List<String>>> fieldMetaAttributes) {
		this(classDetails, modelsContext, importContext, annotated, useGenerics,
				classMetaAttributes, fieldMetaAttributes, Collections.emptyMap());
	}

	TemplateHelper(ClassDetails classDetails, ModelsContext modelsContext,
				   ImportContext importContext, boolean annotated,
				   boolean useGenerics,
				   Map<String, List<String>> classMetaAttributes,
				   Map<String, Map<String, List<String>>> fieldMetaAttributes,
				   Map<String, Map<String, List<String>>> allClassMetaAttributes) {
		this.classDetails = classDetails;
		this.modelsContext = modelsContext;
		this.importContext = importContext;
		this.annotated = annotated;
		this.useGenerics = useGenerics;
		this.classMetaAttributes = classMetaAttributes != null ? classMetaAttributes : Collections.emptyMap();
		this.fieldMetaAttributes = fieldMetaAttributes != null ? fieldMetaAttributes : Collections.emptyMap();
		this.allClassMetaAttributes = allClassMetaAttributes != null ? allClassMetaAttributes : Collections.emptyMap();
		// Process extra-import meta-attribute
		List<String> extraImports = this.classMetaAttributes.getOrDefault("extra-import", Collections.emptyList());
		for (String fqcn : extraImports) {
			importType(fqcn);
		}
		// Build getter method map for property-access entities
		this.getterByFieldName = buildGetterMap(classDetails);
	}

	private static Map<String, MethodDetails> buildGetterMap(ClassDetails classDetails) {
		Map<String, MethodDetails> map = new HashMap<>();
		for (MethodDetails method : classDetails.getMethods()) {
			if (method.getMethodKind() == MethodDetails.MethodKind.GETTER) {
				map.put(method.resolveAttributeName(), method);
			}
		}
		return map;
	}

	/**
	 * Returns the annotation source for a field — the field itself if it
	 * carries annotations directly, or the corresponding getter method
	 * for property-access entities.
	 */
	private AnnotationTarget getAnnotationSource(FieldDetails field) {
		MethodDetails getter = getterByFieldName.get(field.getName());
		if (getter != null && !getter.getDirectAnnotationUsages().isEmpty()) {
			return getter;
		}
		return field;
	}

	<A extends Annotation> boolean fieldHasAnnotation(
			FieldDetails field, Class<A> annotationType) {
		return getAnnotationSource(field).hasDirectAnnotationUsage(annotationType);
	}

	<A extends Annotation> A fieldGetAnnotation(
			FieldDetails field, Class<A> annotationType) {
		return getAnnotationSource(field).getDirectAnnotationUsage(annotationType);
	}

	boolean isAnnotated() {
		return annotated;
	}

	// --- Package / class ---

	public String getPackageDeclaration() {
		String pkg = getGeneratedPackageName();
		if (pkg != null && !pkg.isEmpty()) {
			return "package " + pkg + ";";
		}
		return "";
	}

	public boolean isInterface() {
		return hasClassMetaAttribute("interface")
				&& "true".equalsIgnoreCase(getClassMetaAttribute("interface"));
	}

	public String getDeclarationType() {
		return isInterface() ? "interface" : "class";
	}

	private boolean isSuperclassInterface() {
		ClassDetails superClass = classDetails.getSuperClass();
		if (superClass == null || "java.lang.Object".equals(superClass.getClassName())) {
			return false;
		}
		Map<String, List<String>> superMeta = allClassMetaAttributes.getOrDefault(
				superClass.getClassName(), Collections.emptyMap());
		List<String> ifaceValues = superMeta.getOrDefault("interface", Collections.emptyList());
		return !ifaceValues.isEmpty() && "true".equalsIgnoreCase(ifaceValues.get(0));
	}

	public String getDeclarationName() {
		if (hasClassMetaAttribute("generated-class")) {
			String generatedClass = getClassMetaAttribute("generated-class");
			int lastDot = generatedClass.lastIndexOf('.');
			return lastDot > 0 ? generatedClass.substring(lastDot + 1) : generatedClass;
		}
		String className = classDetails.getClassName();
		int lastDot = className.lastIndexOf('.');
		return lastDot > 0 ? className.substring(lastDot + 1) : className;
	}

	public String getExtendsDeclaration() {
		if (isInterface()) {
			// Interfaces use extends for both superclass and extends meta
			List<String> extendsList = new ArrayList<>();
			ClassDetails superClass = classDetails.getSuperClass();
			if (superClass != null && !"java.lang.Object".equals(superClass.getClassName())) {
				extendsList.add(importType(superClass.getClassName()));
			}
			if (hasClassMetaAttribute("extends")) {
				String extendsFqcn = getClassMetaAttribute("extends");
				extendsList.add(importType(extendsFqcn));
			}
			if (!extendsList.isEmpty()) {
				return "extends " + String.join(", ", extendsList) + " ";
			}
			return "";
		}
		// For classes: extend superclass only if it's not an interface
		ClassDetails superClass = classDetails.getSuperClass();
		if (superClass != null && !"java.lang.Object".equals(superClass.getClassName())) {
			if (!isSuperclassInterface()) {
				importType(superClass.getClassName());
				String superName = superClass.getClassName();
				int dot = superName.lastIndexOf('.');
				return "extends " + (dot > 0 ? superName.substring(dot + 1) : superName) + " ";
			}
		}
		if (hasClassMetaAttribute("extends")) {
			String extendsFqcn = getClassMetaAttribute("extends");
			String simpleName = importType(extendsFqcn);
			return "extends " + simpleName + " ";
		}
		return "";
	}

	public String getImplementsDeclaration() {
		if (isInterface()) {
			return "";
		}
		List<String> interfaces = new ArrayList<>();
		// If superclass is an interface, add it to implements
		if (isSuperclassInterface()) {
			ClassDetails superClass = classDetails.getSuperClass();
			interfaces.add(importType(superClass.getClassName()));
		}
		if (hasClassMetaAttribute("implements")) {
			for (String fqcn : classMetaAttributes.get("implements")) {
				interfaces.add(importType(fqcn));
			}
		}
		interfaces.add(importType(Serializable.class.getName()));
		return "implements " + String.join(", ", interfaces);
	}

	/**
	 * Returns the type name to use in the {@code instanceof} check of
	 * {@code equals()}. When the entity has a proxy interface, returns the
	 * proxy interface name so that equals works with proxy instances.
	 */
	public String getEqualsInstanceOfType() {
		if (hasClassMetaAttribute("implements")) {
			List<String> impls = classMetaAttributes.get("implements");
			// Use the first non-Serializable interface as the proxy type
			for (String fqcn : impls) {
				if (!"java.io.Serializable".equals(fqcn)) {
					return importType(fqcn);
				}
			}
		}
		return getDeclarationName();
	}

	public String generateImports() {
		return importContext.generateImports();
	}

	// --- Import delegation ---

	public String importType(String fqcn) {
		return importContext.importType(fqcn);
	}

	// --- ClassDetails access ---

	public ClassDetails getClassDetails() {
		return classDetails;
	}

	public boolean isEmbeddable() {
		return classDetails.hasDirectAnnotationUsage(Embeddable.class);
	}

	// --- Field categorization ---

	public FieldDetails getCompositeIdField() {
		for (FieldDetails field : getEffectiveFields()) {
			if (fieldHasAnnotation(field, EmbeddedId.class)) {
				return field;
			}
		}
		return null;
	}

	public List<FieldDetails> getBasicFields() {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : getEffectiveFields()) {
			if (!isRelationshipField(field) && !isEmbeddedField(field) && !isEmbeddedIdField(field)
					&& !fieldHasAnnotation(field, ElementCollection.class)) {
				result.add(field);
			}
		}
		return result;
	}

	public List<FieldDetails> getManyToOneFields() {
		return getFieldsWithAnnotation(ManyToOne.class);
	}

	public List<FieldDetails> getOneToOneFields() {
		return getFieldsWithAnnotation(OneToOne.class);
	}

	public List<FieldDetails> getOneToManyFields() {
		return getFieldsWithAnnotation(OneToMany.class);
	}

	public List<FieldDetails> getManyToManyFields() {
		return getFieldsWithAnnotation(ManyToMany.class);
	}

	public List<FieldDetails> getEmbeddedFields() {
		return getFieldsWithAnnotation(Embedded.class);
	}

	public List<FieldDetails> getElementCollectionFields() {
		return getFieldsWithAnnotation(ElementCollection.class);
	}

	public List<FieldDetails> getAnyFields() {
		return getFieldsWithAnnotation(Any.class);
	}

	public List<FieldDetails> getManyToAnyFields() {
		return getFieldsWithAnnotation(ManyToAny.class);
	}

	// --- Field info methods ---

	public boolean isPrimaryKey(FieldDetails field) {
		return fieldHasAnnotation(field, Id.class);
	}

	public boolean isVersion(FieldDetails field) {
		return fieldHasAnnotation(field, Version.class);
	}

	private boolean isGeneratedId(FieldDetails field) {
		return isPrimaryKey(field)
				&& fieldHasAnnotation(field, GeneratedValue.class);
	}

	public boolean isLob(FieldDetails field) {
		return fieldHasAnnotation(field, Lob.class);
	}

	// --- Type name resolution ---

	public String getJavaTypeName(FieldDetails field) {
		if (hasFieldMetaAttribute(field, "property-type")) {
			return importType(getFieldMetaAttribute(field, "property-type"));
		}
		return importType(field.getType().determineRawClass().getClassName());
	}

	public String getCollectionTypeName(FieldDetails field) {
		String rawClassName = field.getType().determineRawClass().getClassName();
		String simpleName = importType(rawClassName);
		if (!useGenerics) {
			return simpleName;
		}
		TypeDetails type = field.getType();
		if (type.getTypeKind() == TypeDetails.Kind.PARAMETERIZED_TYPE) {
			ParameterizedTypeDetails paramType = type.asParameterizedType();
			java.util.List<TypeDetails> args = paramType.getArguments();
			if ("java.util.Map".equals(rawClassName) && args.size() == 2) {
				String keySimple = importType(boxPrimitive(args.get(0).determineRawClass().getClassName()));
				String valueSimple = importType(boxPrimitive(args.get(1).determineRawClass().getClassName()));
				return simpleName + "<" + keySimple + ", " + valueSimple + ">";
			} else if (!args.isEmpty()) {
				String elementSimple = importType(boxPrimitive(
						args.get(args.size() - 1).determineRawClass().getClassName()));
				return simpleName + "<" + elementSimple + ">";
			}
		}
		TypeDetails elementType = field.getElementType();
		if (elementType != null) {
			String elementClassName = elementType.determineRawClass().getClassName();
			elementClassName = boxPrimitive(elementClassName);
			String elementSimpleName = importType(elementClassName);
			return simpleName + "<" + elementSimpleName + ">";
		}
		return simpleName + "<?>";
	}

	public String getCollectionInitializerType(FieldDetails field) {
		String rawClassName = field.getType().determineRawClass().getClassName();
		return switch (rawClassName) {
			case "java.util.List" -> importType("java.util.ArrayList");
			case "java.util.Map" -> importType("java.util.HashMap");
			case "java.util.Collection" -> importType("java.util.ArrayList");
			default -> importType("java.util.HashSet");
		};
	}

	// --- Field/getter/setter names ---

	public String getFieldName(FieldDetails field) {
		String name = field.getName();
		if (NameConverter.isReservedJavaKeyword(name)) {
			return name + "_";
		}
		return name;
	}

	public String getFieldName(String name) {
		if (NameConverter.isReservedJavaKeyword(name)) {
			return name + "_";
		}
		return name;
	}

	public String getGetterName(String fieldName) {
		return "get" + capitalize(fieldName);
	}

	public String getGetterName(FieldDetails field) {
		String prefix = "boolean".equals(
				field.getType().determineRawClass().getClassName()) ? "is" : "get";
		return prefix + capitalize(field.getName());
	}

	public String getSetterName(String fieldName) {
		return "set" + capitalize(fieldName);
	}

	// --- Annotation generation ---

	public String generateClassAnnotations() {
		if (!annotated) {
			return "";
		}
		return new ClassAnnotationGenerator(classDetails, importContext, this).generate();
	}

	public String generateIdAnnotations(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateIdAnnotations(field);
	}

	public String generateVersionAnnotation() {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateVersionAnnotation();
	}

	public String generateBasicAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateBasicAnnotation(field);
	}

	public String generateOptimisticLockAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateOptimisticLockAnnotation(field);
	}

	public String generateAccessAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateAccessAnnotation(field);
	}

	public String generateTemporalAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateTemporalAnnotation(field);
	}

	public String generateLobAnnotation() {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateLobAnnotation();
	}

	public String generateFormulaAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateFormulaAnnotation(field);
	}

	public boolean hasFormula(FieldDetails field) {
		return fieldHasAnnotation(field, Formula.class);
	}

	public String generateElementCollectionAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateElementCollectionAnnotation(field);
	}

	public String generateNaturalIdAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateNaturalIdAnnotation(field);
	}

	public String generateOrderByAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateOrderByAnnotation(field);
	}

	public String generateOrderColumnAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateOrderColumnAnnotation(field);
	}

	public String generateFilterAnnotations(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateFilterAnnotations(field);
	}

	public List<FilterInfo> getFieldFilters(FieldDetails field) {
		return new FieldAnnotationGenerator(importContext, this)
				.getFieldFilters(field);
	}

	public String generateBagAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateBagAnnotation(field);
	}

	public String generateCollectionIdAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateCollectionIdAnnotation(field);
	}

	public String generateSortAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateSortAnnotation(field);
	}

	public String generateMapKeyAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateMapKeyAnnotation(field);
	}

	public String generateMapKeyColumnAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateMapKeyColumnAnnotation(field);
	}

	public String generateFetchAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateFetchAnnotation(field);
	}

	public String generateNotFoundAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateNotFoundAnnotation(field);
	}

	public String generateAnyAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateAnyAnnotation(field);
	}

	public String generateManyToAnyAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateManyToAnyAnnotation(field);
	}

	public String generateConvertAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateConvertAnnotation(field);
	}

	public String generateColumnAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateColumnAnnotation(field);
	}

	public String generateColumnTransformerAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new FieldAnnotationGenerator(importContext, this)
				.generateColumnTransformerAnnotation(field);
	}

	public String generateManyToOneAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new RelationshipAnnotationGenerator(importContext, this)
				.generateManyToOneAnnotation(field);
	}

	public String generateOneToManyAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new RelationshipAnnotationGenerator(importContext, this)
				.generateOneToManyAnnotation(field);
	}

	public String generateOneToOneAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new RelationshipAnnotationGenerator(importContext, this)
				.generateOneToOneAnnotation(field);
	}

	public String generateManyToManyAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new RelationshipAnnotationGenerator(importContext, this)
				.generateManyToManyAnnotation(field);
	}

	public String generateEmbeddedIdAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new RelationshipAnnotationGenerator(importContext, this)
				.generateEmbeddedIdAnnotation(field);
	}

	public String generateEmbeddedAnnotation(FieldDetails field) {
		if (!annotated) return "";
		return new RelationshipAnnotationGenerator(importContext, this)
				.generateEmbeddedAnnotation(field);
	}

	// --- Subclass check ---

	public boolean isSubclass() {
		ClassDetails superClass = classDetails.getSuperClass();
		return superClass != null && !"java.lang.Object".equals(superClass.getClassName());
	}

	// --- Constructor support ---

	public boolean needsFullConstructor() {
		return !getFullConstructorProperties().isEmpty();
	}

	public List<FullConstructorProperty> getFullConstructorProperties() {
		List<FullConstructorProperty> props = new ArrayList<>();
		// Composite ID
		FieldDetails cid = getCompositeIdField();
		if (cid != null) {
			props.add(new FullConstructorProperty(getJavaTypeName(cid), getFieldName(cid)));
		}
		// Basic fields (skip version, formula, generated id, respect gen-property)
		for (FieldDetails field : getBasicFields()) {
			if (!isVersion(field) && isGenProperty(field) && !hasFormula(field)
					&& !isGeneratedId(field)) {
				props.add(new FullConstructorProperty(getJavaTypeName(field), getFieldName(field)));
			}
		}
		// ManyToOne
		for (FieldDetails field : getManyToOneFields()) {
			props.add(new FullConstructorProperty(getJavaTypeName(field), getFieldName(field)));
		}
		// OneToOne
		for (FieldDetails field : getOneToOneFields()) {
			props.add(new FullConstructorProperty(getJavaTypeName(field), getFieldName(field)));
		}
		// OneToMany
		for (FieldDetails field : getOneToManyFields()) {
			props.add(new FullConstructorProperty(getCollectionTypeName(field), getFieldName(field)));
		}
		// ManyToMany
		for (FieldDetails field : getManyToManyFields()) {
			props.add(new FullConstructorProperty(getCollectionTypeName(field), getFieldName(field)));
		}
		// Embedded
		for (FieldDetails field : getEmbeddedFields()) {
			props.add(new FullConstructorProperty(getJavaTypeName(field), getFieldName(field)));
		}
		return props;
	}

	public boolean needsMinimalConstructor() {
		int minTotal = getMinimalConstructorProperties().size()
				+ getSuperclassMinimalConstructorProperties().size();
		int fullTotal = getFullConstructorProperties().size()
				+ getSuperclassFullConstructorProperties().size();
		return minTotal > 0 && minTotal < fullTotal;
	}

	public List<FullConstructorProperty> getMinimalConstructorProperties() {
		List<FullConstructorProperty> props = new ArrayList<>();
		// Composite ID (always included — always assigned)
		FieldDetails cid = getCompositeIdField();
		if (cid != null) {
			props.add(new FullConstructorProperty(getJavaTypeName(cid), getFieldName(cid)));
		}
		// Basic fields: non-nullable, non-version, non-generated-id, respects gen-property
		for (FieldDetails field : getBasicFields()) {
			if (isVersion(field) || !isGenProperty(field) || hasFieldDefaultValue(field)
					|| hasFormula(field)) {
				continue;
			}
			if (isPrimaryKey(field)) {
				// Include ID only if no generator (assigned)
				GeneratedValue gv = fieldGetAnnotation(field,GeneratedValue.class);
				if (gv != null) {
					continue;
				}
				props.add(new FullConstructorProperty(getJavaTypeName(field), getFieldName(field)));
			} else {
				Column col = fieldGetAnnotation(field,Column.class);
				if (col != null && !col.nullable()) {
					props.add(new FullConstructorProperty(getJavaTypeName(field), getFieldName(field)));
				}
			}
		}
		// ManyToOne: non-optional
		for (FieldDetails field : getManyToOneFields()) {
			ManyToOne m2o = fieldGetAnnotation(field,ManyToOne.class);
			if (m2o != null && !m2o.optional()) {
				props.add(new FullConstructorProperty(getJavaTypeName(field), getFieldName(field)));
			}
		}
		// Embedded components
		for (FieldDetails field : getEmbeddedFields()) {
			props.add(new FullConstructorProperty(getJavaTypeName(field), getFieldName(field)));
		}
		return props;
	}

	public String getMinimalConstructorParameterList() {
		StringBuilder sb = new StringBuilder();
		for (FullConstructorProperty prop : getSuperclassMinimalConstructorProperties()) {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(prop.typeName()).append(" ").append(prop.fieldName());
		}
		for (FullConstructorProperty prop : getMinimalConstructorProperties()) {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(prop.typeName()).append(" ").append(prop.fieldName());
		}
		return sb.toString();
	}

	public String getFullConstructorParameterList() {
		StringBuilder sb = new StringBuilder();
		for (FullConstructorProperty prop : getSuperclassFullConstructorProperties()) {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(prop.typeName()).append(" ").append(prop.fieldName());
		}
		for (FullConstructorProperty prop : getFullConstructorProperties()) {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(prop.typeName()).append(" ").append(prop.fieldName());
		}
		return sb.toString();
	}

	public List<FullConstructorProperty> getSuperclassFullConstructorProperties() {
		if (!isSubclass() || isSuperclassInterface()) {
			return Collections.emptyList();
		}
		return createSuperclassHelper().getFullConstructorProperties();
	}

	public String getSuperclassFullConstructorArgumentList() {
		StringBuilder sb = new StringBuilder();
		for (FullConstructorProperty prop : getSuperclassFullConstructorProperties()) {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(prop.fieldName());
		}
		return sb.toString();
	}

	public List<FullConstructorProperty> getSuperclassMinimalConstructorProperties() {
		if (!isSubclass() || isSuperclassInterface()) {
			return Collections.emptyList();
		}
		return createSuperclassHelper().getMinimalConstructorProperties();
	}

	public String getSuperclassMinimalConstructorArgumentList() {
		StringBuilder sb = new StringBuilder();
		for (FullConstructorProperty prop : getSuperclassMinimalConstructorProperties()) {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(prop.fieldName());
		}
		return sb.toString();
	}

	private TemplateHelper createSuperclassHelper() {
		ClassDetails superClass = classDetails.getSuperClass();
		return new TemplateHelper(superClass, modelsContext, importContext, annotated);
	}

	// --- toString support ---

	public List<ToStringProperty> getToStringProperties() {
		List<FieldDetails> basicFields = getBasicFields();
		boolean hasExplicitToString = basicFields.stream()
				.anyMatch(f -> hasFieldMetaAttribute(f, "use-in-tostring"));
		List<ToStringProperty> props = new ArrayList<>();
		if (hasExplicitToString) {
			for (FieldDetails field : basicFields) {
				if (getFieldMetaAsBool(field, "use-in-tostring", false)) {
					props.add(new ToStringProperty(field.getName(), getGetterName(field)));
				}
			}
		} else {
			FieldDetails cid = getCompositeIdField();
			if (cid != null) {
				props.add(new ToStringProperty(cid.getName(), getGetterName(cid)));
			}
			for (FieldDetails field : basicFields) {
				if (isGenProperty(field)) {
					props.add(new ToStringProperty(field.getName(), getGetterName(field)));
				}
			}
		}
		return props;
	}

	// --- equals/hashCode support ---

	public boolean hasCompositeId() {
		return getCompositeIdField() != null;
	}

	public boolean needsEqualsHashCode() {
		if (isEmbeddable()) return true;
		if (hasNaturalId()) return true;
		boolean hasExplicitEquals = getBasicFields().stream()
				.anyMatch(f -> hasFieldMetaAttribute(f, "use-in-equals"));
		if (hasExplicitEquals) return true;
		return hasCompositeId() || !getIdentifierFields().isEmpty();
	}

	public boolean hasNaturalId() {
		return getBasicFields().stream()
				.anyMatch(f -> fieldHasAnnotation(f, NaturalId.class));
	}

	public List<FieldDetails> getNaturalIdFields() {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : getBasicFields()) {
			if (fieldHasAnnotation(field, NaturalId.class)) {
				result.add(field);
			}
		}
		return result;
	}

	// --- Named queries ---

	public List<NamedQueryInfo> getNamedQueries() {
		List<NamedQueryInfo> result = new ArrayList<>();
		NamedQuery single = classDetails.getDirectAnnotationUsage(NamedQuery.class);
		if (single != null) {
			result.add(new NamedQueryInfo(single.name(), single.query()));
		}
		NamedQueries container = classDetails.getDirectAnnotationUsage(NamedQueries.class);
		if (container != null) {
			for (NamedQuery nq : container.value()) {
				result.add(new NamedQueryInfo(nq.name(), nq.query()));
			}
		}
		return result;
	}

	public List<NamedNativeQueryInfo> getNamedNativeQueries() {
		List<NamedNativeQueryInfo> result = new ArrayList<>();
		NamedNativeQuery single = classDetails.getDirectAnnotationUsage(NamedNativeQuery.class);
		if (single != null) {
			result.add(toNamedNativeQueryInfo(single));
		}
		NamedNativeQueries container = classDetails.getDirectAnnotationUsage(NamedNativeQueries.class);
		if (container != null) {
			for (NamedNativeQuery nnq : container.value()) {
				result.add(toNamedNativeQueryInfo(nnq));
			}
		}
		return result;
	}

	private NamedNativeQueryInfo toNamedNativeQueryInfo(NamedNativeQuery nnq) {
		String resultClassName = null;
		if (nnq.resultClass() != null && nnq.resultClass() != void.class) {
			resultClassName = nnq.resultClass().getName();
		}
		String resultSetMapping = nnq.resultSetMapping() != null && !nnq.resultSetMapping().isEmpty()
				? nnq.resultSetMapping() : null;
		return new NamedNativeQueryInfo(nnq.name(), nnq.query(), resultClassName, resultSetMapping);
	}

	public List<SqlResultSetMappingInfo> getSqlResultSetMappings() {
		List<SqlResultSetMappingInfo> result = new ArrayList<>();
		SqlResultSetMapping single = classDetails.getDirectAnnotationUsage(SqlResultSetMapping.class);
		if (single != null) {
			result.add(toSqlResultSetMappingInfo(single));
		}
		SqlResultSetMappings container = classDetails.getDirectAnnotationUsage(SqlResultSetMappings.class);
		if (container != null) {
			for (SqlResultSetMapping mapping : container.value()) {
				result.add(toSqlResultSetMappingInfo(mapping));
			}
		}
		return result;
	}

	private SqlResultSetMappingInfo toSqlResultSetMappingInfo(SqlResultSetMapping mapping) {
		List<EntityResultInfo> entityResults = new ArrayList<>();
		for (EntityResult er : mapping.entities()) {
			List<FieldResultInfo> fieldResults = new ArrayList<>();
			for (FieldResult fr : er.fields()) {
				fieldResults.add(new FieldResultInfo(fr.name(), fr.column()));
			}
			String discriminator = er.discriminatorColumn() != null && !er.discriminatorColumn().isEmpty()
					? er.discriminatorColumn() : null;
			entityResults.add(new EntityResultInfo(er.entityClass().getName(), discriminator, fieldResults));
		}
		List<ColumnResultInfo> columnResults = new ArrayList<>();
		for (ColumnResult cr : mapping.columns()) {
			columnResults.add(new ColumnResultInfo(cr.name()));
		}
		return new SqlResultSetMappingInfo(mapping.name(), entityResults, columnResults);
	}

	public record NamedQueryInfo(String name, String query) {}

	public record NamedNativeQueryInfo(String name, String query,
									   String resultClass, String resultSetMapping) {}

	public record SqlResultSetMappingInfo(String name, List<EntityResultInfo> entityResults,
										  List<ColumnResultInfo> columnResults) {}

	public record EntityResultInfo(String entityClass, String discriminatorColumn,
								   List<FieldResultInfo> fieldResults) {}

	public record FieldResultInfo(String name, String column) {}

	public record ColumnResultInfo(String name) {}

	// --- Filters ---

	public List<FilterInfo> getFilters() {
		List<FilterInfo> result = new ArrayList<>();
		Filter single = classDetails.getDirectAnnotationUsage(Filter.class);
		if (single != null) {
			result.add(new FilterInfo(single.name(), single.condition()));
		}
		Filters container = classDetails.getDirectAnnotationUsage(Filters.class);
		if (container != null) {
			for (Filter f : container.value()) {
				result.add(new FilterInfo(f.name(), f.condition()));
			}
		}
		return result;
	}

	public List<FilterDefInfo> getFilterDefs() {
		List<FilterDefInfo> result = new ArrayList<>();
		FilterDef single = classDetails.getDirectAnnotationUsage(FilterDef.class);
		if (single != null) {
			result.add(toFilterDefInfo(single));
		}
		FilterDefs container = classDetails.getDirectAnnotationUsage(FilterDefs.class);
		if (container != null) {
			for (FilterDef fd : container.value()) {
				result.add(toFilterDefInfo(fd));
			}
		}
		return result;
	}

	private FilterDefInfo toFilterDefInfo(FilterDef fd) {
		Map<String, Class<?>> params = new java.util.LinkedHashMap<>();
		if (fd.parameters() != null) {
			for (ParamDef pd : fd.parameters()) {
				params.put(pd.name(), pd.type());
			}
		}
		return new FilterDefInfo(fd.name(), fd.defaultCondition(), params);
	}

	public record FilterInfo(String name, String condition) {}

	public record FilterDefInfo(String name, String defaultCondition, Map<String, Class<?>> parameters) {}

	public record SecondaryTableInfo(String tableName, List<String> keyColumns) {}

	public List<SQLInsert> getSQLInserts() {
		List<SQLInsert> result = new ArrayList<>();
		SQLInsert single = classDetails.getDirectAnnotationUsage(SQLInsert.class);
		if (single != null) {
			result.add(single);
		}
		SQLInserts container = classDetails.getDirectAnnotationUsage(SQLInserts.class);
		if (container != null) {
			for (SQLInsert si : container.value()) {
				result.add(si);
			}
		}
		return result;
	}

	public List<SQLUpdate> getSQLUpdates() {
		List<SQLUpdate> result = new ArrayList<>();
		SQLUpdate single = classDetails.getDirectAnnotationUsage(SQLUpdate.class);
		if (single != null) {
			result.add(single);
		}
		SQLUpdates container = classDetails.getDirectAnnotationUsage(SQLUpdates.class);
		if (container != null) {
			for (SQLUpdate su : container.value()) {
				result.add(su);
			}
		}
		return result;
	}

	public List<SQLDelete> getSQLDeletes() {
		List<SQLDelete> result = new ArrayList<>();
		SQLDelete single = classDetails.getDirectAnnotationUsage(SQLDelete.class);
		if (single != null) {
			result.add(single);
		}
		SQLDeletes container = classDetails.getDirectAnnotationUsage(SQLDeletes.class);
		if (container != null) {
			for (SQLDelete sd : container.value()) {
				result.add(sd);
			}
		}
		return result;
	}

	public List<FetchProfile> getFetchProfiles() {
		List<FetchProfile> result = new ArrayList<>();
		FetchProfile single = classDetails.getDirectAnnotationUsage(FetchProfile.class);
		if (single != null) {
			result.add(single);
		}
		FetchProfiles container = classDetails.getDirectAnnotationUsage(FetchProfiles.class);
		if (container != null) {
			for (FetchProfile fp : container.value()) {
				result.add(fp);
			}
		}
		return result;
	}

	public List<SecondaryTableInfo> getSecondaryTables() {
		List<SecondaryTableInfo> result = new ArrayList<>();
		SecondaryTable single = classDetails.getDirectAnnotationUsage(SecondaryTable.class);
		if (single != null) {
			result.add(toSecondaryTableInfo(single));
		}
		SecondaryTables container = classDetails.getDirectAnnotationUsage(SecondaryTables.class);
		if (container != null) {
			for (SecondaryTable st : container.value()) {
				result.add(toSecondaryTableInfo(st));
			}
		}
		return result;
	}

	private SecondaryTableInfo toSecondaryTableInfo(SecondaryTable st) {
		List<String> keyColumns = new ArrayList<>();
		if (st.pkJoinColumns() != null) {
			for (PrimaryKeyJoinColumn pkjc : st.pkJoinColumns()) {
				if (pkjc.name() != null && !pkjc.name().isEmpty()) {
					keyColumns.add(pkjc.name());
				}
			}
		}
		return new SecondaryTableInfo(st.name(), keyColumns);
	}

	public boolean hasExplicitEqualsColumns() {
		return getBasicFields().stream()
				.anyMatch(f -> getFieldMetaAsBool(f, "use-in-equals", false));
	}

	public List<FieldDetails> getEqualsFields() {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : getBasicFields()) {
			if (getFieldMetaAsBool(field, "use-in-equals", false)) {
				result.add(field);
			}
		}
		return result;
	}

	public List<FieldDetails> getIdentifierFields() {
		if (isEmbeddable()) {
			return getBasicFields();
		}
		// Prefer @NaturalId fields for equals/hashCode
		List<FieldDetails> naturalIdFields = getNaturalIdFields();
		if (!naturalIdFields.isEmpty()) {
			return naturalIdFields;
		}
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : getBasicFields()) {
			if (isPrimaryKey(field)) {
				result.add(field);
			}
		}
		return result;
	}

	public String generateEqualsExpression(FieldDetails field) {
		String getter = getGetterName(field) + "()";
		String typeName = field.getType().determineRawClass().getClassName();
		if (isPrimitiveType(typeName)) {
			return "this." + getter + " == castOther." + getter;
		}
		return "((this." + getter + " == castOther." + getter + ") || "
				+ "(this." + getter + " != null && castOther." + getter + " != null && "
				+ "this." + getter + ".equals(castOther." + getter + ")))";
	}

	public String generateHashCodeExpression(FieldDetails field) {
		String getter = "this." + getGetterName(field) + "()";
		String typeName = field.getType().determineRawClass().getClassName();
		return switch (typeName) {
			case "int", "char", "short", "byte" -> getter;
			case "boolean" -> "(" + getter + " ? 1 : 0)";
			case "long" -> "(int) " + getter;
			case "float" -> {
				importType("java.lang.Float");
				yield "Float.floatToIntBits(" + getter + ")";
			}
			case "double" -> {
				importType("java.lang.Double");
				yield "(int) Double.doubleToLongBits(" + getter + ")";
			}
			default -> "(" + getter + " == null ? 0 : " + getter + ".hashCode())";
		};
	}

	// --- Meta-attribute support ---

	public boolean hasClassMetaAttribute(String name) {
		return classMetaAttributes.containsKey(name);
	}

	public String getClassMetaAttribute(String name) {
		List<String> values = classMetaAttributes.getOrDefault(name, Collections.emptyList());
		return values.isEmpty() ? "" : String.join("\n", values);
	}

	public boolean hasFieldMetaAttribute(FieldDetails field, String name) {
		Map<String, List<String>> attrs = fieldMetaAttributes.getOrDefault(
				field.getName(), Collections.emptyMap());
		return attrs.containsKey(name);
	}

	public boolean getFieldMetaAsBool(FieldDetails field, String name, boolean defaultValue) {
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

	public boolean hasClassDescription() {
		return hasClassMetaAttribute("class-description");
	}

	public String getClassDescription() {
		return getClassMetaAttribute("class-description");
	}

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

	public boolean hasExtraClassCode() {
		return hasClassMetaAttribute("class-code");
	}

	public String getExtraClassCode() {
		return getClassMetaAttribute("class-code");
	}

	// --- Inner record types for template data ---

	public record FullConstructorProperty(String typeName, String fieldName) {}

	public record ToStringProperty(String fieldName, String getterName) {}

	// --- Lifecycle callbacks ---

	@SuppressWarnings("unchecked")
	private static final Class<? extends java.lang.annotation.Annotation>[] LIFECYCLE_ANNOTATIONS = new Class[] {
			PrePersist.class, PostPersist.class,
			PreRemove.class, PostRemove.class,
			PreUpdate.class, PostUpdate.class,
			PostLoad.class
	};

	public List<LifecycleCallbackInfo> getLifecycleCallbacks() {
		List<LifecycleCallbackInfo> result = new ArrayList<>();
		for (MethodDetails method : classDetails.getMethods()) {
			for (Class<? extends java.lang.annotation.Annotation> ann : LIFECYCLE_ANNOTATIONS) {
				if (method.hasDirectAnnotationUsage(ann)) {
					result.add(new LifecycleCallbackInfo(ann.getSimpleName(), method.getName()));
				}
			}
		}
		return result;
	}

	public String generateLifecycleCallbackAnnotation(LifecycleCallbackInfo callback) {
		if (!annotated) {
			return "";
		}
		String fqcn = switch (callback.annotationType()) {
			case "PrePersist" -> "jakarta.persistence.PrePersist";
			case "PostPersist" -> "jakarta.persistence.PostPersist";
			case "PreRemove" -> "jakarta.persistence.PreRemove";
			case "PostRemove" -> "jakarta.persistence.PostRemove";
			case "PreUpdate" -> "jakarta.persistence.PreUpdate";
			case "PostUpdate" -> "jakarta.persistence.PostUpdate";
			case "PostLoad" -> "jakarta.persistence.PostLoad";
			default -> throw new IllegalArgumentException("Unknown lifecycle annotation: " + callback.annotationType());
		};
		importType(fqcn);
		return "@" + callback.annotationType();
	}

	public record LifecycleCallbackInfo(String annotationType, String methodName) {}

	// --- Private helpers ---

	private String getPackageName() {
		String className = classDetails.getClassName();
		if (className == null) return "";
		int lastDot = className.lastIndexOf('.');
		return lastDot > 0 ? className.substring(0, lastDot) : "";
	}

	private String getGeneratedPackageName() {
		if (hasClassMetaAttribute("generated-class")) {
			String generatedClass = getClassMetaAttribute("generated-class");
			int lastDot = generatedClass.lastIndexOf('.');
			return lastDot > 0 ? generatedClass.substring(0, lastDot) : "";
		}
		return getPackageName();
	}

	private boolean isRelationshipField(FieldDetails field) {
		return fieldHasAnnotation(field, ManyToOne.class)
				|| fieldHasAnnotation(field, OneToMany.class)
				|| fieldHasAnnotation(field, OneToOne.class)
				|| fieldHasAnnotation(field, ManyToMany.class);
	}

	private boolean isEmbeddedField(FieldDetails field) {
		return fieldHasAnnotation(field, Embedded.class);
	}

	private boolean isEmbeddedIdField(FieldDetails field) {
		return fieldHasAnnotation(field, EmbeddedId.class);
	}

	/**
	 * Returns the effective fields for this entity. For most entities, this
	 * is just the entity's own fields. When the superclass is an interface,
	 * the parent's fields are included (since the class must provide concrete
	 * implementations of the interface methods).
	 */
	private List<FieldDetails> getEffectiveFields() {
		List<FieldDetails> fields = new ArrayList<>(classDetails.getFields());
		if (isSuperclassInterface()) {
			ClassDetails superClass = classDetails.getSuperClass();
			// Prepend parent fields so they appear before own fields
			fields.addAll(0, superClass.getFields());
		}
		return fields;
	}

	private <A extends Annotation> List<FieldDetails> getFieldsWithAnnotation(Class<A> annotationType) {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : getEffectiveFields()) {
			if (fieldHasAnnotation(field, annotationType)) {
				result.add(field);
			}
		}
		return result;
	}

	private boolean isPrimitiveType(String className) {
		return switch (className) {
			case "int", "long", "short", "byte", "char", "boolean", "float", "double" -> true;
			default -> false;
		};
	}

	private String boxPrimitive(String className) {
		return switch (className) {
			case "int" -> "java.lang.Integer";
			case "long" -> "java.lang.Long";
			case "short" -> "java.lang.Short";
			case "byte" -> "java.lang.Byte";
			case "char" -> "java.lang.Character";
			case "boolean" -> "java.lang.Boolean";
			case "float" -> "java.lang.Float";
			case "double" -> "java.lang.Double";
			default -> className;
		};
	}

	private static String capitalize(String name) {
		if (name == null || name.isEmpty()) return name;
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}
}
