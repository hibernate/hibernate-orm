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
public class MappingFieldAnnotationHelper {

	private final ClassDetails classDetails;

	MappingFieldAnnotationHelper(ClassDetails classDetails) {
		this.classDetails = classDetails;
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
					&& !field.hasDirectAnnotationUsage(ManyToAny.class)
					&& !field.hasDirectAnnotationUsage(ElementCollection.class)
					&& !field.hasDirectAnnotationUsage(NaturalId.class)) {
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
		return getFieldsWithAnnotation(ManyToOne.class);
	}

	public List<FieldDetails> getOneToManyFields() {
		return getFieldsWithAnnotation(OneToMany.class);
	}

	public List<FieldDetails> getOneToOneFields() {
		return getFieldsWithAnnotation(OneToOne.class);
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

	public List<FieldDetails> getManyToAnyFields() {
		return getFieldsWithAnnotation(ManyToAny.class);
	}

	public List<FieldDetails> getElementCollectionFields() {
		return getFieldsWithAnnotation(ElementCollection.class);
	}

	// --- Column attributes ---

	public String getColumnName(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null ? col.name() : field.getName();
	}

	public boolean isNullable(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col == null || col.nullable();
	}

	public boolean isUnique(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null && col.unique();
	}

	public int getLength(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		if (col == null) return 0;
		return col.length() != 255 ? col.length() : 0;
	}

	public int getPrecision(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null ? col.precision() : 0;
	}

	public int getScale(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null ? col.scale() : 0;
	}

	public String getColumnDefinition(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null && col.columnDefinition() != null && !col.columnDefinition().isEmpty()
				? col.columnDefinition() : null;
	}

	public boolean isInsertable(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col == null || col.insertable();
	}

	public boolean isUpdatable(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col == null || col.updatable();
	}

	public boolean isLob(FieldDetails field) {
		return field.hasDirectAnnotationUsage(Lob.class);
	}

	public String getTemporalType(FieldDetails field) {
		Temporal temporal = field.getDirectAnnotationUsage(Temporal.class);
		return temporal != null ? temporal.value().name() : null;
	}

	public String getGenerationType(FieldDetails field) {
		GeneratedValue gv = field.getDirectAnnotationUsage(GeneratedValue.class);
		return gv != null ? gv.strategy().name() : null;
	}

	public String getGeneratorName(FieldDetails field) {
		GeneratedValue gv = field.getDirectAnnotationUsage(GeneratedValue.class);
		return gv != null && gv.generator() != null && !gv.generator().isEmpty()
				? gv.generator() : null;
	}

	public MappingXmlHelper.SequenceGeneratorInfo getSequenceGenerator(FieldDetails field) {
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

	public MappingXmlHelper.TableGeneratorInfo getTableGenerator(FieldDetails field) {
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

	public String getTargetEntityName(FieldDetails field) {
		return field.getType().determineRawClass().getClassName();
	}

	public String getManyToOneFetchType(FieldDetails field) {
		ManyToOne m2o = field.getDirectAnnotationUsage(ManyToOne.class);
		return m2o != null && m2o.fetch() != FetchType.EAGER ? m2o.fetch().name() : null;
	}

	public boolean isManyToOneOptional(FieldDetails field) {
		ManyToOne m2o = field.getDirectAnnotationUsage(ManyToOne.class);
		return m2o == null || m2o.optional();
	}

	// --- JoinColumn ---

	public String getJoinColumnName(FieldDetails field) {
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		return jc != null ? jc.name() : null;
	}

	public String getReferencedColumnName(FieldDetails field) {
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		return jc != null && jc.referencedColumnName() != null
				&& !jc.referencedColumnName().isEmpty()
				? jc.referencedColumnName() : null;
	}

	public List<MappingXmlHelper.JoinColumnInfo> getJoinColumns(FieldDetails field) {
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

	public String getOneToManyTargetEntity(FieldDetails field) {
		TypeDetails elementType = field.getElementType();
		return elementType != null ? elementType.determineRawClass().getClassName() : null;
	}

	public String getOneToManyMappedBy(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		return o2m != null && o2m.mappedBy() != null && !o2m.mappedBy().isEmpty()
				? o2m.mappedBy() : null;
	}

	public String getOneToManyFetchType(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		return o2m != null && o2m.fetch() != FetchType.LAZY ? o2m.fetch().name() : null;
	}

	public boolean isOneToManyOrphanRemoval(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		return o2m != null && o2m.orphanRemoval();
	}

	public List<CascadeType> getOneToManyCascadeTypes(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		return o2m != null && o2m.cascade().length > 0
				? Arrays.asList(o2m.cascade()) : List.of();
	}

	// --- Ordering ---

	public String getOrderBy(FieldDetails field) {
		OrderBy ob = field.getDirectAnnotationUsage(OrderBy.class);
		return ob != null && ob.value() != null && !ob.value().isEmpty()
				? ob.value() : null;
	}

	public String getOrderColumnName(FieldDetails field) {
		OrderColumn oc = field.getDirectAnnotationUsage(OrderColumn.class);
		return oc != null && oc.name() != null && !oc.name().isEmpty()
				? oc.name() : null;
	}

	// --- Map key ---

	public String getMapKeyName(FieldDetails field) {
		MapKey mk = field.getDirectAnnotationUsage(MapKey.class);
		return mk != null ? mk.name() : null;
	}

	public String getMapKeyColumnName(FieldDetails field) {
		MapKeyColumn mkc = field.getDirectAnnotationUsage(MapKeyColumn.class);
		return mkc != null && mkc.name() != null && !mkc.name().isEmpty()
				? mkc.name() : null;
	}

	public String getMapKeyJoinColumnName(FieldDetails field) {
		MapKeyJoinColumn mkjc = field.getDirectAnnotationUsage(MapKeyJoinColumn.class);
		return mkjc != null && mkjc.name() != null && !mkjc.name().isEmpty()
				? mkjc.name() : null;
	}

	// --- Fetch mode ---

	public String getFetchMode(FieldDetails field) {
		Fetch fetch = field.getDirectAnnotationUsage(Fetch.class);
		if (fetch == null) {
			return null;
		}
		return fetch.value().name();
	}

	// --- NotFound ---

	public String getNotFoundAction(FieldDetails field) {
		NotFound nf = field.getDirectAnnotationUsage(NotFound.class);
		if (nf == null || nf.action() == NotFoundAction.EXCEPTION) {
			return null;
		}
		return nf.action().name();
	}

	// --- Convert ---

	public String getConverterClassName(FieldDetails field) {
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

	public String getOneToOneMappedBy(FieldDetails field) {
		OneToOne o2o = field.getDirectAnnotationUsage(OneToOne.class);
		return o2o != null && o2o.mappedBy() != null && !o2o.mappedBy().isEmpty()
				? o2o.mappedBy() : null;
	}

	public String getOneToOneFetchType(FieldDetails field) {
		OneToOne o2o = field.getDirectAnnotationUsage(OneToOne.class);
		return o2o != null && o2o.fetch() != FetchType.EAGER ? o2o.fetch().name() : null;
	}

	public boolean isOneToOneOptional(FieldDetails field) {
		OneToOne o2o = field.getDirectAnnotationUsage(OneToOne.class);
		return o2o == null || o2o.optional();
	}

	public boolean isOneToOneOrphanRemoval(FieldDetails field) {
		OneToOne o2o = field.getDirectAnnotationUsage(OneToOne.class);
		return o2o != null && o2o.orphanRemoval();
	}

	public List<CascadeType> getOneToOneCascadeTypes(FieldDetails field) {
		OneToOne o2o = field.getDirectAnnotationUsage(OneToOne.class);
		return o2o != null && o2o.cascade().length > 0
				? Arrays.asList(o2o.cascade()) : List.of();
	}

	// --- ManyToMany ---

	public String getManyToManyTargetEntity(FieldDetails field) {
		TypeDetails elementType = field.getElementType();
		return elementType != null ? elementType.determineRawClass().getClassName() : null;
	}

	public String getManyToManyMappedBy(FieldDetails field) {
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		return m2m != null && m2m.mappedBy() != null && !m2m.mappedBy().isEmpty()
				? m2m.mappedBy() : null;
	}

	public String getManyToManyFetchType(FieldDetails field) {
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		return m2m != null && m2m.fetch() != FetchType.LAZY ? m2m.fetch().name() : null;
	}

	public List<CascadeType> getManyToManyCascadeTypes(FieldDetails field) {
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		return m2m != null && m2m.cascade().length > 0
				? Arrays.asList(m2m.cascade()) : List.of();
	}

	public String getJoinTableName(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null ? jt.name() : null;
	}

	public String getJoinTableSchema(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null && jt.schema() != null && !jt.schema().isEmpty() ? jt.schema() : null;
	}

	public String getJoinTableCatalog(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null && jt.catalog() != null && !jt.catalog().isEmpty() ? jt.catalog() : null;
	}

	public String getJoinTableJoinColumnName(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null && jt.joinColumns().length > 0 ? jt.joinColumns()[0].name() : null;
	}

	public String getJoinTableInverseJoinColumnName(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null && jt.inverseJoinColumns().length > 0
				? jt.inverseJoinColumns()[0].name() : null;
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

	// --- Embedded / EmbeddedId attribute overrides ---

	public List<MappingXmlHelper.AttributeOverrideInfo> getAttributeOverrides(FieldDetails field) {
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

	public String getColumnTable(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null && col.table() != null && !col.table().isEmpty()
				? col.table() : null;
	}

	public String getFormula(FieldDetails field) {
		Formula formula = field.getDirectAnnotationUsage(Formula.class);
		return formula != null ? formula.value() : null;
	}

	public boolean isPropertyLazy(FieldDetails field) {
		Basic basic = field.getDirectAnnotationUsage(Basic.class);
		return basic != null && basic.fetch() == FetchType.LAZY;
	}

	public String getFieldAccessType(FieldDetails field) {
		Access access = field.getDirectAnnotationUsage(Access.class);
		if (access == null || access.value() == AccessType.FIELD) {
			return null;
		}
		return access.value().name();
	}

	public boolean isOptimisticLockExcluded(FieldDetails field) {
		OptimisticLock ol = field.getDirectAnnotationUsage(OptimisticLock.class);
		return ol != null && ol.excluded();
	}

	// --- Any ---

	public String getAnyDiscriminatorType(FieldDetails field) {
		AnyDiscriminator ad = field.getDirectAnnotationUsage(AnyDiscriminator.class);
		if (ad == null) {
			return "STRING";
		}
		return ad.value().name();
	}

	public String getAnyKeyType(FieldDetails field) {
		AnyKeyJavaClass akjc = field.getDirectAnnotationUsage(AnyKeyJavaClass.class);
		return akjc != null ? akjc.value().getName() : "java.lang.Long";
	}

	public List<MappingXmlHelper.AnyDiscriminatorMapping> getAnyDiscriminatorMappings(FieldDetails field) {
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

	public String getElementCollectionKeyColumnName(FieldDetails field) {
		CollectionTable ct = field.getDirectAnnotationUsage(CollectionTable.class);
		if (ct != null && ct.joinColumns() != null && ct.joinColumns().length > 0) {
			return ct.joinColumns()[0].name();
		}
		return null;
	}

	public String getElementCollectionTargetClass(FieldDetails field) {
		TypeDetails elementType = field.getElementType();
		return elementType != null ? elementType.determineRawClass().getClassName() : null;
	}

	public String getElementCollectionColumnName(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null && col.name() != null && !col.name().isEmpty() ? col.name() : null;
	}

	// --- Sort ---

	public boolean isSortNatural(FieldDetails field) {
		return field.hasDirectAnnotationUsage(SortNatural.class);
	}

	public String getSortComparatorClass(FieldDetails field) {
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
