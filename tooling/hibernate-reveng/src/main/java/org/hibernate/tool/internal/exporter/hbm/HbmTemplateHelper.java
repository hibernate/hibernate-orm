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

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Version;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.Parameter;

import org.hibernate.tool.internal.util.TypeHelper;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.TypeDetails;

/**
 * Wraps a {@link ClassDetails} and provides template-friendly methods
 * for generating Hibernate hbm.xml mapping files.
 *
 * @author Koen Aers
 */
public class HbmTemplateHelper {

	private final ClassDetails classDetails;
	private final String comment;
	private final Map<String, List<String>> metaAttributes;
	private final Map<String, String> imports;
	private final Map<String, Map<String, List<String>>> fieldMetaAttributes;
	private final Map<String, Map<String, List<String>>> allClassMetaAttributes;
	private final HbmQueryAndFilterHelper queryAndFilterHelper;
	private final HbmCollectionAttributeHelper collectionAttributeHelper;
	private final HbmClassInfoHelper classInfoHelper;

	HbmTemplateHelper(ClassDetails classDetails) {
		this(classDetails, null, Collections.emptyMap(), Collections.emptyMap(),
				Collections.emptyMap(), Collections.emptyMap());
	}

	HbmTemplateHelper(ClassDetails classDetails, String comment,
					   Map<String, List<String>> metaAttributes) {
		this(classDetails, comment, metaAttributes, Collections.emptyMap(),
				Collections.emptyMap(), Collections.emptyMap());
	}

	HbmTemplateHelper(ClassDetails classDetails, String comment,
					   Map<String, List<String>> metaAttributes,
					   Map<String, String> imports) {
		this(classDetails, comment, metaAttributes, imports,
				Collections.emptyMap(), Collections.emptyMap());
	}

	HbmTemplateHelper(ClassDetails classDetails, String comment,
					   Map<String, List<String>> metaAttributes,
					   Map<String, String> imports,
					   Map<String, Map<String, List<String>>> fieldMetaAttributes) {
		this(classDetails, comment, metaAttributes, imports,
				fieldMetaAttributes, Collections.emptyMap());
	}

	HbmTemplateHelper(ClassDetails classDetails, String comment,
					   Map<String, List<String>> metaAttributes,
					   Map<String, String> imports,
					   Map<String, Map<String, List<String>>> fieldMetaAttributes,
					   Map<String, Map<String, List<String>>> allClassMetaAttributes) {
		this.classDetails = classDetails;
		this.comment = comment;
		this.metaAttributes = metaAttributes != null ? metaAttributes : Collections.emptyMap();
		this.imports = imports != null ? imports : Collections.emptyMap();
		this.fieldMetaAttributes = fieldMetaAttributes != null ? fieldMetaAttributes : Collections.emptyMap();
		this.allClassMetaAttributes = allClassMetaAttributes != null ? allClassMetaAttributes : Collections.emptyMap();
		this.queryAndFilterHelper = new HbmQueryAndFilterHelper(classDetails, this.metaAttributes);
		this.collectionAttributeHelper = new HbmCollectionAttributeHelper(this.fieldMetaAttributes);
		this.classInfoHelper = new HbmClassInfoHelper(classDetails, comment, this.metaAttributes);
	}

	// --- Entity / class ---

	public String getClassName() {
		return classInfoHelper.getClassName();
	}

	public String getPackageName() {
		return classInfoHelper.getPackageName();
	}

	// --- Table ---

	public String getTableName() {
		return classInfoHelper.getTableName();
	}

	public String getSchema() {
		return classInfoHelper.getSchema();
	}

	public String getCatalog() {
		return classInfoHelper.getCatalog();
	}

	public String getComment() {
		return classInfoHelper.getComment();
	}

	// --- Class-level attributes ---

	public boolean isMutable() {
		return classInfoHelper.isMutable();
	}

	public boolean isDynamicUpdate() {
		return classInfoHelper.isDynamicUpdate();
	}

	public boolean isDynamicInsert() {
		return classInfoHelper.isDynamicInsert();
	}

	public int getBatchSize() {
		return classInfoHelper.getBatchSize();
	}

	public String getCacheUsage() {
		return classInfoHelper.getCacheUsage();
	}

	public String getCacheRegion() {
		return classInfoHelper.getCacheRegion();
	}

	public String getCacheInclude() {
		return classInfoHelper.getCacheInclude();
	}

	public String getWhere() {
		return classInfoHelper.getWhere();
	}

