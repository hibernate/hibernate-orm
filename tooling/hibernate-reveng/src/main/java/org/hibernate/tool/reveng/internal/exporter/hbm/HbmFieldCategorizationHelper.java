/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.hbm;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CollectionTable;
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
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.NaturalId;

import org.hibernate.tool.reveng.internal.util.CascadeUtil;
import org.hibernate.tool.reveng.internal.util.TypeHelper;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.TypeDetails;

/**
 * Handles field categorization for HBM template generation: composite ID,
 * basic fields, natural ID, version, relationship fields, embedded,
 * element collection, any, many-to-any, and dynamic component fields.
 *
 * @author Koen Aers
 */
public class HbmFieldCategorizationHelper {

	private final ClassDetails classDetails;
	private final Map<String, Map<String, List<String>>> fieldMetaAttributes;
	private final Map<String, List<String>> metaAttributes;

	HbmFieldCategorizationHelper(ClassDetails classDetails,
								Map<String, Map<String, List<String>>> fieldMetaAttributes,
								Map<String, List<String>> metaAttributes) {
		this.classDetails = classDetails;
		this.fieldMetaAttributes = fieldMetaAttributes;
		this.metaAttributes = metaAttributes;
	}

	private Map<String, List<String>> getFieldMetaAttributeMap(FieldDetails field) {
		return FieldMetaUtil.forField(fieldMetaAttributes, field);
	}

	// --- Composite ID ---

