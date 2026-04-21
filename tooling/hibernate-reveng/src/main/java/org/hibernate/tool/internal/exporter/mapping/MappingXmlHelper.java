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

	MappingXmlHelper(ClassDetails classDetails) {
		this.entityInfoHelper = new MappingEntityInfoHelper(classDetails);
		this.queryAndFilterHelper = new MappingQueryAndFilterHelper(classDetails);
		this.fieldAnnotationHelper = new MappingFieldAnnotationHelper(classDetails);
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
		return fieldAnnotationHelper.getTargetEntityName(field);
	}

	public String getManyToOneFetchType(FieldDetails field) {
		return fieldAnnotationHelper.getManyToOneFetchType(field);
	}

	public boolean isManyToOneOptional(FieldDetails field) {
		return fieldAnnotationHelper.isManyToOneOptional(field);
	}

	// --- JoinColumn ---

	public String getJoinColumnName(FieldDetails field) {
		return fieldAnnotationHelper.getJoinColumnName(field);
	}

	public String getReferencedColumnName(FieldDetails field) {
		return fieldAnnotationHelper.getReferencedColumnName(field);
	}

	public List<JoinColumnInfo> getJoinColumns(FieldDetails field) {
		return fieldAnnotationHelper.getJoinColumns(field);
	}

	public record JoinColumnInfo(String name, String referencedColumnName) {}

	// --- OneToMany ---

	public String getOneToManyTargetEntity(FieldDetails field) {
		return fieldAnnotationHelper.getOneToManyTargetEntity(field);
	}

	public String getOneToManyMappedBy(FieldDetails field) {
		return fieldAnnotationHelper.getOneToManyMappedBy(field);
	}

	public String getOneToManyFetchType(FieldDetails field) {
		return fieldAnnotationHelper.getOneToManyFetchType(field);
	}

	public boolean isOneToManyOrphanRemoval(FieldDetails field) {
		return fieldAnnotationHelper.isOneToManyOrphanRemoval(field);
	}

	public List<CascadeType> getOneToManyCascadeTypes(FieldDetails field) {
		return fieldAnnotationHelper.getOneToManyCascadeTypes(field);
	}

	// --- Ordering ---

	public String getOrderBy(FieldDetails field) {
		return fieldAnnotationHelper.getOrderBy(field);
	}

	public String getOrderColumnName(FieldDetails field) {
		return fieldAnnotationHelper.getOrderColumnName(field);
	}

	// --- Map key ---

	public String getMapKeyName(FieldDetails field) {
		return fieldAnnotationHelper.getMapKeyName(field);
	}

	public String getMapKeyColumnName(FieldDetails field) {
		return fieldAnnotationHelper.getMapKeyColumnName(field);
	}

	public String getMapKeyJoinColumnName(FieldDetails field) {
		return fieldAnnotationHelper.getMapKeyJoinColumnName(field);
	}

	// --- Fetch mode ---

	public String getFetchMode(FieldDetails field) {
		return fieldAnnotationHelper.getFetchMode(field);
	}

	// --- NotFound ---

	public String getNotFoundAction(FieldDetails field) {
		return fieldAnnotationHelper.getNotFoundAction(field);
	}

	// --- Convert ---

	public String getConverterClassName(FieldDetails field) {
		return fieldAnnotationHelper.getConverterClassName(field);
	}

	// --- OneToOne ---

	public String getOneToOneMappedBy(FieldDetails field) {
		return fieldAnnotationHelper.getOneToOneMappedBy(field);
	}

	public String getOneToOneFetchType(FieldDetails field) {
		return fieldAnnotationHelper.getOneToOneFetchType(field);
	}

	public boolean isOneToOneOptional(FieldDetails field) {
		return fieldAnnotationHelper.isOneToOneOptional(field);
	}

	public boolean isOneToOneOrphanRemoval(FieldDetails field) {
		return fieldAnnotationHelper.isOneToOneOrphanRemoval(field);
	}

	public List<CascadeType> getOneToOneCascadeTypes(FieldDetails field) {
		return fieldAnnotationHelper.getOneToOneCascadeTypes(field);
	}

	// --- ManyToMany ---

	public String getManyToManyTargetEntity(FieldDetails field) {
		return fieldAnnotationHelper.getManyToManyTargetEntity(field);
	}

	public String getManyToManyMappedBy(FieldDetails field) {
		return fieldAnnotationHelper.getManyToManyMappedBy(field);
	}

	public String getManyToManyFetchType(FieldDetails field) {
		return fieldAnnotationHelper.getManyToManyFetchType(field);
	}

	public List<CascadeType> getManyToManyCascadeTypes(FieldDetails field) {
		return fieldAnnotationHelper.getManyToManyCascadeTypes(field);
	}

	public String getJoinTableName(FieldDetails field) {
		return fieldAnnotationHelper.getJoinTableName(field);
	}

	public String getJoinTableSchema(FieldDetails field) {
		return fieldAnnotationHelper.getJoinTableSchema(field);
	}

	public String getJoinTableCatalog(FieldDetails field) {
		return fieldAnnotationHelper.getJoinTableCatalog(field);
	}

	public String getJoinTableJoinColumnName(FieldDetails field) {
		return fieldAnnotationHelper.getJoinTableJoinColumnName(field);
	}

	public String getJoinTableInverseJoinColumnName(FieldDetails field) {
		return fieldAnnotationHelper.getJoinTableInverseJoinColumnName(field);
	}

	public List<String> getJoinTableJoinColumnNames(FieldDetails field) {
		return fieldAnnotationHelper.getJoinTableJoinColumnNames(field);
	}

	public List<String> getJoinTableInverseJoinColumnNames(FieldDetails field) {
		return fieldAnnotationHelper.getJoinTableInverseJoinColumnNames(field);
	}

	// --- Embedded / EmbeddedId attribute overrides ---

	public List<AttributeOverrideInfo> getAttributeOverrides(FieldDetails field) {
		return fieldAnnotationHelper.getAttributeOverrides(field);
	}

	public record AttributeOverrideInfo(String fieldName, String columnName) {}

	// --- Property-level attributes ---

	public String getColumnTable(FieldDetails field) {
		return fieldAnnotationHelper.getColumnTable(field);
	}

	public String getFormula(FieldDetails field) {
		return fieldAnnotationHelper.getFormula(field);
	}

	public boolean isPropertyLazy(FieldDetails field) {
		return fieldAnnotationHelper.isPropertyLazy(field);
	}

	public String getAccessType(FieldDetails field) {
		return fieldAnnotationHelper.getFieldAccessType(field);
	}

	public boolean isOptimisticLockExcluded(FieldDetails field) {
		return fieldAnnotationHelper.isOptimisticLockExcluded(field);
	}

	// --- Any ---

	public String getAnyDiscriminatorType(FieldDetails field) {
		return fieldAnnotationHelper.getAnyDiscriminatorType(field);
	}

	public String getAnyKeyType(FieldDetails field) {
		return fieldAnnotationHelper.getAnyKeyType(field);
	}

	public List<AnyDiscriminatorMapping> getAnyDiscriminatorMappings(FieldDetails field) {
		return fieldAnnotationHelper.getAnyDiscriminatorMappings(field);
	}

	public record AnyDiscriminatorMapping(String value, String entityClass) {}

	// --- ElementCollection ---

	public boolean isElementCollectionOfEmbeddable(FieldDetails field) {
		return fieldAnnotationHelper.isElementCollectionOfEmbeddable(field);
	}

	public String getElementCollectionTableName(FieldDetails field) {
		return fieldAnnotationHelper.getElementCollectionTableName(field);
	}

	public String getElementCollectionKeyColumnName(FieldDetails field) {
		return fieldAnnotationHelper.getElementCollectionKeyColumnName(field);
	}

	public String getElementCollectionTargetClass(FieldDetails field) {
		return fieldAnnotationHelper.getElementCollectionTargetClass(field);
	}

	public String getElementCollectionColumnName(FieldDetails field) {
		return fieldAnnotationHelper.getElementCollectionColumnName(field);
	}

	// --- Sort ---

	public boolean isSortNatural(FieldDetails field) {
		return fieldAnnotationHelper.isSortNatural(field);
	}

	public String getSortComparatorClass(FieldDetails field) {
		return fieldAnnotationHelper.getSortComparatorClass(field);
	}
}