	public boolean isAbstract() {
		return classInfoHelper.isAbstract();
	}

	public String getOptimisticLockMode() {
		return classInfoHelper.getOptimisticLockMode();
	}

	public String getRowId() {
		return classInfoHelper.getRowId();
	}

	public String getSubselect() {
		return classInfoHelper.getSubselect();
	}

	public boolean isConcreteProxy() {
		return classInfoHelper.isConcreteProxy();
	}

	public String getProxy() {
		return classInfoHelper.getProxy();
	}

	public String getEntityName() {
		return classInfoHelper.getEntityName();
	}

	// --- Inheritance ---

	public boolean isSubclass() {
		return classInfoHelper.isSubclass();
	}

	public String getParentClassName() {
		return classInfoHelper.getParentClassName();
	}

	public String getClassTag() {
		return classInfoHelper.getClassTag();
	}

	public boolean needsDiscriminator() {
		return classInfoHelper.needsDiscriminator();
	}

	public String getDiscriminatorColumnName() {
		return classInfoHelper.getDiscriminatorColumnName();
	}

	public String getDiscriminatorTypeName() {
		return classInfoHelper.getDiscriminatorTypeName();
	}

	public int getDiscriminatorColumnLength() {
		return classInfoHelper.getDiscriminatorColumnLength();
	}

	public String getDiscriminatorValue() {
		return classInfoHelper.getDiscriminatorValue();
	}

	public String getPrimaryKeyJoinColumnName() {
		return classInfoHelper.getPrimaryKeyJoinColumnName();
	}

