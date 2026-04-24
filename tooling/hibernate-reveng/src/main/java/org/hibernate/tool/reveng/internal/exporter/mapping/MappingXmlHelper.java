/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.mapping;

import java.util.List;
import java.util.Map;

import jakarta.persistence.CascadeType;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;

/**
 * Wraps a {@link ClassDetails} and provides template-friendly methods
 * for generating JPA mapping XML.
 *
 * @author Koen Aers
 */
public class MappingXmlHelper {

	private final MappingEntityInfoHelper entityInfoHelper;
	private final MappingQueryAndFilterHelper queryAndFilterHelper;
	private final MappingFieldAnnotationHelper fieldAnnotationHelper;
	private final MappingAssociationHelper associationHelper;

	MappingXmlHelper(ClassDetails classDetails) {
		this.entityInfoHelper = new MappingEntityInfoHelper(classDetails);
		this.queryAndFilterHelper = new MappingQueryAndFilterHelper(classDetails);
		this.fieldAnnotationHelper = new MappingFieldAnnotationHelper(classDetails);
		this.associationHelper = new MappingAssociationHelper(classDetails);
	}

	public MappingEntityInfoHelper getEntityInfoHelper() {
		return entityInfoHelper;
	}

	public MappingFieldAnnotationHelper getFieldAnnotationHelper() {
		return fieldAnnotationHelper;
	}

	public MappingQueryAndFilterHelper getQueryAndFilterHelper() {
		return queryAndFilterHelper;
	}

	// --- Entity / class ---

	public boolean isEmbeddable() {
		return entityInfoHelper.isEmbeddable();
	}

	public String getClassName() {
		return entityInfoHelper.getClassName();
	}

	public String getPackageName() {
		return entityInfoHelper.getPackageName();
	}

	// --- Entity-level Hibernate extensions ---

	public boolean isMutable() {
		return entityInfoHelper.isMutable();
	}

	public boolean isDynamicUpdate() {
		return entityInfoHelper.isDynamicUpdate();
	}

	public boolean isDynamicInsert() {
		return entityInfoHelper.isDynamicInsert();
	}

	public int getBatchSize() {
		return entityInfoHelper.getBatchSize();
	}

	public String getCacheAccessType() {
		return entityInfoHelper.getCacheAccessType();
	}

	public String getCacheRegion() {
		return entityInfoHelper.getCacheRegion();
	}

	public boolean isCacheIncludeLazy() {
		return entityInfoHelper.isCacheIncludeLazy();
	}

	public String getSqlRestriction() {
		return entityInfoHelper.getSqlRestriction();
	}

	public String getOptimisticLockMode() {
		return entityInfoHelper.getOptimisticLockMode();
	}

	public String getRowId() {
		return entityInfoHelper.getRowId();
	}

	public String getSubselect() {
		return entityInfoHelper.getSubselect();
	}

	public boolean isConcreteProxy() {
		return entityInfoHelper.isConcreteProxy();
	}

	public String getAccessType() {
		return entityInfoHelper.getClassAccessType();
	}

	// --- Table ---

	public String getTableName() {
		return entityInfoHelper.getTableName();
	}

	public String getSchema() {
		return entityInfoHelper.getSchema();
	}

	public String getCatalog() {
		return entityInfoHelper.getCatalog();
	}

	// --- Inheritance ---

	public boolean hasInheritance() {
		return entityInfoHelper.hasInheritance();
	}

	public String getInheritanceStrategy() {
		return entityInfoHelper.getInheritanceStrategy();
	}

	public String getDiscriminatorColumnName() {
		return entityInfoHelper.getDiscriminatorColumnName();
	}

	public String getDiscriminatorType() {
		return entityInfoHelper.getDiscriminatorType();
	}

	public int getDiscriminatorColumnLength() {
		return entityInfoHelper.getDiscriminatorColumnLength();
	}

	public String getDiscriminatorValue() {
		return entityInfoHelper.getDiscriminatorValue();
	}

	public String getPrimaryKeyJoinColumnName() {
		return entityInfoHelper.getPrimaryKeyJoinColumnName();
	}

	public List<String> getPrimaryKeyJoinColumnNames() {
		return entityInfoHelper.getPrimaryKeyJoinColumnNames();
	}

	// --- Secondary tables ---