	public FieldDetails getCompositeIdField() {
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(EmbeddedId.class)) {
				return field;
			}
		}
		return null;
	}

	public String getCompositeIdClassName() {
		FieldDetails cid = getCompositeIdField();
		return cid != null ? cid.getType().determineRawClass().getClassName() : null;
	}

	public boolean hasIdClass() {
		return classDetails.hasDirectAnnotationUsage(IdClass.class);
	}

	public String getIdClassName() {
		IdClass idClass = classDetails.getDirectAnnotationUsage(IdClass.class);
		return idClass != null ? idClass.value().getName() : null;
	}

	public List<FieldDetails> getCompositeIdAllFields() {
		FieldDetails cid = getCompositeIdField();
		if (cid == null) {
			return Collections.emptyList();
		}
		return new ArrayList<>(cid.getType().determineRawClass().getFields());
	}

	public List<FieldDetails> getCompositeIdKeyProperties() {
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

	public boolean hasCompositeIdKeyManyToOnes() {
		return !getCompositeIdKeyManyToOnes().isEmpty();
	}

	public List<FieldDetails> getCompositeIdKeyManyToOnes() {
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

	public String getKeyManyToOneClassName(FieldDetails field) {
		return field.getType().determineRawClass().getClassName();
	}

	public String getKeyManyToOneColumnName(FieldDetails field) {
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		return jc != null ? jc.name() : field.getName();
	}

	public List<String> getKeyManyToOneColumnNames(FieldDetails field) {
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

	public List<FieldDetails> getIdFields() {
		return getFieldsWithAnnotation(Id.class);
	}

	public List<FieldDetails> getBasicFields() {
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

	public List<FieldDetails> getNaturalIdFields() {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(NaturalId.class)) {
				result.add(field);
			}
		}
		return result;
	}

	public boolean isNaturalIdMutable() {
		for (FieldDetails field : classDetails.getFields()) {
			NaturalId nid = field.getDirectAnnotationUsage(NaturalId.class);
			if (nid != null) {
				return nid.mutable();
			}
		}
		return false;
	}

	public List<FieldDetails> getVersionFields() {
		return getFieldsWithAnnotation(Version.class);
	}

	public List<FieldDetails> getManyToOneFields() {
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

	public List<FieldDetails> getOneToOneFields() {
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

	public List<FieldDetails> getConstrainedOneToOneAsM2OFields() {
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

	public List<FieldDetails> getOneToManyFields() {
		return getFieldsWithAnnotation(OneToMany.class);
	}

	public List<FieldDetails> getManyToManyFields() {
		return getFieldsWithAnnotation(ManyToMany.class);
	}

	public List<FieldDetails> getEmbeddedFields() {
		return getFieldsWithAnnotation(Embedded.class);
	}

	public List<FieldDetails> getAnyFields() {
		return getFieldsWithAnnotation(Any.class);
	}

	public List<FieldDetails> getElementCollectionFields() {
		return getFieldsWithAnnotation(ElementCollection.class);
	}

	public List<FieldDetails> getManyToAnyFields() {
		return getFieldsWithAnnotation(ManyToAny.class);
	}

	// --- Dynamic component ---

	public List<FieldDetails> getDynamicComponentFields() {
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

	public List<HbmTemplateHelper.DynamicComponentProperty> getDynamicComponentProperties(
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

	public boolean isBasicField(FieldDetails field) {
		return !isRelationshipField(field) && !isEmbeddedField(field)
				&& !field.hasDirectAnnotationUsage(EmbeddedId.class)
				&& !field.hasDirectAnnotationUsage(Id.class)
				&& !field.hasDirectAnnotationUsage(Version.class)
				&& !field.hasDirectAnnotationUsage(Any.class)
				&& !field.hasDirectAnnotationUsage(ElementCollection.class)
				&& !field.hasDirectAnnotationUsage(ManyToAny.class);
	}

	public boolean isManyToOneField(FieldDetails field) {
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

	private String getClassMetaValue(String key) {
		List<String> values = metaAttributes.get(key);
		return values != null && !values.isEmpty() ? values.get(0) : null;
	}

	// --- ElementCollection ---

	public boolean isElementCollectionOfEmbeddable(FieldDetails field) {
		TypeDetails elementType = field.getElementType();
		if (elementType == null) return false;
		ClassDetails rawClass = elementType.determineRawClass();
		return rawClass.hasDirectAnnotationUsage(jakarta.persistence.Embeddable.class);
	}

	public String getElementCollectionTableName(FieldDetails field) {
		CollectionTable ct = field.getDirectAnnotationUsage(CollectionTable.class);
		return ct != null && ct.name() != null && !ct.name().isEmpty() ? ct.name() : null;
	}

	public String getElementCollectionTableSchema(FieldDetails field) {
		CollectionTable ct = field.getDirectAnnotationUsage(CollectionTable.class);
		return ct != null && ct.schema() != null && !ct.schema().isEmpty() ? ct.schema() : null;
	}

	public String getElementCollectionTableCatalog(FieldDetails field) {
		CollectionTable ct = field.getDirectAnnotationUsage(CollectionTable.class);
		return ct != null && ct.catalog() != null && !ct.catalog().isEmpty() ? ct.catalog() : null;
	}

	public String getElementCollectionKeyColumnName(FieldDetails field) {
		CollectionTable ct = field.getDirectAnnotationUsage(CollectionTable.class);
		if (ct != null && ct.joinColumns() != null && ct.joinColumns().length > 0) {
			return ct.joinColumns()[0].name();
		}
		return null;
	}

	public String getElementCollectionElementType(FieldDetails field) {
		TypeDetails elementType = field.getElementType();
		if (elementType != null) {
			return TypeHelper.toHibernateType(elementType.determineRawClass().getClassName());
		}
		return null;
	}

	public String getElementCollectionElementColumnName(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null && col.name() != null && !col.name().isEmpty() ? col.name() : null;
	}

	// --- Composite element (embeddable inside ElementCollection) ---

	private ClassDetails resolveCompositeClass(FieldDetails field) {
		if (field.hasDirectAnnotationUsage(ElementCollection.class)) {
			TypeDetails elementType = field.getElementType();
			return elementType != null ? elementType.determineRawClass() : null;
		}
		if (field.hasDirectAnnotationUsage(Embedded.class)) {
			return field.getType().determineRawClass();
		}
		return null;
	}

	public List<FieldDetails> getCompositeElementProperties(FieldDetails field) {
		ClassDetails embeddable = resolveCompositeClass(field);
		if (embeddable == null) return Collections.emptyList();
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails f : embeddable.getFields()) {
			if (!f.hasDirectAnnotationUsage(ManyToOne.class) &&
					!f.hasDirectAnnotationUsage(Embedded.class) &&
					!f.hasDirectAnnotationUsage(OneToMany.class) &&
					!f.hasDirectAnnotationUsage(ManyToMany.class) &&
					!f.hasDirectAnnotationUsage(ElementCollection.class)) {
				result.add(f);
			}
		}
		return result;
	}

	public List<FieldDetails> getCompositeElementManyToOnes(FieldDetails field) {
		ClassDetails embeddable = resolveCompositeClass(field);
		if (embeddable == null) return Collections.emptyList();
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails f : embeddable.getFields()) {
			if (f.hasDirectAnnotationUsage(ManyToOne.class)) {
				result.add(f);
			}
		}
		return result;
	}

	public List<FieldDetails> getCompositeElementEmbeddeds(FieldDetails field) {
		ClassDetails embeddable = resolveCompositeClass(field);
		if (embeddable == null) return Collections.emptyList();
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails f : embeddable.getFields()) {
			if (f.hasDirectAnnotationUsage(Embedded.class)) {
				result.add(f);
			}
		}
		return result;
	}

	// --- Any ---

	public String getAnyIdType(FieldDetails field) {
		AnyKeyJavaClass akjc = field.getDirectAnnotationUsage(AnyKeyJavaClass.class);
		if (akjc != null) {
			return TypeHelper.toHibernateType(akjc.value().getName());
		}
		return "long";
	}

	public String getAnyMetaType(FieldDetails field) {
		AnyDiscriminator ad = field.getDirectAnnotationUsage(AnyDiscriminator.class);
		if (ad == null) return "string";
		return switch (ad.value()) {
			case STRING -> "string";
			case CHAR -> "character";
			case INTEGER -> "integer";
		};
	}

	public List<HbmTemplateHelper.AnyMetaValue> getAnyMetaValues(FieldDetails field) {
		List<HbmTemplateHelper.AnyMetaValue> result = new ArrayList<>();
		Map<String, List<String>> fieldMeta = FieldMetaUtil.forField(fieldMetaAttributes, field);
		AnyDiscriminatorValue single = field.getDirectAnnotationUsage(AnyDiscriminatorValue.class);
		if (single != null) {
			String entityName = resolveAnyEntityClass(single, fieldMeta);
			result.add(new HbmTemplateHelper.AnyMetaValue(single.discriminator(), entityName));
		}
		AnyDiscriminatorValues container = field.getDirectAnnotationUsage(AnyDiscriminatorValues.class);
		if (container != null) {
			for (AnyDiscriminatorValue adv : container.value()) {
				String entityName = resolveAnyEntityClass(adv, fieldMeta);
				result.add(new HbmTemplateHelper.AnyMetaValue(adv.discriminator(), entityName));
			}
		}
		return result;
	}

	private String resolveAnyEntityClass(AnyDiscriminatorValue adv,
										Map<String, List<String>> fieldMeta) {
		List<String> metaClassName = fieldMeta.get("hibernate.any.meta-value:" + adv.discriminator());
		if (metaClassName != null && !metaClassName.isEmpty()) {
			return metaClassName.get(0);
		}
		return adv.entity().getName();
	}

	public String getAnyCascadeString(FieldDetails field) {
		Cascade cascade = field.getDirectAnnotationUsage(Cascade.class);
		if (cascade == null || cascade.value().length == 0) return null;
		return CascadeUtil.formatHibernateCascade(cascade);
	}

	// --- Properties groups (<properties> element) ---

	public List<HbmTemplateHelper.PropertiesGroupInfo> getPropertiesGroups() {
		List<String> groupNames = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			Map<String, List<String>> attrs = fieldMetaAttributes.getOrDefault(
					field.getName(), Collections.emptyMap());
			List<String> groups = attrs.get("hibernate.properties-group");
			if (groups != null && !groups.isEmpty()) {
				String groupName = groups.get(0);
				if (!groupNames.contains(groupName)) {
					groupNames.add(groupName);
				}
			}
		}
		List<HbmTemplateHelper.PropertiesGroupInfo> result = new ArrayList<>();
		for (String groupName : groupNames) {
			List<FieldDetails> fields = new ArrayList<>();
			for (FieldDetails field : classDetails.getFields()) {
				Map<String, List<String>> attrs = fieldMetaAttributes.getOrDefault(
						field.getName(), Collections.emptyMap());
				List<String> groups = attrs.get("hibernate.properties-group");
				if (groups != null && groups.contains(groupName)) {
					fields.add(field);
				}
			}
			boolean unique = "true".equals(getClassMetaValue("hibernate.properties-group." + groupName + ".unique"));
			boolean insert = !"false".equals(getClassMetaValue("hibernate.properties-group." + groupName + ".insert"));
			boolean update = !"false".equals(getClassMetaValue("hibernate.properties-group." + groupName + ".update"));
			boolean optimisticLock = !"false".equals(getClassMetaValue("hibernate.properties-group." + groupName + ".optimistic-lock"));
			result.add(new HbmTemplateHelper.PropertiesGroupInfo(groupName, unique, insert, update, optimisticLock, fields));
		}
		return result;
	}
}
