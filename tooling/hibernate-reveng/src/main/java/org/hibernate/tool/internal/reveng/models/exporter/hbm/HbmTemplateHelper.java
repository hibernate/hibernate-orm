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
package org.hibernate.tool.internal.reveng.models.exporter.hbm;

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
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.FetchType;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Version;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.Bag;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLDeletes;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLInserts;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.SQLUpdates;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.FlushModeType;

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

	HbmTemplateHelper(ClassDetails classDetails) {
		this(classDetails, null, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
	}

	HbmTemplateHelper(ClassDetails classDetails, String comment,
					   Map<String, List<String>> metaAttributes) {
		this(classDetails, comment, metaAttributes, Collections.emptyMap(), Collections.emptyMap());
	}

	HbmTemplateHelper(ClassDetails classDetails, String comment,
					   Map<String, List<String>> metaAttributes,
					   Map<String, String> imports) {
		this(classDetails, comment, metaAttributes, imports, Collections.emptyMap());
	}

	HbmTemplateHelper(ClassDetails classDetails, String comment,
					   Map<String, List<String>> metaAttributes,
					   Map<String, String> imports,
					   Map<String, Map<String, List<String>>> fieldMetaAttributes) {
		this.classDetails = classDetails;
		this.comment = comment;
		this.metaAttributes = metaAttributes != null ? metaAttributes : Collections.emptyMap();
		this.imports = imports != null ? imports : Collections.emptyMap();
		this.fieldMetaAttributes = fieldMetaAttributes != null ? fieldMetaAttributes : Collections.emptyMap();
	}

	// --- Entity / class ---

	public String getClassName() {
		String name = classDetails.getClassName();
		return name.startsWith(".") ? name.substring(1) : name;
	}

	public String getPackageName() {
		String name = getClassName();
		int dot = name.lastIndexOf('.');
		return dot > 0 ? name.substring(0, dot) : null;
	}

	// --- Table ---

	public String getTableName() {
		Table table = classDetails.getDirectAnnotationUsage(Table.class);
		return table != null ? table.name() : null;
	}

	public String getSchema() {
		Table table = classDetails.getDirectAnnotationUsage(Table.class);
		return table != null && table.schema() != null && !table.schema().isEmpty()
				? table.schema() : null;
	}

	public String getCatalog() {
		Table table = classDetails.getDirectAnnotationUsage(Table.class);
		return table != null && table.catalog() != null && !table.catalog().isEmpty()
				? table.catalog() : null;
	}

	public String getComment() {
		return comment;
	}

	// --- Class-level attributes ---

	public boolean isMutable() {
		return !classDetails.hasDirectAnnotationUsage(Immutable.class);
	}

	public boolean isDynamicUpdate() {
		return classDetails.hasDirectAnnotationUsage(DynamicUpdate.class);
	}

	public boolean isDynamicInsert() {
		return classDetails.hasDirectAnnotationUsage(DynamicInsert.class);
	}

	public int getBatchSize() {
		BatchSize bs = classDetails.getDirectAnnotationUsage(BatchSize.class);
		return bs != null ? bs.size() : 0;
	}

	public String getCacheUsage() {
		Cache cache = classDetails.getDirectAnnotationUsage(Cache.class);
		if (cache == null || cache.usage() == CacheConcurrencyStrategy.NONE) {
			return null;
		}
		return cache.usage().name().toLowerCase().replace('_', '-');
	}

	public String getCacheRegion() {
		Cache cache = classDetails.getDirectAnnotationUsage(Cache.class);
		return cache != null && cache.region() != null && !cache.region().isEmpty()
				? cache.region() : null;
	}

	public String getCacheInclude() {
		Cache cache = classDetails.getDirectAnnotationUsage(Cache.class);
		return cache != null && !cache.includeLazy() ? "non-lazy" : null;
	}

	public String getWhere() {
		SQLRestriction sr = classDetails.getDirectAnnotationUsage(SQLRestriction.class);
		return sr != null ? sr.value() : null;
	}

	public boolean isAbstract() {
		return classDetails.isAbstract();
	}

	public String getOptimisticLockMode() {
		OptimisticLocking ol = classDetails.getDirectAnnotationUsage(OptimisticLocking.class);
		if (ol == null || ol.type() == OptimisticLockType.VERSION) {
			return null;
		}
		return ol.type().name().toLowerCase();
	}

	public String getRowId() {
		RowId rid = classDetails.getDirectAnnotationUsage(RowId.class);
		return rid != null && rid.value() != null && !rid.value().isEmpty()
				? rid.value() : null;
	}

	public String getSubselect() {
		Subselect ss = classDetails.getDirectAnnotationUsage(Subselect.class);
		return ss != null ? ss.value() : null;
	}

	public boolean isConcreteProxy() {
		return classDetails.hasDirectAnnotationUsage(ConcreteProxy.class);
	}

	public String getEntityName() {
		jakarta.persistence.Entity entity = classDetails.getDirectAnnotationUsage(jakarta.persistence.Entity.class);
		if (entity == null || entity.name() == null || entity.name().isEmpty()) {
			return null;
		}
		String simpleName = getClassName();
		int dot = simpleName.lastIndexOf('.');
		if (dot >= 0) {
			simpleName = simpleName.substring(dot + 1);
		}
		return entity.name().equals(simpleName) ? null : entity.name();
	}

	// --- Inheritance ---

	public boolean isSubclass() {
		ClassDetails superClass = classDetails.getSuperClass();
		return superClass != null
				&& !"java.lang.Object".equals(superClass.getClassName());
	}

	public String getParentClassName() {
		if (!isSubclass()) {
			return null;
		}
		String name = classDetails.getSuperClass().getClassName();
		return name.startsWith(".") ? name.substring(1) : name;
	}

	public String getClassTag() {
		if (!isSubclass()) {
			return "class";
		}
		Inheritance inh = classDetails.getDirectAnnotationUsage(Inheritance.class);
		if (inh != null && inh.strategy() == InheritanceType.TABLE_PER_CLASS) {
			return "union-subclass";
		}
		if (classDetails.hasDirectAnnotationUsage(PrimaryKeyJoinColumn.class)) {
			return "joined-subclass";
		}
		return "subclass";
	}

	public boolean needsDiscriminator() {
		return classDetails.hasDirectAnnotationUsage(DiscriminatorColumn.class);
	}

	public String getDiscriminatorColumnName() {
		DiscriminatorColumn dc = classDetails.getDirectAnnotationUsage(DiscriminatorColumn.class);
		return dc != null ? dc.name() : null;
	}

	public String getDiscriminatorTypeName() {
		DiscriminatorColumn dc = classDetails.getDirectAnnotationUsage(DiscriminatorColumn.class);
		if (dc == null) {
			return "string";
		}
		return switch (dc.discriminatorType()) {
			case STRING -> "string";
			case CHAR -> "character";
			case INTEGER -> "integer";
		};
	}

	public int getDiscriminatorColumnLength() {
		DiscriminatorColumn dc = classDetails.getDirectAnnotationUsage(DiscriminatorColumn.class);
		if (dc == null) {
			return 0;
		}
		return dc.length() != 31 ? dc.length() : 0;
	}

	public String getDiscriminatorValue() {
		DiscriminatorValue dv = classDetails.getDirectAnnotationUsage(DiscriminatorValue.class);
		return dv != null ? dv.value() : null;
	}

	public String getPrimaryKeyJoinColumnName() {
		PrimaryKeyJoinColumn pkjc = classDetails.getDirectAnnotationUsage(PrimaryKeyJoinColumn.class);
		return pkjc != null ? pkjc.name() : null;
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
					&& !isSecondaryTableField(field)) {
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

	public List<FieldDetails> getAnyFields() {
		return getFieldsWithAnnotation(Any.class);
	}

	public List<FieldDetails> getElementCollectionFields() {
		return getFieldsWithAnnotation(ElementCollection.class);
	}

	public List<FieldDetails> getManyToAnyFields() {
		return getFieldsWithAnnotation(ManyToAny.class);
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

	public String getElementCollectionElementType(FieldDetails field) {
		TypeDetails elementType = field.getElementType();
		if (elementType != null) {
			return JavaClassToHibernateType.toHibernateType(
					elementType.determineRawClass().getClassName());
		}
		return null;
	}

	public String getElementCollectionElementColumnName(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null && col.name() != null && !col.name().isEmpty() ? col.name() : null;
	}

	// --- Any ---

	public String getAnyIdType(FieldDetails field) {
		AnyKeyJavaClass akjc = field.getDirectAnnotationUsage(AnyKeyJavaClass.class);
		if (akjc != null) {
			return JavaClassToHibernateType.toHibernateType(akjc.value().getName());
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
		AnyDiscriminatorValue single = field.getDirectAnnotationUsage(AnyDiscriminatorValue.class);
		if (single != null) {
			result.add(new AnyMetaValue(single.discriminator(), single.entity().getName()));
		}
		AnyDiscriminatorValues container = field.getDirectAnnotationUsage(AnyDiscriminatorValues.class);
		if (container != null) {
			for (AnyDiscriminatorValue adv : container.value()) {
				result.add(new AnyMetaValue(adv.discriminator(), adv.entity().getName()));
			}
		}
		return result;
	}

	public record AnyMetaValue(String value, String entityClass) {}

	// --- Property-level attributes ---

	public String getFormula(FieldDetails field) {
		Formula formula = field.getDirectAnnotationUsage(Formula.class);
		return formula != null ? formula.value() : null;
	}

	public String getAccessType(FieldDetails field) {
		Access access = field.getDirectAnnotationUsage(Access.class);
		if (access == null || access.value() == AccessType.FIELD) {
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
		return params;
	}

	// --- Column / type attributes ---

	public String getColumnName(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null ? col.name() : field.getName();
	}

	public String getHibernateTypeName(FieldDetails field) {
		String className = field.getType().determineRawClass().getClassName();
		return JavaClassToHibernateType.toHibernateType(className);
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

	public String getGeneratorClass(FieldDetails field) {
		GeneratedValue gv = field.getDirectAnnotationUsage(GeneratedValue.class);
		return toGeneratorClass(gv != null ? gv.strategy() : null);
	}

	// --- ManyToOne ---

	public String getTargetEntityName(FieldDetails field) {
		return field.getType().determineRawClass().getClassName();
	}

	public boolean isManyToOneLazy(FieldDetails field) {
		ManyToOne m2o = field.getDirectAnnotationUsage(ManyToOne.class);
		return m2o != null && m2o.fetch() == jakarta.persistence.FetchType.LAZY;
	}

	public boolean isManyToOneOptional(FieldDetails field) {
		ManyToOne m2o = field.getDirectAnnotationUsage(ManyToOne.class);
		return m2o == null || m2o.optional();
	}

	// --- JoinColumn (shared by ManyToOne, OneToOne) ---

	public String getJoinColumnName(FieldDetails field) {
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		return jc != null ? jc.name() : null;
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
		return field.hasDirectAnnotationUsage(JoinColumn.class);
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
		return jt != null ? jt.name() : null;
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

	public String getManyToManyCascadeString(FieldDetails field) {
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		if (m2m == null || m2m.cascade().length == 0) {
			return null;
		}
		return getCascadeString(m2m.cascade());
	}

	// --- Collection type ---

	public String getCollectionTag(FieldDetails field) {
		if (field.hasDirectAnnotationUsage(Bag.class)) {
			return "bag";
		}
		if (field.hasDirectAnnotationUsage(OrderColumn.class)) {
			return "list";
		}
		if (field.hasDirectAnnotationUsage(CollectionId.class)) {
			return "idbag";
		}
		TypeDetails type = field.getType();
		if (type.isImplementor(java.util.Map.class)) {
			return "map";
		}
		if (field.isArray()) {
			TypeDetails elementType = field.getElementType();
			if (elementType != null
					&& elementType.getTypeKind() == TypeDetails.Kind.PRIMITIVE) {
				return "primitive-array";
			}
			return "array";
		}
		return "set";
	}

	public boolean isCollectionInverse(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		if (o2m != null) {
			return o2m.mappedBy() != null && !o2m.mappedBy().isEmpty();
		}
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		if (m2m != null) {
			return m2m.mappedBy() != null && !m2m.mappedBy().isEmpty();
		}
		return false;
	}

	public String getCollectionLazy(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		if (o2m != null && o2m.fetch() == FetchType.EAGER) {
			return "false";
		}
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		if (m2m != null && m2m.fetch() == FetchType.EAGER) {
			return "false";
		}
		return null;
	}

	public String getCollectionFetchMode(FieldDetails field) {
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

	public int getCollectionBatchSize(FieldDetails field) {
		BatchSize bs = field.getDirectAnnotationUsage(BatchSize.class);
		return bs != null ? bs.size() : 0;
	}

	public String getCollectionCascadeString(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		if (o2m != null && o2m.cascade().length > 0) {
			return getCascadeString(o2m.cascade());
		}
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		if (m2m != null && m2m.cascade().length > 0) {
			return getCascadeString(m2m.cascade());
		}
		return null;
	}

	public String getCollectionOrderBy(FieldDetails field) {
		OrderBy ob = field.getDirectAnnotationUsage(OrderBy.class);
		return ob != null && ob.value() != null && !ob.value().isEmpty()
				? ob.value() : null;
	}

	public String getCollectionCacheUsage(FieldDetails field) {
		Cache cache = field.getDirectAnnotationUsage(Cache.class);
		if (cache == null || cache.usage() == CacheConcurrencyStrategy.NONE) {
			return null;
		}
		return cache.usage().name().toLowerCase().replace('_', '-');
	}

	public String getCollectionCacheRegion(FieldDetails field) {
		Cache cache = field.getDirectAnnotationUsage(Cache.class);
		return cache != null && cache.region() != null && !cache.region().isEmpty()
				? cache.region() : null;
	}

	public List<FilterInfo> getCollectionFilters(FieldDetails field) {
		List<FilterInfo> result = new ArrayList<>();
		Filter single = field.getDirectAnnotationUsage(Filter.class);
		if (single != null) {
			result.add(new FilterInfo(single.name(), single.condition()));
		}
		Filters container = field.getDirectAnnotationUsage(Filters.class);
		if (container != null) {
			for (Filter f : container.value()) {
				result.add(new FilterInfo(f.name(), f.condition()));
			}
		}
		return result;
	}

	// --- List-specific ---

	public String getListIndexColumnName(FieldDetails field) {
		OrderColumn oc = field.getDirectAnnotationUsage(OrderColumn.class);
		return oc != null ? oc.name() : null;
	}

	// --- Map-specific ---

	public String getMapKeyColumnName(FieldDetails field) {
		MapKeyColumn mkc = field.getDirectAnnotationUsage(MapKeyColumn.class);
		return mkc != null ? mkc.name() : null;
	}

	public String getMapKeyType(FieldDetails field) {
		TypeDetails mapKeyType = field.getMapKeyType();
		if (mapKeyType != null) {
			return mapKeyType.determineRawClass().getClassName();
		}
		return null;
	}

	// --- IdBag-specific ---

	public String getCollectionIdColumnName(FieldDetails field) {
		CollectionId cid = field.getDirectAnnotationUsage(CollectionId.class);
		return cid != null ? cid.column().name() : null;
	}

	public String getCollectionIdGenerator(FieldDetails field) {
		CollectionId cid = field.getDirectAnnotationUsage(CollectionId.class);
		return cid != null && cid.generator() != null && !cid.generator().isEmpty()
				? cid.generator() : null;
	}

	// --- Collection element type ---

	public String getCollectionElementType(FieldDetails field) {
		TypeDetails elementType = field.getElementType();
		return elementType != null ? elementType.determineRawClass().getClassName() : null;
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

	// --- Meta attributes ---

	public Map<String, List<String>> getMetaAttributes() {
		return metaAttributes;
	}

	public List<String> getMetaAttribute(String name) {
		return metaAttributes.getOrDefault(name, Collections.emptyList());
	}

	public Map<String, List<String>> getFieldMetaAttributes(FieldDetails field) {
		return fieldMetaAttributes.getOrDefault(field.getName(), Collections.emptyMap());
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
		Map<String, String> params = new java.util.LinkedHashMap<>();
		if (fd.parameters() != null) {
			for (ParamDef pd : fd.parameters()) {
				params.put(pd.name(), JavaClassToHibernateType.toHibernateType(pd.type().getName()));
			}
		}
		return new FilterDefInfo(fd.name(), fd.defaultCondition(), params);
	}

	public record FilterInfo(String name, String condition) {}

	public record FilterDefInfo(String name, String defaultCondition, Map<String, String> parameters) {}

	// --- Named queries ---

	public List<NamedQueryInfo> getNamedQueries() {
		List<NamedQueryInfo> result = new ArrayList<>();
		// Check Hibernate @NamedQuery (has typed attributes)
		org.hibernate.annotations.NamedQuery hibSingle =
				classDetails.getDirectAnnotationUsage(org.hibernate.annotations.NamedQuery.class);
		if (hibSingle != null) {
			result.add(toNamedQueryInfo(hibSingle));
		}
		org.hibernate.annotations.NamedQueries hibContainer =
				classDetails.getDirectAnnotationUsage(org.hibernate.annotations.NamedQueries.class);
		if (hibContainer != null) {
			for (org.hibernate.annotations.NamedQuery nq : hibContainer.value()) {
				result.add(toNamedQueryInfo(nq));
			}
		}
		// Check JPA @NamedQuery (no typed attributes for flush-mode etc.)
		if (result.isEmpty()) {
			NamedQuery single = classDetails.getDirectAnnotationUsage(NamedQuery.class);
			if (single != null) {
				result.add(new NamedQueryInfo(single.name(), single.query(),
						"", false, "", -1, -1, "", false));
			}
			NamedQueries container = classDetails.getDirectAnnotationUsage(NamedQueries.class);
			if (container != null) {
				for (NamedQuery nq : container.value()) {
					result.add(new NamedQueryInfo(nq.name(), nq.query(),
							"", false, "", -1, -1, "", false));
				}
			}
		}
		return result;
	}

	private NamedQueryInfo toNamedQueryInfo(org.hibernate.annotations.NamedQuery nq) {
		String flushMode = nq.flushMode() != FlushModeType.PERSISTENCE_CONTEXT
				? nq.flushMode().name().toLowerCase() : "";
		return new NamedQueryInfo(nq.name(), nq.query(), flushMode,
				nq.cacheable(), nq.cacheRegion(), nq.fetchSize(), nq.timeout(),
				nq.comment(), nq.readOnly());
	}

	public List<NamedNativeQueryInfo> getNamedNativeQueries() {
		List<NamedNativeQueryInfo> result = new ArrayList<>();
		// Check Hibernate @NamedNativeQuery (has typed attributes)
		org.hibernate.annotations.NamedNativeQuery hibSingle =
				classDetails.getDirectAnnotationUsage(org.hibernate.annotations.NamedNativeQuery.class);
		if (hibSingle != null) {
			result.add(toNamedNativeQueryInfo(hibSingle.name(), hibSingle.query(),
					hibSingle.flushMode(), hibSingle.cacheable(), hibSingle.cacheRegion(),
					hibSingle.fetchSize(), hibSingle.timeout(), hibSingle.comment(),
					hibSingle.readOnly(), hibSingle.querySpaces(),
					hibSingle.resultClass(), hibSingle.resultSetMapping()));
		}
		org.hibernate.annotations.NamedNativeQueries hibContainer =
				classDetails.getDirectAnnotationUsage(org.hibernate.annotations.NamedNativeQueries.class);
		if (hibContainer != null) {
			for (org.hibernate.annotations.NamedNativeQuery nnq : hibContainer.value()) {
				result.add(toNamedNativeQueryInfo(nnq.name(), nnq.query(),
						nnq.flushMode(), nnq.cacheable(), nnq.cacheRegion(),
						nnq.fetchSize(), nnq.timeout(), nnq.comment(),
						nnq.readOnly(), nnq.querySpaces(),
						nnq.resultClass(), nnq.resultSetMapping()));
			}
		}
		// Check JPA @NamedNativeQuery (no typed attributes for flush-mode etc.)
		if (result.isEmpty()) {
			NamedNativeQuery single = classDetails.getDirectAnnotationUsage(NamedNativeQuery.class);
			if (single != null) {
				result.add(toNamedNativeQueryInfo(single.name(), single.query(),
						FlushModeType.PERSISTENCE_CONTEXT, false, "", -1, -1, "",
						false, new String[0], single.resultClass(), single.resultSetMapping()));
			}
			NamedNativeQueries container = classDetails.getDirectAnnotationUsage(NamedNativeQueries.class);
			if (container != null) {
				for (NamedNativeQuery nnq : container.value()) {
					result.add(toNamedNativeQueryInfo(nnq.name(), nnq.query(),
							FlushModeType.PERSISTENCE_CONTEXT, false, "", -1, -1, "",
							false, new String[0], nnq.resultClass(), nnq.resultSetMapping()));
				}
			}
		}
		return result;
	}

	private NamedNativeQueryInfo toNamedNativeQueryInfo(
			String name, String query, FlushModeType flushModeType,
			boolean cacheable, String cacheRegion, int fetchSize, int timeout,
			String comment, boolean readOnly, String[] spaces,
			Class<?> resultClass, String resultSetMapping) {
		String flushMode = flushModeType != FlushModeType.PERSISTENCE_CONTEXT
				? flushModeType.name().toLowerCase() : "";
		List<String> querySpaces = spaces != null && spaces.length > 0
				? List.of(spaces) : List.of();
		List<EntityReturnInfo> entityReturns = new ArrayList<>();
		List<ScalarReturnInfo> scalarReturns = new ArrayList<>();
		// Resolve returns from resultClass
		if (resultClass != null && resultClass != void.class) {
			entityReturns.add(new EntityReturnInfo(resultClass.getName(), "", List.of()));
		}
		// Resolve returns from @SqlResultSetMapping
		if (resultSetMapping != null && !resultSetMapping.isEmpty()) {
			SqlResultSetMapping mapping = findSqlResultSetMapping(resultSetMapping);
			if (mapping != null) {
				for (EntityResult er : mapping.entities()) {
					List<FieldMappingInfo> fieldMappings = new ArrayList<>();
					for (FieldResult fr : er.fields()) {
						fieldMappings.add(new FieldMappingInfo(fr.name(), fr.column()));
					}
					entityReturns.add(new EntityReturnInfo(
							er.entityClass().getName(), er.discriminatorColumn(), fieldMappings));
				}
				for (ColumnResult cr : mapping.columns()) {
					scalarReturns.add(new ScalarReturnInfo(cr.name()));
				}
			}
		}
		return new NamedNativeQueryInfo(name, query, flushMode, cacheable, cacheRegion,
				fetchSize, timeout, comment, readOnly, querySpaces,
				entityReturns, scalarReturns);
	}

	private SqlResultSetMapping findSqlResultSetMapping(String name) {
		SqlResultSetMapping single = classDetails.getDirectAnnotationUsage(SqlResultSetMapping.class);
		if (single != null && name.equals(single.name())) {
			return single;
		}
		SqlResultSetMappings container = classDetails.getDirectAnnotationUsage(SqlResultSetMappings.class);
		if (container != null) {
			for (SqlResultSetMapping mapping : container.value()) {
				if (name.equals(mapping.name())) {
					return mapping;
				}
			}
		}
		return null;
	}

	public record NamedQueryInfo(String name, String query, String flushMode,
								 boolean cacheable, String cacheRegion, int fetchSize,
								 int timeout, String comment, boolean readOnly) {}

	public record NamedNativeQueryInfo(String name, String query, String flushMode,
									   boolean cacheable, String cacheRegion, int fetchSize,
									   int timeout, String comment, boolean readOnly,
									   List<String> querySpaces,
									   List<EntityReturnInfo> entityReturns,
									   List<ScalarReturnInfo> scalarReturns) {}

	public record EntityReturnInfo(String entityClass, String discriminatorColumn,
								   List<FieldMappingInfo> fieldMappings) {}

	public record FieldMappingInfo(String name, String column) {}

	public record ScalarReturnInfo(String column) {}

	// --- SecondaryTable / Joins ---

	public List<JoinInfo> getJoins() {
		List<JoinInfo> result = new ArrayList<>();
		SecondaryTable single = classDetails.getDirectAnnotationUsage(SecondaryTable.class);
		if (single != null) {
			result.add(toJoinInfo(single));
		}
		SecondaryTables container = classDetails.getDirectAnnotationUsage(SecondaryTables.class);
		if (container != null) {
			for (SecondaryTable st : container.value()) {
				result.add(toJoinInfo(st));
			}
		}
		return result;
	}

	public List<FieldDetails> getJoinProperties(String tableName) {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			Column col = field.getDirectAnnotationUsage(Column.class);
			if (col != null && tableName.equals(col.table())) {
				result.add(field);
			}
		}
		return result;
	}

	private JoinInfo toJoinInfo(SecondaryTable st) {
		List<String> keyColumns = new ArrayList<>();
		if (st.pkJoinColumns() != null) {
			for (PrimaryKeyJoinColumn pkjc : st.pkJoinColumns()) {
				keyColumns.add(pkjc.name());
			}
		}
		return new JoinInfo(st.name(), keyColumns);
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
		SQLInsert si = classDetails.getDirectAnnotationUsage(SQLInsert.class);
		return si != null ? new CustomSqlInfo(si.sql(), si.callable()) : null;
	}

	public CustomSqlInfo getSQLUpdate() {
		SQLUpdate su = classDetails.getDirectAnnotationUsage(SQLUpdate.class);
		return su != null ? new CustomSqlInfo(su.sql(), su.callable()) : null;
	}

	public CustomSqlInfo getSQLDelete() {
		SQLDelete sd = classDetails.getDirectAnnotationUsage(SQLDelete.class);
		return sd != null ? new CustomSqlInfo(sd.sql(), sd.callable()) : null;
	}

	public CustomSqlInfo getSQLDeleteAll() {
		SQLDeleteAll sda = classDetails.getDirectAnnotationUsage(SQLDeleteAll.class);
		return sda != null ? new CustomSqlInfo(sda.sql(), sda.callable()) : null;
	}

	// --- Sort ---

	public String getSort(FieldDetails field) {
		if (field.hasDirectAnnotationUsage(SortNatural.class)) {
			return "natural";
		}
		SortComparator sc = field.getDirectAnnotationUsage(SortComparator.class);
		if (sc != null) {
			return sc.value().getName();
		}
		return null;
	}

	// --- Fetch profiles ---

	public record FetchProfileInfo(String name, List<FetchOverrideInfo> overrides) {}

	public record FetchOverrideInfo(String entity, String association, String style) {}

	public List<FetchProfileInfo> getFetchProfiles() {
		List<FetchProfileInfo> result = new ArrayList<>();
		FetchProfile single = classDetails.getDirectAnnotationUsage(FetchProfile.class);
		if (single != null) {
			result.add(toFetchProfileInfo(single));
		}
		FetchProfiles container = classDetails.getDirectAnnotationUsage(FetchProfiles.class);
		if (container != null) {
			for (FetchProfile fp : container.value()) {
				result.add(toFetchProfileInfo(fp));
			}
		}
		return result;
	}

	private FetchProfileInfo toFetchProfileInfo(FetchProfile fp) {
		List<FetchOverrideInfo> overrides = new ArrayList<>();
		for (FetchProfile.FetchOverride fo : fp.fetchOverrides()) {
			overrides.add(new FetchOverrideInfo(
					fo.entity().getName(), fo.association(), fo.mode().name().toLowerCase()));
		}
		return new FetchProfileInfo(fp.name(), overrides);
	}
}