	public List<SecondaryTableInfo> getSecondaryTables() {
		return entityInfoHelper.getSecondaryTables();
	}

	public record SecondaryTableInfo(String tableName, List<String> keyColumns) {}

	// --- Filters ---

	public List<FilterInfo> getFilters() {
		return queryAndFilterHelper.getFilters();
	}

	public List<FilterDefInfo> getFilterDefs() {
		return queryAndFilterHelper.getFilterDefs();
	}

	public List<FilterInfo> getCollectionFilters(FieldDetails field) {
		return queryAndFilterHelper.getCollectionFilters(field);
	}

	public record FilterInfo(String name, String condition) {}

	public record FilterDefInfo(String name, String defaultCondition, Map<String, String> parameters) {}

	// --- Named queries ---

	public List<NamedQueryInfo> getNamedQueries() {
		return queryAndFilterHelper.getNamedQueries();
	}

	public List<NamedQueryInfo> getNamedNativeQueries() {
		return queryAndFilterHelper.getNamedNativeQueries();
	}

	public record NamedQueryInfo(String name, String query) {}

	// --- Fetch profiles ---

	public List<FetchProfileInfo> getFetchProfiles() {
		return queryAndFilterHelper.getFetchProfiles();
	}

	public record FetchProfileInfo(String name, List<FetchOverrideInfo> overrides) {}

	public record FetchOverrideInfo(String entity, String association, String style) {}

	// --- SQL operations ---

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

	public record CustomSqlInfo(String sql, boolean callable) {}

	// --- Entity listeners ---

	public List<String> getEntityListenerClassNames() {
		return queryAndFilterHelper.getEntityListenerClassNames();
	}

	// --- Lifecycle callbacks ---

	public List<LifecycleCallbackInfo> getLifecycleCallbacks() {
		return queryAndFilterHelper.getLifecycleCallbacks();
	}

	public record LifecycleCallbackInfo(String elementName, String methodName) {}

	// --- Field categorization ---

	public FieldDetails getCompositeIdField() {
		return fieldAnnotationHelper.getCompositeIdField();
	}

	public List<FieldDetails> getIdFields() {
		return fieldAnnotationHelper.getIdFields();
	}

	public List<FieldDetails> getBasicFields() {
		return fieldAnnotationHelper.getBasicFields();
	}

	public List<FieldDetails> getNaturalIdFields() {
		return fieldAnnotationHelper.getNaturalIdFields();
	}

	public boolean isNaturalIdMutable() {
		return fieldAnnotationHelper.isNaturalIdMutable();
	}

	public List<FieldDetails> getVersionFields() {
		return fieldAnnotationHelper.getVersionFields();
	}

	public List<FieldDetails> getManyToOneFields() {
		return fieldAnnotationHelper.getManyToOneFields();
	}

	public List<FieldDetails> getOneToManyFields() {
		return fieldAnnotationHelper.getOneToManyFields();
	}

	public List<FieldDetails> getOneToOneFields() {
		return fieldAnnotationHelper.getOneToOneFields();
	}

	public List<FieldDetails> getManyToManyFields() {
		return fieldAnnotationHelper.getManyToManyFields();
	}

	public List<FieldDetails> getEmbeddedFields() {
		return fieldAnnotationHelper.getEmbeddedFields();
	}

	public List<FieldDetails> getAnyFields() {
		return fieldAnnotationHelper.getAnyFields();
	}

	public List<FieldDetails> getManyToAnyFields() {
		return fieldAnnotationHelper.getManyToAnyFields();
	}

	public List<FieldDetails> getElementCollectionFields() {
		return fieldAnnotationHelper.getElementCollectionFields();
	}

	// --- Column attributes ---

	public String getColumnName(FieldDetails field) {
		return fieldAnnotationHelper.getColumnName(field);
	}

	public boolean isNullable(FieldDetails field) {
		return fieldAnnotationHelper.isNullable(field);
	}

	public boolean isUnique(FieldDetails field) {
		return fieldAnnotationHelper.isUnique(field);
	}

	public int getLength(FieldDetails field) {
		return fieldAnnotationHelper.getLength(field);
	}

	public int getPrecision(FieldDetails field) {
		return fieldAnnotationHelper.getPrecision(field);
	}

	public int getScale(FieldDetails field) {
		return fieldAnnotationHelper.getScale(field);
	}