	// --- Field categorization ---

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
		// Exclude constrained O2O with composite PK — rendered as many-to-one
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(OneToOne.class)
					&& !isOneToOneConstrained(field)) {
				result.add(field);
			}
		}
		return result;
	}

	/**
	 * Returns constrained one-to-one fields that need composite FK column
	 * mapping. These are rendered as many-to-one with unique="true" in HBM XML
	 * because one-to-one constrained doesn't support explicit composite column
	 * lists.
	 */
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

	public List<DynamicComponentProperty> getDynamicComponentProperties(FieldDetails field) {
		List<DynamicComponentProperty> result = new ArrayList<>();
		Map<String, List<String>> fieldMeta = getFieldMetaAttributeMap(field);
		String prefix = "hibernate.dynamic-component.property:";
		for (Map.Entry<String, List<String>> entry : fieldMeta.entrySet()) {
			if (entry.getKey().startsWith(prefix)) {
				String propName = entry.getKey().substring(prefix.length());
				String typeName = entry.getValue().get(0);
				result.add(new DynamicComponentProperty(propName, typeName));
			}
		}
		return result;
	}

	public record DynamicComponentProperty(String name, String type) {}

	// --- ElementCollection ---

	public boolean isElementCollectionOfEmbeddable(FieldDetails field) {
		TypeDetails elementType = field.getElementType();
		if (elementType == null) {
			return false;
		}
		ClassDetails rawClass = elementType.determineRawClass();
		return rawClass.hasDirectAnnotationUsage(jakarta.persistence.Embeddable.class);
	}

	public String getElementCollectionTableName(FieldDetails field) {
		CollectionTable ct = field.getDirectAnnotationUsage(CollectionTable.class);
		return ct != null && ct.name() != null && !ct.name().isEmpty() ? ct.name() : null;
	}

	public String getElementCollectionTableSchema(FieldDetails field) {
		CollectionTable ct = field.getDirectAnnotationUsage(CollectionTable.class);
		return ct != null && ct.schema() != null && !ct.schema().isEmpty()
				? ct.schema() : null;
	}

	public String getElementCollectionTableCatalog(FieldDetails field) {
		CollectionTable ct = field.getDirectAnnotationUsage(CollectionTable.class);
		return ct != null && ct.catalog() != null && !ct.catalog().isEmpty()
				? ct.catalog() : null;
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
			return TypeHelper.toHibernateType(
					elementType.determineRawClass().getClassName());
		}
		return null;
	}

	public String getElementCollectionElementColumnName(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null && col.name() != null && !col.name().isEmpty() ? col.name() : null;
	}

	// --- Composite element (embeddable inside ElementCollection) ---

	/**
	 * Returns the ClassDetails of the embeddable class for a composite element
	 * (from @ElementCollection) or nested composite (from @Embedded).
	 */
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

	/**
	 * Returns basic property fields of a composite element's embeddable class.
	 */
	public List<FieldDetails> getCompositeElementProperties(FieldDetails field) {
		ClassDetails embeddable = resolveCompositeClass(field);
		if (embeddable == null) {
			return Collections.emptyList();
		}
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

	/**
	 * Returns many-to-one fields of a composite element's embeddable class.
	 */
	public List<FieldDetails> getCompositeElementManyToOnes(FieldDetails field) {
		ClassDetails embeddable = resolveCompositeClass(field);
		if (embeddable == null) {
			return Collections.emptyList();
		}
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails f : embeddable.getFields()) {
			if (f.hasDirectAnnotationUsage(ManyToOne.class)) {
				result.add(f);
			}
		}
		return result;
	}

	/**
	 * Returns embedded fields (nested composite elements) of a composite element's
	 * embeddable class.
	 */
	public List<FieldDetails> getCompositeElementEmbeddeds(FieldDetails field) {
		ClassDetails embeddable = resolveCompositeClass(field);
		if (embeddable == null) {
			return Collections.emptyList();
		}
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails f : embeddable.getFields()) {
			if (f.hasDirectAnnotationUsage(Embedded.class)) {
				result.add(f);
			}
		}
		return result;
	}

	/**
	 * Returns the cascade string for a many-to-one field (used inside composite elements).
	 */
	public String getManyToOneCascadeString(FieldDetails field) {
		ManyToOne m2o = field.getDirectAnnotationUsage(ManyToOne.class);
		if (m2o != null && m2o.cascade().length > 0) {
			return getCascadeString(m2o.cascade());
		}
		Cascade cascade = field.getDirectAnnotationUsage(Cascade.class);
		if (cascade != null && cascade.value().length > 0) {
			return getAnyCascadeString(cascade);
		}
		return null;
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
		if (ad == null) {
			return "string";
		}
		return switch (ad.value()) {
			case STRING -> "string";
			case CHAR -> "character";
			case INTEGER -> "integer";
		};
	}

	public List<AnyMetaValue> getAnyMetaValues(FieldDetails field) {
		List<AnyMetaValue> result = new ArrayList<>();
		Map<String, List<String>> fieldMeta = getFieldMetaAttributeMap(field);
		AnyDiscriminatorValue single = field.getDirectAnnotationUsage(AnyDiscriminatorValue.class);
		if (single != null) {
			String entityName = resolveAnyEntityClass(single, fieldMeta);
			result.add(new AnyMetaValue(single.discriminator(), entityName));
		}
		AnyDiscriminatorValues container = field.getDirectAnnotationUsage(AnyDiscriminatorValues.class);
		if (container != null) {
			for (AnyDiscriminatorValue adv : container.value()) {
				String entityName = resolveAnyEntityClass(adv, fieldMeta);
				result.add(new AnyMetaValue(adv.discriminator(), entityName));
			}
		}
		return result;
	}

	private String resolveAnyEntityClass(AnyDiscriminatorValue adv,
										  Map<String, List<String>> fieldMeta) {
		// Check meta attribute first (stores original class name when class not on classpath)
		List<String> metaClassName = fieldMeta.get("hibernate.any.meta-value:" + adv.discriminator());
		if (metaClassName != null && !metaClassName.isEmpty()) {
			return metaClassName.get(0);
		}
		return adv.entity().getName();
	}

	public record AnyMetaValue(String value, String entityClass) {}

	public String getAnyCascadeString(FieldDetails field) {
		Cascade cascade = field.getDirectAnnotationUsage(Cascade.class);
		if (cascade == null || cascade.value().length == 0) {
			return null;
		}
		return getAnyCascadeString(cascade);
	}

	private String getAnyCascadeString(Cascade cascade) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cascade.value().length; i++) {
			if (i > 0) sb.append(", ");
			sb.append(toHbmHibernateCascade(cascade.value()[i]));
		}
		return sb.toString();
	}

	private String toHbmHibernateCascade(org.hibernate.annotations.CascadeType cascadeType) {
		return switch (cascadeType) {
			case ALL -> "all";
			case PERSIST -> "persist";
			case MERGE -> "merge";
			case REMOVE -> "delete";
			case REFRESH -> "refresh";
			case DETACH -> "evict";
			case LOCK -> "lock";
			case REPLICATE -> "replicate";
			case DELETE_ORPHAN -> "delete-orphan";
		};
	}

	public String getArrayElementClass(FieldDetails field) {
		Map<String, List<String>> fieldMeta = getFieldMetaAttributeMap(field);
		List<String> ec = fieldMeta.get("hibernate.array.element-class");
		return (ec != null && !ec.isEmpty()) ? ec.get(0) : null;
	}

	public String getManyToAnyFkColumnName(FieldDetails field) {
		Map<String, List<String>> fieldMeta = getFieldMetaAttributeMap(field);
		List<String> fkCol = fieldMeta.get("hibernate.any.fk.column");
		return (fkCol != null && !fkCol.isEmpty()) ? fkCol.get(0) : null;
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

	// --- Generator parameters ---

	public Map<String, String> getGeneratorParameters(FieldDetails field) {
		Map<String, String> params = new java.util.LinkedHashMap<>();
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
			// Fallback to meta-based generator params (e.g. foreign generator)
			params = getGeneratorParametersFromMeta(field);
		}
		return params;
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
		// Check for type name stored as field meta attribute (from hbm.xml <type> element)
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
		// Check for type params stored as field meta attributes
		return !getTypeParametersFromMeta(field).isEmpty();
	}

	public Map<String, String> getTypeParameters(FieldDetails field) {
		Type typeAnn = field.getDirectAnnotationUsage(Type.class);
		if (typeAnn != null && typeAnn.parameters() != null && typeAnn.parameters().length > 0) {
			Map<String, String> params = new java.util.LinkedHashMap<>();
			for (Parameter param : typeAnn.parameters()) {
				params.put(param.name(), param.value());
			}
			return params;
		}
		return getTypeParametersFromMeta(field);
	}

	private Map<String, String> getTypeParametersFromMeta(FieldDetails field) {
		Map<String, List<String>> fieldMeta = getFieldMetaAttributeMap(field);
		Map<String, String> params = new java.util.LinkedHashMap<>();
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
		Map<String, String> params = new java.util.LinkedHashMap<>();
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

	private Map<String, List<String>> getFieldMetaAttributeMap(FieldDetails field) {
		return fieldMetaAttributes.getOrDefault(field.getName(), Collections.emptyMap());
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

	public String getGeneratorClass(FieldDetails field) {
		// Check meta attribute first (preserves original hbm.xml generator class like "foreign")
		Map<String, List<String>> fieldMeta = getFieldMetaAttributeMap(field);
		List<String> genClassMeta = fieldMeta.get("hibernate.generator.class");
		if (genClassMeta != null && !genClassMeta.isEmpty()) {
			return genClassMeta.get(0);
		}
		GeneratedValue gv = field.getDirectAnnotationUsage(GeneratedValue.class);
		return toGeneratorClass(gv != null ? gv.strategy() : null);
	}

	// --- ManyToOne ---

	public String getTargetEntityName(FieldDetails field) {
		return field.getType().determineRawClass().getClassName();
	}

	/**
	 * Returns true if the many-to-one target is referenced by entity-name
	 * (i.e., the target ClassDetails uses an entity-name that differs from
	 * its Java class name).
	 */
	public boolean isManyToOneEntityNameRef(FieldDetails field) {
		String targetClassName = field.getType().determineRawClass().getClassName();
		Map<String, List<String>> targetMeta = allClassMetaAttributes.get(targetClassName);
		if (targetMeta != null) {
			List<String> realClass = targetMeta.get("hibernate.class-name");
			if (realClass != null && !realClass.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the entity-name for a many-to-one target, or null if the target
	 * is not entity-name based.
	 */
	public String getManyToOneEntityName(FieldDetails field) {
		if (!isManyToOneEntityNameRef(field)) {
			return null;
		}
		// The ClassDetails className holds the entity-name-based identity;
		// extract the simple name as entity-name
		String identityName = field.getType().determineRawClass().getClassName();
		int dot = identityName.lastIndexOf('.');
		return dot >= 0 ? identityName.substring(dot + 1) : identityName;
	}

	public List<String> getManyToOneFormulas(FieldDetails field) {
		List<String> formulas = getFieldMetaAttribute(field, "hibernate.formula");
		return formulas != null ? formulas : Collections.emptyList();
	}

	// --- ManyToMany ---

	/**
	 * Returns true if the many-to-many target is referenced by entity-name.
	 */
	public boolean isManyToManyEntityNameRef(FieldDetails field) {
		String targetClassName = getManyToManyTargetEntity(field);
		Map<String, List<String>> targetMeta = allClassMetaAttributes.get(targetClassName);
		if (targetMeta != null) {
			List<String> realClass = targetMeta.get("hibernate.class-name");
			if (realClass != null && !realClass.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the entity-name for a many-to-many target, or null if not entity-name based.
	 */
	public String getManyToManyEntityName(FieldDetails field) {
		if (!isManyToManyEntityNameRef(field)) {
			return null;
		}
		String identityName = getManyToManyTargetEntity(field);
		int dot = identityName.lastIndexOf('.');
		return dot >= 0 ? identityName.substring(dot + 1) : identityName;
	}

	public List<String> getManyToManyFormulas(FieldDetails field) {
		List<String> formulas = getFieldMetaAttribute(field, "hibernate.formula");
		return formulas != null ? formulas : Collections.emptyList();
	}

	public boolean isManyToOneLazy(FieldDetails field) {
		ManyToOne m2o = field.getDirectAnnotationUsage(ManyToOne.class);
		return m2o != null && m2o.fetch() == jakarta.persistence.FetchType.LAZY;
	}

	public boolean isManyToOneUpdatable(FieldDetails field) {
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		if (jc != null) return jc.updatable();
		JoinColumns jcs = field.getDirectAnnotationUsage(JoinColumns.class);
		if (jcs != null && jcs.value().length > 0) return jcs.value()[0].updatable();
		return true;
	}

	public boolean isManyToOneInsertable(FieldDetails field) {
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		if (jc != null) return jc.insertable();
		JoinColumns jcs = field.getDirectAnnotationUsage(JoinColumns.class);
		if (jcs != null && jcs.value().length > 0) return jcs.value()[0].insertable();
		return true;
	}

	public boolean isManyToOneOptional(FieldDetails field) {
		ManyToOne m2o = field.getDirectAnnotationUsage(ManyToOne.class);
		return m2o == null || m2o.optional();
	}

	// --- JoinColumn (shared by ManyToOne, OneToOne) ---

	public String getPropertyRef(FieldDetails field) {
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		return jc != null && jc.referencedColumnName() != null
				&& !jc.referencedColumnName().isEmpty()
				? jc.referencedColumnName() : null;
	}

	public String getJoinColumnName(FieldDetails field) {
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		return jc != null ? jc.name() : null;
	}

	public List<String> getJoinColumnNames(FieldDetails field) {
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
		return result;
	}

	// --- OneToOne ---

	public String getOneToOneMappedBy(FieldDetails field) {
		OneToOne o2o = field.getDirectAnnotationUsage(OneToOne.class);
		return o2o != null && o2o.mappedBy() != null && !o2o.mappedBy().isEmpty()
				? o2o.mappedBy() : null;
	}

	public String getOneToOneCascadeString(FieldDetails field) {
		OneToOne o2o = field.getDirectAnnotationUsage(OneToOne.class);
		if (o2o == null || o2o.cascade().length == 0) {
			return null;
		}
		return getCascadeString(o2o.cascade());
	}

	public boolean isOneToOneConstrained(FieldDetails field) {
		return field.hasDirectAnnotationUsage(JoinColumn.class)
				|| field.hasDirectAnnotationUsage(JoinColumns.class);
	}

	// --- OneToMany ---

	public String getOneToManyTargetEntity(FieldDetails field) {
		TypeDetails elementType = field.getElementType();
		return elementType != null ? elementType.determineRawClass().getClassName() : null;
	}

	public List<String> getKeyColumnNames(FieldDetails field) {
		// Check @JoinColumns (multiple columns)
		jakarta.persistence.JoinColumns jcs =
				field.getDirectAnnotationUsage(jakarta.persistence.JoinColumns.class);
		if (jcs != null && jcs.value().length > 0) {
			List<String> names = new java.util.ArrayList<>();
			for (jakarta.persistence.JoinColumn jc : jcs.value()) {
				names.add(jc.name());
			}
			return names;
		}
		// Check single @JoinColumn
		jakarta.persistence.JoinColumn jc =
				field.getDirectAnnotationUsage(jakarta.persistence.JoinColumn.class);
		if (jc != null) {
			return java.util.Collections.singletonList(jc.name());
		}
		// Fall back to @OneToMany.mappedBy
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		if (o2m != null && o2m.mappedBy() != null && !o2m.mappedBy().isEmpty()) {
			return java.util.Collections.singletonList(o2m.mappedBy());
		}
		return java.util.Collections.emptyList();
	}

	public String getOneToManyCascadeString(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		if (o2m == null || o2m.cascade().length == 0) {
			return null;
		}
		return getCascadeString(o2m.cascade());
	}

	public boolean isOneToManyEager(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		return o2m != null && o2m.fetch() == jakarta.persistence.FetchType.EAGER;
	}

	// --- ManyToMany ---

	public String getManyToManyTargetEntity(FieldDetails field) {
		TypeDetails elementType = field.getElementType();
		return elementType != null ? elementType.determineRawClass().getClassName() : null;
	}

	public boolean isManyToManyInverse(FieldDetails field) {
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		return m2m != null && m2m.mappedBy() != null && !m2m.mappedBy().isEmpty();
	}

	public String getJoinTableName(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		if (jt == null) {
			return null;
		}
		String name = jt.name();
		return (name != null && !name.isEmpty()) ? name : null;
	}

	public boolean hasJoinTable(FieldDetails field) {
		return field.hasDirectAnnotationUsage(JoinTable.class);
	}

	public String getJoinTableSchema(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null && jt.schema() != null && !jt.schema().isEmpty()
				? jt.schema() : null;
	}

	public String getJoinTableCatalog(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null && jt.catalog() != null && !jt.catalog().isEmpty()
				? jt.catalog() : null;
	}

	public String getJoinTableJoinColumnName(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null && jt.joinColumns().length > 0 ? jt.joinColumns()[0].name() : null;
	}

	public List<String> getJoinTableJoinColumnNames(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		List<String> result = new ArrayList<>();
		if (jt != null) {
			for (JoinColumn jc : jt.joinColumns()) {
				result.add(jc.name());
			}
		}
		return result;
	}

	public String getJoinTableInverseJoinColumnName(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null && jt.inverseJoinColumns().length > 0
				? jt.inverseJoinColumns()[0].name() : null;
	}

	public List<String> getJoinTableInverseJoinColumnNames(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		List<String> result = new ArrayList<>();
		if (jt != null) {
			for (JoinColumn jc : jt.inverseJoinColumns()) {
				result.add(jc.name());
			}
		}
		return result;
	}

	public String getManyToManyCascadeString(FieldDetails field) {
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		if (m2m == null || m2m.cascade().length == 0) {
			return null;
		}
		return getCascadeString(m2m.cascade());
	}

	// --- Collection type ---

	public String getCollectionTag(FieldDetails field) {
		return collectionAttributeHelper.getCollectionTag(field);
	}

	public boolean isCollectionInverse(FieldDetails field) {
		return collectionAttributeHelper.isCollectionInverse(field);
	}

	public String getCollectionLazy(FieldDetails field) {
		return collectionAttributeHelper.getCollectionLazy(field);
	}

	public String getCollectionFetchMode(FieldDetails field) {
		return collectionAttributeHelper.getCollectionFetchMode(field);
	}

	public int getCollectionBatchSize(FieldDetails field) {
		return collectionAttributeHelper.getCollectionBatchSize(field);
	}

	public String getCollectionCascadeString(FieldDetails field) {
		return collectionAttributeHelper.getCollectionCascadeString(field);
	}

	public String getCollectionOrderBy(FieldDetails field) {
		return collectionAttributeHelper.getCollectionOrderBy(field);
	}

	public String getCollectionCacheUsage(FieldDetails field) {
		return collectionAttributeHelper.getCollectionCacheUsage(field);
	}

	public String getCollectionCacheRegion(FieldDetails field) {
		return collectionAttributeHelper.getCollectionCacheRegion(field);
	}

	public List<FilterInfo> getCollectionFilters(FieldDetails field) {
		return collectionAttributeHelper.getCollectionFilters(field);
	}

	// --- List-specific ---

	public String getListIndexColumnName(FieldDetails field) {
		return collectionAttributeHelper.getListIndexColumnName(field);
	}

	// --- Map-specific ---

	public String getMapKeyColumnName(FieldDetails field) {
		return collectionAttributeHelper.getMapKeyColumnName(field);
	}

	public String getMapKeyType(FieldDetails field) {
		return collectionAttributeHelper.getMapKeyType(field);
	}

	public boolean hasMapKeyJoinColumn(FieldDetails field) {
		return collectionAttributeHelper.hasMapKeyJoinColumn(field);
	}

	public String getMapKeyJoinColumnName(FieldDetails field) {
		return collectionAttributeHelper.getMapKeyJoinColumnName(field);
	}

	public String getMapKeyEntityClass(FieldDetails field) {
		return collectionAttributeHelper.getMapKeyEntityClass(field);
	}

	// --- IdBag-specific ---

	public String getCollectionIdColumnName(FieldDetails field) {
		return collectionAttributeHelper.getCollectionIdColumnName(field);
	}

	public String getCollectionIdGenerator(FieldDetails field) {
		return collectionAttributeHelper.getCollectionIdGenerator(field);
	}

	// --- Collection element type ---

	public String getCollectionElementType(FieldDetails field) {
		return collectionAttributeHelper.getCollectionElementType(field);
	}

	// --- Embedded / EmbeddedId attribute overrides ---

	public String getEmbeddableClassName(FieldDetails field) {
		return field.getType().determineRawClass().getClassName();
	}

	public List<AttributeOverrideInfo> getAttributeOverrides(FieldDetails field) {
		List<AttributeOverrideInfo> result = new ArrayList<>();
		AttributeOverrides overrides = field.getDirectAnnotationUsage(AttributeOverrides.class);
		if (overrides != null) {
			for (AttributeOverride ao : overrides.value()) {
				result.add(new AttributeOverrideInfo(ao.name(), ao.column().name()));
			}
		}
		return result;
	}

	public record AttributeOverrideInfo(String fieldName, String columnName) {}

	// --- Properties groups (<properties> element) ---

	private boolean isInPropertiesGroup(FieldDetails field) {
		Map<String, List<String>> attrs = fieldMetaAttributes.getOrDefault(
				field.getName(), Collections.emptyMap());
		return attrs.containsKey("hibernate.properties-group");
	}

	public List<PropertiesGroupInfo> getPropertiesGroups() {
		// Collect unique group names in field order
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
		List<PropertiesGroupInfo> result = new ArrayList<>();
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
			boolean unique = "true".equals(
					getClassMetaValue("hibernate.properties-group." + groupName + ".unique"));
			boolean insert = !"false".equals(
					getClassMetaValue("hibernate.properties-group." + groupName + ".insert"));
			boolean update = !"false".equals(
					getClassMetaValue("hibernate.properties-group." + groupName + ".update"));
			boolean optimisticLock = !"false".equals(
					getClassMetaValue("hibernate.properties-group." + groupName + ".optimistic-lock"));
			result.add(new PropertiesGroupInfo(groupName, unique, insert, update,
					optimisticLock, fields));
		}
		return result;
	}

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

	public record PropertiesGroupInfo(String name, boolean unique, boolean insert,
									   boolean update, boolean optimisticLock,
									   List<FieldDetails> fields) {}

	// --- Meta attributes ---

	public Map<String, List<String>> getMetaAttributes() {
		Map<String, List<String>> result = new java.util.LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : metaAttributes.entrySet()) {
			if (!entry.getKey().startsWith("hibernate.proxy")
					&& !entry.getKey().equals("hibernate.comment")
					&& !entry.getKey().equals("hibernate.class-name")
					&& !entry.getKey().startsWith("hibernate.join.comment.")
					&& !entry.getKey().startsWith("hibernate.sql-query.")
					&& !entry.getKey().startsWith("hibernate.properties-group.")) {
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return result;
	}

	public List<String> getMetaAttribute(String name) {
		return metaAttributes.getOrDefault(name, Collections.emptyList());
	}

	private List<String> getClassMetaAttribute(String name) {
		List<String> values = metaAttributes.get(name);
		return (values != null && !values.isEmpty()) ? values : null;
	}

	private String getClassMetaValue(String key) {
		List<String> values = metaAttributes.get(key);
		return values != null && !values.isEmpty() ? values.get(0) : null;
	}

	public Map<String, List<String>> getFieldMetaAttributes(FieldDetails field) {
		Map<String, List<String>> all = fieldMetaAttributes.getOrDefault(
				field.getName(), Collections.emptyMap());
		// Filter out internal type parameter keys
		Map<String, List<String>> result = new java.util.LinkedHashMap<>();
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

	// --- Imports ---

	public List<ImportInfo> getImports() {
		List<ImportInfo> result = new ArrayList<>();
		for (Map.Entry<String, String> entry : imports.entrySet()) {
			if (!entry.getKey().equals(entry.getValue())) {
				result.add(new ImportInfo(entry.getKey(), entry.getValue()));
			}
		}
		return result;
	}

	public record ImportInfo(String className, String rename) {}

	// --- Filters ---

	public List<FilterInfo> getFilters() {
		return queryAndFilterHelper.getFilters();
	}

	public List<FilterDefInfo> getFilterDefs() {
		return queryAndFilterHelper.getFilterDefs();
	}

	public record FilterInfo(String name, String condition) {}

	public record FilterDefInfo(String name, String defaultCondition, Map<String, String> parameters) {}

	// --- Named queries ---

	public List<NamedQueryInfo> getNamedQueries() {
		return queryAndFilterHelper.getNamedQueries();
	}

	public List<NamedNativeQueryInfo> getNamedNativeQueries() {
		return queryAndFilterHelper.getNamedNativeQueries();
	}

	public record NamedQueryInfo(String name, String query, String flushMode,
								 boolean cacheable, String cacheRegion, int fetchSize,
								 int timeout, String comment, boolean readOnly) {}

	public record NamedNativeQueryInfo(String name, String query, String flushMode,
									   boolean cacheable, String cacheRegion, int fetchSize,
									   int timeout, String comment, boolean readOnly,
									   List<String> querySpaces,
									   List<EntityReturnInfo> entityReturns,
									   List<ScalarReturnInfo> scalarReturns,
									   List<ReturnJoinInfo> returnJoins,
									   List<LoadCollectionInfo> loadCollections) {}

	public record EntityReturnInfo(String alias, String entityClass,
								   String discriminatorColumn,
								   List<FieldMappingInfo> fieldMappings) {}

	public record FieldMappingInfo(String name, String column) {}

	public record ScalarReturnInfo(String column) {}

	public record ReturnJoinInfo(String alias, String property) {}

	public record LoadCollectionInfo(String alias, String role, String lockMode) {}

	// --- SecondaryTable / Joins ---

	public List<JoinInfo> getJoins() {
		return queryAndFilterHelper.getJoins();
	}

	public List<FieldDetails> getJoinProperties(String tableName) {
		return queryAndFilterHelper.getJoinProperties(tableName);
	}

	public String getJoinComment(String tableName) {
		return queryAndFilterHelper.getJoinComment(tableName);
	}

	public record JoinInfo(String tableName, List<String> keyColumns) {}

	// --- Utilities ---

	public String getCascadeString(CascadeType[] cascadeTypes) {
		if (cascadeTypes == null || cascadeTypes.length == 0) {
			return "none";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cascadeTypes.length; i++) {
			if (i > 0) sb.append(", ");
			sb.append(toHbmCascade(cascadeTypes[i]));
		}
		return sb.toString();
	}

	String toGeneratorClass(GenerationType generationType) {
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

	private <A extends Annotation> List<FieldDetails> getFieldsWithAnnotation(Class<A> annotationType) {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(annotationType)) {
				result.add(field);
			}
		}
		return result;
	}

	private String toHbmCascade(CascadeType cascadeType) {
		return switch (cascadeType) {
			case ALL -> "all";
			case PERSIST -> "persist";
			case MERGE -> "merge";
			case REMOVE -> "delete";
			case REFRESH -> "refresh";
			case DETACH -> "evict";
		};
	}

	// --- SQL operations ---

	public record CustomSqlInfo(String sql, boolean callable) {}

	public CustomSqlInfo getSQLInsert() {
		return queryAndFilterHelper.getSQLInsert();
	}

	public CustomSqlInfo getSQLUpdate() {
		return queryAndFilterHelper.getSQLUpdate();
	}

	public CustomSqlInfo getSQLDelete() {
		return queryAndFilterHelper.getSQLDelete();
	}

	public CustomSqlInfo getSQLDeleteAll() {
		return queryAndFilterHelper.getSQLDeleteAll();
	}

	public CustomSqlInfo getCollectionSQLInsert(FieldDetails field) {
		return collectionAttributeHelper.getCollectionSQLInsert(field);
	}

	public CustomSqlInfo getCollectionSQLUpdate(FieldDetails field) {
		return collectionAttributeHelper.getCollectionSQLUpdate(field);
	}

	public CustomSqlInfo getCollectionSQLDelete(FieldDetails field) {
		return collectionAttributeHelper.getCollectionSQLDelete(field);
	}

	public CustomSqlInfo getCollectionSQLDeleteAll(FieldDetails field) {
		return collectionAttributeHelper.getCollectionSQLDeleteAll(field);
	}

	// --- Sort ---

	public String getSort(FieldDetails field) {
		return collectionAttributeHelper.getSort(field);
	}

	// --- Fetch profiles ---

	public record FetchProfileInfo(String name, List<FetchOverrideInfo> overrides) {}

	public record FetchOverrideInfo(String entity, String association, String style) {}

	public List<FetchProfileInfo> getFetchProfiles() {
		return queryAndFilterHelper.getFetchProfiles();
	}
}
