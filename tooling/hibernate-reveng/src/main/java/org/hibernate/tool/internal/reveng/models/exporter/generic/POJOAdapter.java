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
package org.hibernate.tool.internal.reveng.models.exporter.generic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.internal.reveng.models.exporter.entity.ImportContext;
import org.hibernate.tool.internal.reveng.models.exporter.entity.ImportContextImpl;
import org.hibernate.tool.internal.reveng.models.exporter.entity.TemplateHelper;

/**
 * Adapter that wraps {@link ClassDetails} via {@link TemplateHelper}
 * to provide backwards compatibility with legacy FreeMarker templates
 * ({@code pojo/Pojo.ftl} and friends) that expect the old
 * {@code POJOClass} API.
 *
 * @author Koen Aers
 */
public class POJOAdapter {

	private final TemplateHelper helper;

	public POJOAdapter(ClassDetails classDetails, ModelsContext modelsContext,
					   Map<String, List<String>> classMetaAttributes,
					   Map<String, Map<String, List<String>>> fieldMetaAttributes) {
		ImportContext importContext = new ImportContextImpl(
				getPackageName(classDetails));
		this.helper = new TemplateHelper(
				classDetails, modelsContext, importContext, false,
				classMetaAttributes != null ? classMetaAttributes : Collections.emptyMap(),
				fieldMetaAttributes != null ? fieldMetaAttributes : Collections.emptyMap());
	}

	// --- Declaration ---

	public String getPackageDeclaration() {
		return helper.getPackageDeclaration();
	}

	public String getDeclarationName() {
		return helper.getDeclarationName();
	}

	public String getShortName() {
		return helper.getDeclarationName();
	}

	public String getDeclarationType() {
		return "class";
	}

	public String getClassModifiers() {
		return "public";
	}

	public String getExtendsDeclaration() {
		return helper.getExtendsDeclaration();
	}

	public String getImplementsDeclaration() {
		return helper.getImplementsDeclaration();
	}

	// --- Boolean queries ---

	public boolean isInterface() {
		return false;
	}

	public boolean isComponent() {
		return helper.isEmbeddable();
	}

	public boolean isSubclass() {
		return helper.isSubclass();
	}

	public boolean hasIdentifierProperty() {
		return helper.getBasicFields().stream()
				.anyMatch(helper::isPrimaryKey);
	}

	public boolean needsEqualsHashCode() {
		return helper.needsEqualsHashCode();
	}

	public boolean needsToString() {
		List<TemplateHelper.ToStringProperty> props = helper.getToStringProperties();
		return props != null && !props.isEmpty();
	}

	public boolean needsFullConstructor() {
		return helper.needsFullConstructor();
	}

	public boolean needsMinimalConstructor() {
		return helper.needsMinimalConstructor();
	}

	// --- Properties / fields ---

	public List<FieldDetails> getAllPropertiesIterator() {
		List<FieldDetails> all = new ArrayList<>();
		FieldDetails compositeId = helper.getCompositeIdField();
		if (compositeId != null) {
			all.add(compositeId);
		}
		all.addAll(helper.getBasicFields());
		all.addAll(helper.getManyToOneFields());
		all.addAll(helper.getOneToOneFields());
		all.addAll(helper.getOneToManyFields());
		all.addAll(helper.getManyToManyFields());
		all.addAll(helper.getEmbeddedFields());
		return all;
	}

	public List<PropertyAdapter> getPropertiesForMinimalConstructor() {
		return helper.getMinimalConstructorProperties().stream()
				.map(p -> new PropertyAdapter(p.fieldName(), p.typeName()))
				.toList();
	}

	public List<PropertyAdapter> getPropertiesForFullConstructor() {
		return helper.getFullConstructorProperties().stream()
				.map(p -> new PropertyAdapter(p.fieldName(), p.typeName()))
				.toList();
	}

	public List<PropertyAdapter> getPropertyClosureForMinimalConstructor() {
		return getPropertiesForMinimalConstructor();
	}

	public List<PropertyAdapter> getPropertyClosureForFullConstructor() {
		return getPropertiesForFullConstructor();
	}

	public List<PropertyAdapter> getPropertyClosureForSuperclassMinimalConstructor() {
		return helper.getSuperclassMinimalConstructorProperties().stream()
				.map(p -> new PropertyAdapter(p.fieldName(), p.typeName()))
				.toList();
	}

	public List<PropertyAdapter> getPropertyClosureForSuperclassFullConstructor() {
		return helper.getSuperclassFullConstructorProperties().stream()
				.map(p -> new PropertyAdapter(p.fieldName(), p.typeName()))
				.toList();
	}

	public List<ToStringAdapter> getToStringPropertiesIterator() {
		return helper.getToStringProperties().stream()
				.map(p -> new ToStringAdapter(p.fieldName(), p.getterName()))
				.toList();
	}