	public String getColumnDefinition(FieldDetails field) {
		return fieldAnnotationHelper.getColumnDefinition(field);
	}

	public boolean isInsertable(FieldDetails field) {
		return fieldAnnotationHelper.isInsertable(field);
	}

	public boolean isUpdatable(FieldDetails field) {
		return fieldAnnotationHelper.isUpdatable(field);
	}

	public boolean isLob(FieldDetails field) {
		return fieldAnnotationHelper.isLob(field);
	}

	public String getTemporalType(FieldDetails field) {
		return fieldAnnotationHelper.getTemporalType(field);
	}

	public String getGenerationType(FieldDetails field) {
		return fieldAnnotationHelper.getGenerationType(field);
	}

	public String getGeneratorName(FieldDetails field) {
		return fieldAnnotationHelper.getGeneratorName(field);
	}

	public SequenceGeneratorInfo getSequenceGenerator(FieldDetails field) {
		return fieldAnnotationHelper.getSequenceGenerator(field);
	}

	public record SequenceGeneratorInfo(String name, String sequenceName,
			Integer allocationSize, Integer initialValue) {}

	public TableGeneratorInfo getTableGenerator(FieldDetails field) {
		return fieldAnnotationHelper.getTableGenerator(field);
	}

	public record TableGeneratorInfo(String name, String table, String pkColumnName,
			String valueColumnName, String pkColumnValue,
			Integer allocationSize, Integer initialValue) {}

	// --- ManyToOne ---

	public String getTargetEntityName(FieldDetails field) {
		return associationHelper.getTargetEntityName(field);
	}

	public String getManyToOneFetchType(FieldDetails field) {
		return associationHelper.getManyToOneFetchType(field);
	}

	public boolean isManyToOneOptional(FieldDetails field) {
		return associationHelper.isManyToOneOptional(field);
	}

	// --- JoinColumn ---

	public String getJoinColumnName(FieldDetails field) {
		return associationHelper.getJoinColumnName(field);
	}

	public String getReferencedColumnName(FieldDetails field) {
		return associationHelper.getReferencedColumnName(field);
	}

	public List<JoinColumnInfo> getJoinColumns(FieldDetails field) {
		return associationHelper.getJoinColumns(field);
	}

	public record JoinColumnInfo(String name, String referencedColumnName) {}

	// --- OneToMany ---

	public String getOneToManyTargetEntity(FieldDetails field) {
		return associationHelper.getOneToManyTargetEntity(field);
	}

	public String getOneToManyMappedBy(FieldDetails field) {
		return associationHelper.getOneToManyMappedBy(field);
	}

	public String getOneToManyFetchType(FieldDetails field) {
		return associationHelper.getOneToManyFetchType(field);
	}

	public boolean isOneToManyOrphanRemoval(FieldDetails field) {
		return associationHelper.isOneToManyOrphanRemoval(field);
	}

	public List<CascadeType> getOneToManyCascadeTypes(FieldDetails field) {
		return associationHelper.getOneToManyCascadeTypes(field);
	}

	// --- Ordering ---

	public String getOrderBy(FieldDetails field) {
		return associationHelper.getOrderBy(field);
	}

	public String getOrderColumnName(FieldDetails field) {
		return associationHelper.getOrderColumnName(field);
	}

	// --- Map key ---

	public String getMapKeyName(FieldDetails field) {
		return associationHelper.getMapKeyName(field);
	}

	public String getMapKeyColumnName(FieldDetails field) {
		return associationHelper.getMapKeyColumnName(field);
	}

	public String getMapKeyJoinColumnName(FieldDetails field) {
		return associationHelper.getMapKeyJoinColumnName(field);
	}

	// --- Fetch mode ---

	public String getFetchMode(FieldDetails field) {
		return associationHelper.getFetchMode(field);
	}

	// --- NotFound ---

	public String getNotFoundAction(FieldDetails field) {
		return associationHelper.getNotFoundAction(field);
	}

	// --- Convert ---

	public String getConverterClassName(FieldDetails field) {
		return associationHelper.getConverterClassName(field);
	}

	// --- OneToOne ---

	public String getOneToOneMappedBy(FieldDetails field) {
		return associationHelper.getOneToOneMappedBy(field);
	}

	public String getOneToOneFetchType(FieldDetails field) {
		return associationHelper.getOneToOneFetchType(field);
	}

