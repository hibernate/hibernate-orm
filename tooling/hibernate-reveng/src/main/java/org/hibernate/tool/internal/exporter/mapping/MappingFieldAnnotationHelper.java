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
package org.hibernate.tool.internal.exporter.mapping;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.Version;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.TypeDetails;

/**
 * Handles field-level annotation reading for mapping XML template generation:
 * field categorization, column attributes, generators, associations,
 * Any/ElementCollection, ordering, map keys, and property-level attributes.
 *
 * @author Koen Aers
 */
class MappingFieldAnnotationHelper {

	private final ClassDetails classDetails;

	MappingFieldAnnotationHelper(ClassDetails classDetails) {
		this.classDetails = classDetails;
	}

	// --- Field categorization ---

	FieldDetails getCompositeIdField() {
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(EmbeddedId.class)) {
				return field;
			}
		}
		return null;
	}

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
					&& !field.hasDirectAnnotationUsage(ManyToAny.class)
					&& !field.hasDirectAnnotationUsage(ElementCollection.class)
					&& !field.hasDirectAnnotationUsage(NaturalId.class)) {
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
		return getFieldsWithAnnotation(ManyToOne.class);
	}

	List<FieldDetails> getOneToManyFields() {
		return getFieldsWithAnnotation(OneToMany.class);
	}

	List<FieldDetails> getOneToOneFields() {
		return getFieldsWithAnnotation(OneToOne.class);
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

	List<FieldDetails> getManyToAnyFields() {
		return getFieldsWithAnnotation(ManyToAny.class);
	}

	List<FieldDetails> getElementCollectionFields() {
		return getFieldsWithAnnotation(ElementCollection.class);
	}

	// --- Column attributes ---

	String getColumnName(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null ? col.name() : field.getName();
	}

	boolean isNullable(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col == null || col.nullable();
	}

	boolean isUnique(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null && col.unique();
	}

	int getLength(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		if (col == null) return 0;
		return col.length() != 255 ? col.length() : 0;
	}

	int getPrecision(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null ? col.precision() : 0;
	}

	int getScale(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null ? col.scale() : 0;
	}

	String getColumnDefinition(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null && col.columnDefinition() != null && !col.columnDefinition().isEmpty()
				? col.columnDefinition() : null;
	}

	boolean isInsertable(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col == null || col.insertable();
	}

	boolean isUpdatable(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col == null || col.updatable();
	}

	boolean isLob(FieldDetails field) {
		return field.hasDirectAnnotationUsage(Lob.class);
	}

	String getTemporalType(FieldDetails field) {
		Temporal temporal = field.getDirectAnnotationUsage(Temporal.class);
		return temporal != null ? temporal.value().name() : null;
	}

	String getGenerationType(FieldDetails field) {
		GeneratedValue gv = field.getDirectAnnotationUsage(GeneratedValue.class);
		return gv != null ? gv.strategy().name() : null;
	}

	String getGeneratorName(FieldDetails field) {
		GeneratedValue gv = field.getDirectAnnotationUsage(GeneratedValue.class);
		return gv != null && gv.generator() != null && !gv.generator().isEmpty()
				? gv.generator() : null;
	}

	MappingXmlHelper.SequenceGeneratorInfo getSequenceGenerator(FieldDetails field) {
		SequenceGenerator sg = field.getDirectAnnotationUsage(SequenceGenerator.class);
		if (sg == null) {
			return null;
		}
		return new MappingXmlHelper.SequenceGeneratorInfo(
				sg.name(),
				sg.sequenceName() != null && !sg.sequenceName().isEmpty() ? sg.sequenceName() : null,
				sg.allocationSize() != 50 ? sg.allocationSize() : null,
				sg.initialValue() != 1 ? sg.initialValue() : null);
	}

	MappingXmlHelper.TableGeneratorInfo getTableGenerator(FieldDetails field) {
		TableGenerator tg = field.getDirectAnnotationUsage(TableGenerator.class);
		if (tg == null) {
			return null;
		}
		return new MappingXmlHelper.TableGeneratorInfo(
				tg.name(),
				tg.table() != null && !tg.table().isEmpty() ? tg.table() : null,
				tg.pkColumnName() != null && !tg.pkColumnName().isEmpty() ? tg.pkColumnName() : null,
				tg.valueColumnName() != null && !tg.valueColumnName().isEmpty() ? tg.valueColumnName() : null,
				tg.pkColumnValue() != null && !tg.pkColumnValue().isEmpty() ? tg.pkColumnValue() : null,
				tg.allocationSize() != 50 ? tg.allocationSize() : null,
				tg.initialValue() != 0 ? tg.initialValue() : null);
	}

	// --- ManyToOne ---

	String getTargetEntityName(FieldDetails field) {
		return field.getType().determineRawClass().getClassName();
	}

	String getManyToOneFetchType(FieldDetails field) {
		ManyToOne m2o = field.getDirectAnnotationUsage(ManyToOne.class);
		return m2o != null && m2o.fetch() != FetchType.EAGER ? m2o.fetch().name() : null;
	}

	boolean isManyToOneOptional(FieldDetails field) {
		ManyToOne m2o = field.getDirectAnnotationUsage(ManyToOne.class);
		return m2o == null || m2o.optional();
	}

	// --- JoinColumn ---

	String getJoinColumnName(FieldDetails field) {
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		return jc != null ? jc.name() : null;
	}

	String getReferencedColumnName(FieldDetails field) {
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		return jc != null && jc.referencedColumnName() != null
				&& !jc.referencedColumnName().isEmpty()
				? jc.referencedColumnName() : null;
	}

	List<MappingXmlHelper.JoinColumnInfo> getJoinColumns(FieldDetails field) {
		List<MappingXmlHelper.JoinColumnInfo> result = new ArrayList<>();
		JoinColumn single = field.getDirectAnnotationUsage(JoinColumn.class);
		if (single != null) {
			result.add(toJoinColumnInfo(single));
		}
		JoinColumns container = field.getDirectAnnotationUsage(JoinColumns.class);
		if (container != null) {
			for (JoinColumn jc : container.value()) {
				result.add(toJoinColumnInfo(jc));
			}
		}
		return result;
	}

	private MappingXmlHelper.JoinColumnInfo toJoinColumnInfo(JoinColumn jc) {
		String refCol = jc.referencedColumnName() != null
				&& !jc.referencedColumnName().isEmpty()
				? jc.referencedColumnName() : null;
		return new MappingXmlHelper.JoinColumnInfo(jc.name(), refCol);
	}

	// --- OneToMany ---

	String getOneToManyTargetEntity(FieldDetails field) {
		TypeDetails elementType = field.getElementType();
		return elementType != null ? elementType.determineRawClass().getClassName() : null;
	}

	String getOneToManyMappedBy(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		return o2m != null && o2m.mappedBy() != null && !o2m.mappedBy().isEmpty()
				? o2m.mappedBy() : null;
	}

	String getOneToManyFetchType(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		return o2m != null && o2m.fetch() != FetchType.LAZY ? o2m.fetch().name() : null;
	}

	boolean isOneToManyOrphanRemoval(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		return o2m != null && o2m.orphanRemoval();
	}

	List<CascadeType> getOneToManyCascadeTypes(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		return o2m != null && o2m.cascade().length > 0
				? Arrays.asList(o2m.cascade()) : List.of();
	}

	// --- Ordering ---

	String getOrderBy(FieldDetails field) {
		OrderBy ob = field.getDirectAnnotationUsage(OrderBy.class);
		return ob != null && ob.value() != null && !ob.value().isEmpty()
				? ob.value() : null;
	}

	String getOrderColumnName(FieldDetails field) {
		OrderColumn oc = field.getDirectAnnotationUsage(OrderColumn.class);
		return oc != null && oc.name() != null && !oc.name().isEmpty()
				? oc.name() : null;
	}

	// --- Map key ---

	String getMapKeyName(FieldDetails field) {
		MapKey mk = field.getDirectAnnotationUsage(MapKey.class);
		return mk != null ? mk.name() : null;
	}

	String getMapKeyColumnName(FieldDetails field) {
		MapKeyColumn mkc = field.getDirectAnnotationUsage(MapKeyColumn.class);
		return mkc != null && mkc.name() != null && !mkc.name().isEmpty()
				? mkc.name() : null;
	}

	String getMapKeyJoinColumnName(FieldDetails field) {
		MapKeyJoinColumn mkjc = field.getDirectAnnotationUsage(MapKeyJoinColumn.class);
		return mkjc != null && mkjc.name() != null && !mkjc.name().isEmpty()
				? mkjc.name() : null;
	}

	// --- Fetch mode ---

	String getFetchMode(FieldDetails field) {
		Fetch fetch = field.getDirectAnnotationUsage(Fetch.class);
		if (fetch == null) {
			return null;
		}
		return fetch.value().name();
	}

	// --- NotFound ---

	String getNotFoundAction(FieldDetails field) {
		NotFound nf = field.getDirectAnnotationUsage(NotFound.class);
		if (nf == null || nf.action() == NotFoundAction.EXCEPTION) {
			return null;
		}
		return nf.action().name();
	}

	// --- Convert ---

	String getConverterClassName(FieldDetails field) {
		Convert convert = field.getDirectAnnotationUsage(Convert.class);
		if (convert == null || convert.disableConversion()) {
			return null;
		}
		Class<?> converterClass = convert.converter();
		if (converterClass == null || converterClass == jakarta.persistence.AttributeConverter.class) {
			return null;
		}
		return converterClass.getName();
	}

	// --- OneToOne ---

	String getOneToOneMappedBy(FieldDetails field) {
		OneToOne o2o = field.getDirectAnnotationUsage(OneToOne.class);
		return o2o != null && o2o.mappedBy() != null && !o2o.mappedBy().isEmpty()
				? o2o.mappedBy() : null;
	}

	String getOneToOneFetchType(FieldDetails field) {
		OneToOne o2o = field.getDirectAnnotationUsage(OneToOne.class);
		return o2o != null && o2o.fetch() != FetchType.EAGER ? o2o.fetch().name() : null;
	}

	boolean isOneToOneOptional(FieldDetails field) {
		OneToOne o2o = field.getDirectAnnotationUsage(OneToOne.class);
		return o2o == null || o2o.optional();
	}

	boolean isOneToOneOrphanRemoval(FieldDetails field) {
		OneToOne o2o = field.getDirectAnnotationUsage(OneToOne.class);
		return o2o != null && o2o.orphanRemoval();
	}

	List<CascadeType> getOneToOneCascadeTypes(FieldDetails field) {
		OneToOne o2o = field.getDirectAnnotationUsage(OneToOne.class);
		return o2o != null && o2o.cascade().length > 0
				? Arrays.asList(o2o.cascade()) : List.of();
	}

	// --- ManyToMany ---

	String getManyToManyTargetEntity(FieldDetails field) {
		TypeDetails elementType = field.getElementType();
		return elementType != null ? elementType.determineRawClass().getClassName() : null;
	}

	String getManyToManyMappedBy(FieldDetails field) {
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		return m2m != null && m2m.mappedBy() != null && !m2m.mappedBy().isEmpty()
				? m2m.mappedBy() : null;
	}

	String getManyToManyFetchType(FieldDetails field) {
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		return m2m != null && m2m.fetch() != FetchType.LAZY ? m2m.fetch().name() : null;
	}

	List<CascadeType> getManyToManyCascadeTypes(FieldDetails field) {
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		return m2m != null && m2m.cascade().length > 0
				? Arrays.asList(m2m.cascade()) : List.of();
	}

	String getJoinTableName(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null ? jt.name() : null;
	}

	String getJoinTableSchema(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null && jt.schema() != null && !jt.schema().isEmpty() ? jt.schema() : null;
	}

	String getJoinTableCatalog(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null && jt.catalog() != null && !jt.catalog().isEmpty() ? jt.catalog() : null;
	}

	String getJoinTableJoinColumnName(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null && jt.joinColumns().length > 0 ? jt.joinColumns()[0].name() : null;
	}

	String getJoinTableInverseJoinColumnName(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null && jt.inverseJoinColumns().length > 0
				? jt.inverseJoinColumns()[0].name() : null;
	}

	List<String> getJoinTableJoinColumnNames(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		List<String> result = new ArrayList<>();
		if (jt != null) {
			for (JoinColumn jc : jt.joinColumns()) {
				result.add(jc.name());
			}
		}
		return result;
	}

	List<String> getJoinTableInverseJoinColumnNames(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		List<String> result = new ArrayList<>();
		if (jt != null) {
			for (JoinColumn jc : jt.inverseJoinColumns()) {
				result.add(jc.name());
			}
		}
		return result;
	}

	// --- Embedded / EmbeddedId attribute overrides ---

	List<MappingXmlHelper.AttributeOverrideInfo> getAttributeOverrides(FieldDetails field) {
		List<MappingXmlHelper.AttributeOverrideInfo> result = new ArrayList<>();
		AttributeOverrides overrides = field.getDirectAnnotationUsage(AttributeOverrides.class);
		if (overrides != null) {
			for (AttributeOverride ao : overrides.value()) {
				result.add(new MappingXmlHelper.AttributeOverrideInfo(ao.name(), ao.column().name()));
			}
		}
		return result;
	}

	// --- Property-level attributes ---

	String getColumnTable(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null && col.table() != null && !col.table().isEmpty()
				? col.table() : null;
	}

	String getFormula(FieldDetails field) {
		Formula formula = field.getDirectAnnotationUsage(Formula.class);
		return formula != null ? formula.value() : null;
	}

	boolean isPropertyLazy(FieldDetails field) {
		Basic basic = field.getDirectAnnotationUsage(Basic.class);
		return basic != null && basic.fetch() == FetchType.LAZY;
	}

	String getFieldAccessType(FieldDetails field) {
		Access access = field.getDirectAnnotationUsage(Access.class);
		if (access == null || access.value() == AccessType.FIELD) {
			return null;
		}
		return access.value().name();
	}

	boolean isOptimisticLockExcluded(FieldDetails field) {
		OptimisticLock ol = field.getDirectAnnotationUsage(OptimisticLock.class);
		return ol != null && ol.excluded();
	}

	// --- Any ---

	String getAnyDiscriminatorType(FieldDetails field) {
		AnyDiscriminator ad = field.getDirectAnnotationUsage(AnyDiscriminator.class);
		if (ad == null) {
			return "STRING";
		}
		return ad.value().name();
	}

	String getAnyKeyType(FieldDetails field) {
		AnyKeyJavaClass akjc = field.getDirectAnnotationUsage(AnyKeyJavaClass.class);
		return akjc != null ? akjc.value().getName() : "java.lang.Long";
	}

	List<MappingXmlHelper.AnyDiscriminatorMapping> getAnyDiscriminatorMappings(FieldDetails field) {
		List<MappingXmlHelper.AnyDiscriminatorMapping> result = new ArrayList<>();
		AnyDiscriminatorValue single = field.getDirectAnnotationUsage(AnyDiscriminatorValue.class);
		if (single != null) {
			result.add(new MappingXmlHelper.AnyDiscriminatorMapping(
					single.discriminator(), single.entity().getName()));
		}
		AnyDiscriminatorValues container = field.getDirectAnnotationUsage(AnyDiscriminatorValues.class);
		if (container != null) {
			for (AnyDiscriminatorValue adv : container.value()) {
				result.add(new MappingXmlHelper.AnyDiscriminatorMapping(
						adv.discriminator(), adv.entity().getName()));
			}
		}
		return result;
	}

	// --- ElementCollection ---

	boolean isElementCollectionOfEmbeddable(FieldDetails field) {
		TypeDetails elementType = field.getElementType();
		if (elementType == null) {
			return false;
		}
		ClassDetails rawClass = elementType.determineRawClass();
		return rawClass.hasDirectAnnotationUsage(jakarta.persistence.Embeddable.class);
	}

	String getElementCollectionTableName(FieldDetails field) {
		CollectionTable ct = field.getDirectAnnotationUsage(CollectionTable.class);
		return ct != null && ct.name() != null && !ct.name().isEmpty() ? ct.name() : null;
	}

	String getElementCollectionKeyColumnName(FieldDetails field) {
		CollectionTable ct = field.getDirectAnnotationUsage(CollectionTable.class);
		if (ct != null && ct.joinColumns() != null && ct.joinColumns().length > 0) {
			return ct.joinColumns()[0].name();
		}
		return null;
	}

	String getElementCollectionTargetClass(FieldDetails field) {
		TypeDetails elementType = field.getElementType();
		return elementType != null ? elementType.determineRawClass().getClassName() : null;
	}

	String getElementCollectionColumnName(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null && col.name() != null && !col.name().isEmpty() ? col.name() : null;
	}

	// --- Sort ---

	boolean isSortNatural(FieldDetails field) {
		return field.hasDirectAnnotationUsage(SortNatural.class);
	}

	String getSortComparatorClass(FieldDetails field) {
		SortComparator sc = field.getDirectAnnotationUsage(SortComparator.class);
		return sc != null ? sc.value().getName() : null;
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
