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
package org.hibernate.tool.internal.exporter.hbm;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.NaturalId;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;

/**
 * Handles field categorization for HBM template generation: composite ID,
 * basic fields, natural ID, version, relationship fields, embedded,
 * element collection, any, many-to-any, and dynamic component fields.
 *
 * @author Koen Aers
 */
class HbmFieldCategorizationHelper {

	private final ClassDetails classDetails;
	private final Map<String, Map<String, List<String>>> fieldMetaAttributes;

	HbmFieldCategorizationHelper(ClassDetails classDetails,
								  Map<String, Map<String, List<String>>> fieldMetaAttributes) {
		this.classDetails = classDetails;
		this.fieldMetaAttributes = fieldMetaAttributes;
	}

	private Map<String, List<String>> getFieldMetaAttributeMap(FieldDetails field) {
		return FieldMetaUtil.forField(fieldMetaAttributes, field);
	}

	// --- Composite ID ---

	FieldDetails getCompositeIdField() {
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(EmbeddedId.class)) {
				return field;
			}
		}
		return null;
	}

	String getCompositeIdClassName() {
		FieldDetails cid = getCompositeIdField();
		return cid != null ? cid.getType().determineRawClass().getClassName() : null;
	}

	boolean hasIdClass() {
		return classDetails.hasDirectAnnotationUsage(IdClass.class);
	}

	String getIdClassName() {
		IdClass idClass = classDetails.getDirectAnnotationUsage(IdClass.class);
		return idClass != null ? idClass.value().getName() : null;
	}

	List<FieldDetails> getCompositeIdAllFields() {
		FieldDetails cid = getCompositeIdField();
		if (cid == null) {
			return Collections.emptyList();
		}
		return new ArrayList<>(cid.getType().determineRawClass().getFields());
	}

	List<FieldDetails> getCompositeIdKeyProperties() {
		FieldDetails cid = getCompositeIdField();
		if (cid == null) {
			return Collections.emptyList();
		}
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : cid.getType().determineRawClass().getFields()) {
			if (!field.hasDirectAnnotationUsage(ManyToOne.class)) {
				result.add(field);
			}
		}
		return result;
	}

	boolean hasCompositeIdKeyManyToOnes() {
		return !getCompositeIdKeyManyToOnes().isEmpty();
	}

	List<FieldDetails> getCompositeIdKeyManyToOnes() {
		FieldDetails cid = getCompositeIdField();
		if (cid == null) {
			return Collections.emptyList();
		}
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : cid.getType().determineRawClass().getFields()) {
			if (field.hasDirectAnnotationUsage(ManyToOne.class)) {
				result.add(field);
			}
		}
		return result;
	}

	String getKeyManyToOneClassName(FieldDetails field) {
		return field.getType().determineRawClass().getClassName();
	}

	String getKeyManyToOneColumnName(FieldDetails field) {
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		return jc != null ? jc.name() : field.getName();
	}

	List<String> getKeyManyToOneColumnNames(FieldDetails field) {
		List<String> result = new ArrayList<>();
		JoinColumn single = field.getDirectAnnotationUsage(JoinColumn.class);
		if (single != null) {
			result.add(single.name());
		}
		JoinColumns container = field.getDirectAnnotationUsage(JoinColumns.class);
		if (container != null) {
			for (JoinColumn jc : container.value()) {
				result.add(jc.name());
			}
		}
		if (result.isEmpty()) {
			result.add(field.getName());
		}
		return result;
	}

	// --- Field lists ---

	List<FieldDetails> getIdFields() {
		return getFieldsWithAnnotation(Id.class);
	}

	List<FieldDetails> getBasicFields() {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			if (!isRelationshipField(field) && !isEmbeddedField(field)
					&& !field.hasDirectAnnotationUsage(EmbeddedId.class)
					&& !field.hasDirectAnnotationUsage(Id.class)
					&& !field.hasDirectAnnotationUsage(Version.class)
					&& !field.hasDirectAnnotationUsage(Any.class)
					&& !field.hasDirectAnnotationUsage(ElementCollection.class)
					&& !field.hasDirectAnnotationUsage(ManyToAny.class)
					&& !field.hasDirectAnnotationUsage(NaturalId.class)
					&& !isSecondaryTableField(field)
					&& !isInPropertiesGroup(field)) {
				result.add(field);
			}
		}
		return result;
	}

	List<FieldDetails> getNaturalIdFields() {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(NaturalId.class)) {
				result.add(field);
			}
		}
		return result;
	}

	boolean isNaturalIdMutable() {
		for (FieldDetails field : classDetails.getFields()) {
			NaturalId nid = field.getDirectAnnotationUsage(NaturalId.class);
			if (nid != null) {
				return nid.mutable();
			}
		}
		return false;
	}

	List<FieldDetails> getVersionFields() {
		return getFieldsWithAnnotation(Version.class);
	}

	List<FieldDetails> getManyToOneFields() {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(ManyToOne.class)
					&& !field.hasDirectAnnotationUsage(Id.class)
					&& !isInPropertiesGroup(field)) {
				result.add(field);
			}
		}
		return result;
	}

	List<FieldDetails> getOneToOneFields() {
		boolean hasCompositeId = getCompositeIdField() != null;
		if (!hasCompositeId) {
			return getFieldsWithAnnotation(OneToOne.class);
		}
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(OneToOne.class)
					&& !isOneToOneConstrained(field)) {
				result.add(field);
			}
		}
		return result;
	}

	List<FieldDetails> getConstrainedOneToOneAsM2OFields() {
		if (getCompositeIdField() == null) {
			return Collections.emptyList();
		}
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(OneToOne.class)
					&& isOneToOneConstrained(field)) {
				result.add(field);
			}
		}
		return result;
	}

	List<FieldDetails> getOneToManyFields() {
		return getFieldsWithAnnotation(OneToMany.class);
	}

	List<FieldDetails> getManyToManyFields() {
		return getFieldsWithAnnotation(ManyToMany.class);
	}

	List<FieldDetails> getEmbeddedFields() {
		return getFieldsWithAnnotation(Embedded.class);
	}

	List<FieldDetails> getAnyFields() {
		return getFieldsWithAnnotation(Any.class);
	}

	List<FieldDetails> getElementCollectionFields() {
		return getFieldsWithAnnotation(ElementCollection.class);
	}

	List<FieldDetails> getManyToAnyFields() {
		return getFieldsWithAnnotation(ManyToAny.class);
	}

	// --- Dynamic component ---

	List<FieldDetails> getDynamicComponentFields() {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			Map<String, List<String>> fieldMeta = getFieldMetaAttributeMap(field);
			List<String> dynComp = fieldMeta.get("hibernate.dynamic-component");
			if (dynComp != null && !dynComp.isEmpty() && "true".equals(dynComp.get(0))) {
				result.add(field);
			}
		}
		return result;
	}

	List<HbmTemplateHelper.DynamicComponentProperty> getDynamicComponentProperties(
			FieldDetails field) {
		List<HbmTemplateHelper.DynamicComponentProperty> result = new ArrayList<>();
		Map<String, List<String>> fieldMeta = getFieldMetaAttributeMap(field);
		String prefix = "hibernate.dynamic-component.property:";
		for (Map.Entry<String, List<String>> entry : fieldMeta.entrySet()) {
			if (entry.getKey().startsWith(prefix)) {
				String propName = entry.getKey().substring(prefix.length());
				String typeName = entry.getValue().get(0);
				result.add(new HbmTemplateHelper.DynamicComponentProperty(propName, typeName));
			}
		}
		return result;
	}

	// --- Field type checks ---

	boolean isBasicField(FieldDetails field) {
		return !isRelationshipField(field) && !isEmbeddedField(field)
				&& !field.hasDirectAnnotationUsage(EmbeddedId.class)
				&& !field.hasDirectAnnotationUsage(Id.class)
				&& !field.hasDirectAnnotationUsage(Version.class)
				&& !field.hasDirectAnnotationUsage(Any.class)
				&& !field.hasDirectAnnotationUsage(ElementCollection.class)
				&& !field.hasDirectAnnotationUsage(ManyToAny.class);
	}

	boolean isManyToOneField(FieldDetails field) {
		return field.hasDirectAnnotationUsage(ManyToOne.class);
	}

	// --- Private helpers ---

	private boolean isRelationshipField(FieldDetails field) {
		return field.hasDirectAnnotationUsage(ManyToOne.class)
				|| field.hasDirectAnnotationUsage(OneToMany.class)
				|| field.hasDirectAnnotationUsage(OneToOne.class)
				|| field.hasDirectAnnotationUsage(ManyToMany.class);
	}

	private boolean isEmbeddedField(FieldDetails field) {
		return field.hasDirectAnnotationUsage(Embedded.class);
	}

	private boolean isSecondaryTableField(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null && col.table() != null && !col.table().isEmpty();
	}

	private boolean isInPropertiesGroup(FieldDetails field) {
		Map<String, List<String>> attrs = fieldMetaAttributes.getOrDefault(
				field.getName(), Collections.emptyMap());
		return attrs.containsKey("hibernate.properties-group");
	}

	private boolean isOneToOneConstrained(FieldDetails field) {
		return field.hasDirectAnnotationUsage(JoinColumn.class)
				|| field.hasDirectAnnotationUsage(JoinColumns.class);
	}

	private <A extends Annotation> List<FieldDetails> getFieldsWithAnnotation(
			Class<A> annotationType) {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(annotationType)) {
				result.add(field);
			}
		}
		return result;
	}
}