	public boolean isOneToOneOptional(FieldDetails field) {
		return associationHelper.isOneToOneOptional(field);
	}

	public boolean isOneToOneOrphanRemoval(FieldDetails field) {
		return associationHelper.isOneToOneOrphanRemoval(field);
	}

	public List<CascadeType> getOneToOneCascadeTypes(FieldDetails field) {
		return associationHelper.getOneToOneCascadeTypes(field);
	}

	// --- ManyToMany ---

	public String getManyToManyTargetEntity(FieldDetails field) {
		return associationHelper.getManyToManyTargetEntity(field);
	}

	public String getManyToManyMappedBy(FieldDetails field) {
		return associationHelper.getManyToManyMappedBy(field);
	}

	public String getManyToManyFetchType(FieldDetails field) {
		return associationHelper.getManyToManyFetchType(field);
	}

	public List<CascadeType> getManyToManyCascadeTypes(FieldDetails field) {
		return associationHelper.getManyToManyCascadeTypes(field);
	}

	public String getJoinTableName(FieldDetails field) {
		return associationHelper.getJoinTableName(field);
	}

	public String getJoinTableSchema(FieldDetails field) {
		return associationHelper.getJoinTableSchema(field);
	}

	public String getJoinTableCatalog(FieldDetails field) {
		return associationHelper.getJoinTableCatalog(field);
	}

	public String getJoinTableJoinColumnName(FieldDetails field) {
		return associationHelper.getJoinTableJoinColumnName(field);
	}

	public String getJoinTableInverseJoinColumnName(FieldDetails field) {
		return associationHelper.getJoinTableInverseJoinColumnName(field);
	}

	public List<String> getJoinTableJoinColumnNames(FieldDetails field) {
		return associationHelper.getJoinTableJoinColumnNames(field);
	}

	public List<String> getJoinTableInverseJoinColumnNames(FieldDetails field) {
		return associationHelper.getJoinTableInverseJoinColumnNames(field);
	}

	// --- Embedded / EmbeddedId attribute overrides ---

	public List<AttributeOverrideInfo> getAttributeOverrides(FieldDetails field) {
		return associationHelper.getAttributeOverrides(field);
	}

	public record AttributeOverrideInfo(String fieldName, String columnName) {}

	// --- Property-level attributes ---

	public String getColumnTable(FieldDetails field) {
		return associationHelper.getColumnTable(field);
	}

	public String getFormula(FieldDetails field) {
		return associationHelper.getFormula(field);
	}

	public boolean isPropertyLazy(FieldDetails field) {
		return associationHelper.isPropertyLazy(field);
	}

	public String getAccessType(FieldDetails field) {
		return associationHelper.getFieldAccessType(field);
	}

	public boolean isOptimisticLockExcluded(FieldDetails field) {
		return associationHelper.isOptimisticLockExcluded(field);
	}

	// --- Any ---

	public String getAnyDiscriminatorType(FieldDetails field) {
		return associationHelper.getAnyDiscriminatorType(field);
	}

	public String getAnyKeyType(FieldDetails field) {
		return associationHelper.getAnyKeyType(field);
	}

	public List<AnyDiscriminatorMapping> getAnyDiscriminatorMappings(FieldDetails field) {
		return associationHelper.getAnyDiscriminatorMappings(field);
	}

	public record AnyDiscriminatorMapping(String value, String entityClass) {}

	// --- ElementCollection ---

	public boolean isElementCollectionOfEmbeddable(FieldDetails field) {
		return associationHelper.isElementCollectionOfEmbeddable(field);
	}

	public String getElementCollectionTableName(FieldDetails field) {
		return associationHelper.getElementCollectionTableName(field);
	}

	public String getElementCollectionKeyColumnName(FieldDetails field) {
		return associationHelper.getElementCollectionKeyColumnName(field);
	}

	public String getElementCollectionTargetClass(FieldDetails field) {
		return associationHelper.getElementCollectionTargetClass(field);
	}

	public String getElementCollectionColumnName(FieldDetails field) {
		return associationHelper.getElementCollectionColumnName(field);
	}

	// --- Sort ---

	public boolean isSortNatural(FieldDetails field) {
		return associationHelper.isSortNatural(field);
	}

	public String getSortComparatorClass(FieldDetails field) {
		return associationHelper.getSortComparatorClass(field);
	}
}