	// --- Type and naming ---

	public String getJavaTypeName(FieldDetails field, boolean jdk5) {
		return helper.getJavaTypeName(field);
	}

	public String getPropertyName(FieldDetails field) {
		String name = helper.getFieldName(field);
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	public String getGetterSignature(FieldDetails field) {
		return helper.getGetterName(field);
	}

	public String getGetterSignature(ToStringAdapter prop) {
		return prop.getGetterName();
	}

	public String getFieldModifiers(FieldDetails field) {
		return helper.getFieldModifiers(field);
	}

	public String getPropertyGetModifiers(FieldDetails field) {
		return helper.getPropertyGetModifiers(field);
	}

	public String getPropertySetModifiers(FieldDetails field) {
		return helper.getPropertySetModifiers(field);
	}

	// --- Field initialization ---

	public boolean hasFieldInitializor(FieldDetails field, boolean jdk5) {
		String init = helper.getCollectionInitializerType(field);
		return init != null && !init.isEmpty();
	}

	public String getFieldInitialization(FieldDetails field, boolean jdk5) {
		String init = helper.getCollectionInitializerType(field);
		return init != null ? "new " + init + "()" : "";
	}

	// --- JavaDoc ---

	public String getClassJavaDoc(String description, int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indent; i++) sb.append(" ");
		sb.append(" * ").append(description);
		return sb.toString();
	}

	public boolean hasFieldJavaDoc(FieldDetails field) {
		return helper.hasFieldDescription(field);
	}

	public String getFieldJavaDoc(FieldDetails field, int indent) {
		if (!helper.hasFieldDescription(field)) return "";
		String desc = helper.getFieldDescription(field);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indent; i++) sb.append(" ");
		sb.append(" * ").append(desc);
		return sb.toString();
	}

	// --- Meta attributes ---

	public boolean getMetaAttribAsBool(FieldDetails field, String attributeName, boolean defaultValue) {
		return helper.getFieldMetaAsBool(field, attributeName, defaultValue);
	}

	public boolean hasMetaAttribute(FieldDetails field, String attributeName) {
		return helper.hasFieldMetaAttribute(field, attributeName);
	}

	public boolean hasMetaAttribute(String attributeName) {
		return helper.hasClassMetaAttribute(attributeName);
	}

	// --- Code generation ---

	public String generateImports() {
		return helper.generateImports();
	}

	public String importType(String fqcn) {
		return helper.importType(fqcn);
	}

	public String getExtraClassCode() {
		return helper.getExtraClassCode();
	}

	public String generateEquals(String thisRef, String otherRef, boolean jdk5) {
		List<FieldDetails> fields = helper.getIdentifierFields();
		if (fields.isEmpty()) return "true";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < fields.size(); i++) {
			if (i > 0) sb.append("\n && ");
			sb.append(helper.generateEqualsExpression(fields.get(i)));
		}
		return sb.toString();
	}

	public String generateHashCode(FieldDetails field, String resultVar,
								   String thisRef, boolean jdk5) {
		return resultVar + " = 37 * " + resultVar + " + "
				+ helper.generateHashCodeExpression(field) + ";";
	}

	// Annotation generation stubs — only active when ejb3=true
	public String generateAnnIdGenerator() { return ""; }
	public String generateAnnColumnAnnotation(FieldDetails field) { return ""; }
	public String generateAnnTableUniqueConstraint() { return ""; }
	public String generateBasicAnnotation(FieldDetails field) { return ""; }
	public String generateOneToOneAnnotation(FieldDetails field, Object md) { return ""; }
	public String generateManyToOneAnnotation(FieldDetails field) { return ""; }
	public String generateCollectionAnnotation(FieldDetails field, Object md) { return ""; }
	public String generateJoinColumnsAnnotation(FieldDetails field, Object md) { return ""; }

	// --- Helpers ---

	private static String getPackageName(ClassDetails classDetails) {
		String className = classDetails.getClassName();
		if (className == null) return "";
		int lastDot = className.lastIndexOf('.');
		return lastDot > 0 ? className.substring(0, lastDot) : "";
	}

	/**
	 * Adapter for constructor property lists, providing a {@code name}
	 * property compatible with the old template API.
	 */
	public static class PropertyAdapter {
		private final String name;
		private final String typeName;

		PropertyAdapter(String name, String typeName) {
			this.name = name;
			this.typeName = typeName;
		}

		public String getName() { return name; }
		public String getTypeName() { return typeName; }
	}

	/**
	 * Adapter for toString property lists.
	 */
	public static class ToStringAdapter {
		private final String name;
		private final String getterName;

		ToStringAdapter(String name, String getterName) {
			this.name = name;
			this.getterName = getterName;
		}

		public String getName() { return name; }
		public String getGetterName() { return getterName; }
	}
}
