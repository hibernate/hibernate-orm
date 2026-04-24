/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.hbm;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.GenerationType;

import org.hibernate.tool.reveng.internal.util.CascadeUtil;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;

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
	private final HbmFieldCategorizationHelper fieldCategorizationHelper;
	private final HbmFieldAttributeHelper fieldAttributeHelper;
	private final HbmAssociationAttributeHelper associationAttributeHelper;

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
		this.classInfoHelper = new HbmClassInfoHelper(classDetails, comment, this.metaAttributes, this.imports);
		this.fieldCategorizationHelper = new HbmFieldCategorizationHelper(classDetails, this.fieldMetaAttributes, this.metaAttributes);
		this.fieldAttributeHelper = new HbmFieldAttributeHelper(this.fieldMetaAttributes);
		this.associationAttributeHelper = new HbmAssociationAttributeHelper(this.fieldMetaAttributes, this.allClassMetaAttributes);
	}

	// --- Delegate accessors ---

	public HbmClassInfoHelper getClassInfoHelper() {
		return classInfoHelper;
	}

	public HbmFieldCategorizationHelper getFieldCategorizationHelper() {
		return fieldCategorizationHelper;
	}

	public HbmFieldAttributeHelper getFieldAttributeHelper() {
		return fieldAttributeHelper;
	}

	public HbmAssociationAttributeHelper getAssociationAttributeHelper() {
		return associationAttributeHelper;
	}

	public HbmCollectionAttributeHelper getCollectionAttributeHelper() {
		return collectionAttributeHelper;
	}

	public HbmQueryAndFilterHelper getQueryAndFilterHelper() {
		return queryAndFilterHelper;
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
		return fieldCategorizationHelper.getCompositeIdField();
	}

	public String getCompositeIdClassName() {
		return fieldCategorizationHelper.getCompositeIdClassName();
	}

	public boolean hasIdClass() {
		return fieldCategorizationHelper.hasIdClass();
	}

	public String getIdClassName() {
		return fieldCategorizationHelper.getIdClassName();
	}

	public List<FieldDetails> getCompositeIdAllFields() {
		return fieldCategorizationHelper.getCompositeIdAllFields();
	}

	public List<FieldDetails> getCompositeIdKeyProperties() {
		return fieldCategorizationHelper.getCompositeIdKeyProperties();
	}

	public boolean hasCompositeIdKeyManyToOnes() {
		return fieldCategorizationHelper.hasCompositeIdKeyManyToOnes();
	}

	public List<FieldDetails> getCompositeIdKeyManyToOnes() {
		return fieldCategorizationHelper.getCompositeIdKeyManyToOnes();
	}

	public String getKeyManyToOneClassName(FieldDetails field) {
		return fieldCategorizationHelper.getKeyManyToOneClassName(field);
	}

	public String getKeyManyToOneColumnName(FieldDetails field) {
		return fieldCategorizationHelper.getKeyManyToOneColumnName(field);
	}

	public List<String> getKeyManyToOneColumnNames(FieldDetails field) {
		return fieldCategorizationHelper.getKeyManyToOneColumnNames(field);
	}

	public List<FieldDetails> getIdFields() {
		return fieldCategorizationHelper.getIdFields();
	}

	public List<FieldDetails> getBasicFields() {
		return fieldCategorizationHelper.getBasicFields();
	}

	public List<FieldDetails> getNaturalIdFields() {
		return fieldCategorizationHelper.getNaturalIdFields();
	}

	public boolean isNaturalIdMutable() {
		return fieldCategorizationHelper.isNaturalIdMutable();
	}

	public List<FieldDetails> getVersionFields() {
		return fieldCategorizationHelper.getVersionFields();
	}

	public List<FieldDetails> getManyToOneFields() {
		return fieldCategorizationHelper.getManyToOneFields();
	}

	public List<FieldDetails> getOneToOneFields() {
		return fieldCategorizationHelper.getOneToOneFields();
	}

	public List<FieldDetails> getConstrainedOneToOneAsM2OFields() {
		return fieldCategorizationHelper.getConstrainedOneToOneAsM2OFields();
	}

	public List<FieldDetails> getOneToManyFields() {
		return fieldCategorizationHelper.getOneToManyFields();
	}

	public List<FieldDetails> getManyToManyFields() {
		return fieldCategorizationHelper.getManyToManyFields();
	}

	public List<FieldDetails> getEmbeddedFields() {
		return fieldCategorizationHelper.getEmbeddedFields();
	}

	public List<FieldDetails> getAnyFields() {
		return fieldCategorizationHelper.getAnyFields();
	}

	public List<FieldDetails> getElementCollectionFields() {
		return fieldCategorizationHelper.getElementCollectionFields();
	}

	public List<FieldDetails> getManyToAnyFields() {
		return fieldCategorizationHelper.getManyToAnyFields();
	}

	// --- Dynamic component ---

	public List<FieldDetails> getDynamicComponentFields() {
		return fieldCategorizationHelper.getDynamicComponentFields();
	}

	public List<DynamicComponentProperty> getDynamicComponentProperties(FieldDetails field) {
		return fieldCategorizationHelper.getDynamicComponentProperties(field);
	}

	public record DynamicComponentProperty(String name, String type) {}

	// --- ElementCollection ---

	public boolean isElementCollectionOfEmbeddable(FieldDetails field) {
		return fieldCategorizationHelper.isElementCollectionOfEmbeddable(field);
	}

	public String getElementCollectionTableName(FieldDetails field) {
		return fieldCategorizationHelper.getElementCollectionTableName(field);
	}

	public String getElementCollectionTableSchema(FieldDetails field) {
		return fieldCategorizationHelper.getElementCollectionTableSchema(field);
	}

	public String getElementCollectionTableCatalog(FieldDetails field) {
		return fieldCategorizationHelper.getElementCollectionTableCatalog(field);
	}

	public String getElementCollectionKeyColumnName(FieldDetails field) {
		return fieldCategorizationHelper.getElementCollectionKeyColumnName(field);
	}

	public String getElementCollectionElementType(FieldDetails field) {
		return fieldCategorizationHelper.getElementCollectionElementType(field);
	}

	public String getElementCollectionElementColumnName(FieldDetails field) {
		return fieldCategorizationHelper.getElementCollectionElementColumnName(field);
	}

	// --- Composite element (embeddable inside ElementCollection) ---

	public List<FieldDetails> getCompositeElementProperties(FieldDetails field) {
		return fieldCategorizationHelper.getCompositeElementProperties(field);
	}

	public List<FieldDetails> getCompositeElementManyToOnes(FieldDetails field) {
		return fieldCategorizationHelper.getCompositeElementManyToOnes(field);
	}

	public List<FieldDetails> getCompositeElementEmbeddeds(FieldDetails field) {
		return fieldCategorizationHelper.getCompositeElementEmbeddeds(field);
	}

	public String getManyToOneCascadeString(FieldDetails field) {
		return associationAttributeHelper.getManyToOneCascadeString(field);
	}

	// --- Any ---

	public String getAnyIdType(FieldDetails field) {
		return fieldCategorizationHelper.getAnyIdType(field);
	}

	public String getAnyMetaType(FieldDetails field) {
		return fieldCategorizationHelper.getAnyMetaType(field);
	}

	public List<AnyMetaValue> getAnyMetaValues(FieldDetails field) {
		return fieldCategorizationHelper.getAnyMetaValues(field);
	}

	public record AnyMetaValue(String value, String entityClass) {}

	public String getAnyCascadeString(FieldDetails field) {
		return fieldCategorizationHelper.getAnyCascadeString(field);
	}

	public String getArrayElementClass(FieldDetails field) {
		return associationAttributeHelper.getArrayElementClass(field);
	}

	public String getManyToAnyFkColumnName(FieldDetails field) {
		return associationAttributeHelper.getManyToAnyFkColumnName(field);
	}

	// --- Property-level attributes ---

	public String getFormula(FieldDetails field) {
		return fieldAttributeHelper.getFormula(field);
	}

	public String getAccessType(FieldDetails field) {
		return fieldAttributeHelper.getAccessType(field);
	}

	public String getFetchMode(FieldDetails field) {
		return fieldAttributeHelper.getFetchMode(field);
	}

	public String getNotFoundAction(FieldDetails field) {
		return fieldAttributeHelper.getNotFoundAction(field);
	}

	public boolean isTimestamp(FieldDetails field) {
		return fieldAttributeHelper.isTimestamp(field);
	}

	public boolean isPropertyUpdatable(FieldDetails field) {
		return fieldAttributeHelper.isPropertyUpdatable(field);
	}

	public boolean isPropertyInsertable(FieldDetails field) {
		return fieldAttributeHelper.isPropertyInsertable(field);
	}

	public boolean isPropertyLazy(FieldDetails field) {
		return fieldAttributeHelper.isPropertyLazy(field);
	}

	public boolean isOptimisticLockExcluded(FieldDetails field) {
		return fieldAttributeHelper.isOptimisticLockExcluded(field);
	}

	// --- Generator parameters ---

	public Map<String, String> getGeneratorParameters(FieldDetails field) {
		return fieldAttributeHelper.getGeneratorParameters(field);
	}

	// --- Column / type attributes ---

	public String getColumnName(FieldDetails field) {
		return fieldAttributeHelper.getColumnName(field);
	}

	public String getHibernateTypeName(FieldDetails field) {
		return fieldAttributeHelper.getHibernateTypeName(field);
	}

	public boolean hasTypeParameters(FieldDetails field) {
		return fieldAttributeHelper.hasTypeParameters(field);
	}

	public Map<String, String> getTypeParameters(FieldDetails field) {
		return fieldAttributeHelper.getTypeParameters(field);
	}

	public String getColumnAttributes(FieldDetails field) {
		return fieldAttributeHelper.getColumnAttributes(field);
	}

	public String getColumnComment(FieldDetails field) {
		return fieldAttributeHelper.getColumnComment(field);
	}

	public String getGeneratorClass(FieldDetails field) {
		return fieldAttributeHelper.getGeneratorClass(field);
	}

	// --- ManyToOne ---

	public String getTargetEntityName(FieldDetails field) {
		return fieldAttributeHelper.getTargetEntityName(field);
	}

	public boolean isManyToOneEntityNameRef(FieldDetails field) {
		return associationAttributeHelper.isManyToOneEntityNameRef(field);
	}

	public String getManyToOneEntityName(FieldDetails field) {
		return associationAttributeHelper.getManyToOneEntityName(field);
	}

	public List<String> getManyToOneFormulas(FieldDetails field) {
		return associationAttributeHelper.getManyToOneFormulas(field);
	}

	// --- ManyToMany ---

	public boolean isManyToManyEntityNameRef(FieldDetails field) {
		return associationAttributeHelper.isManyToManyEntityNameRef(field);
	}

	public String getManyToManyEntityName(FieldDetails field) {
		return associationAttributeHelper.getManyToManyEntityName(field);
	}

	public List<String> getManyToManyFormulas(FieldDetails field) {
		return associationAttributeHelper.getManyToManyFormulas(field);
	}

	public boolean isManyToOneLazy(FieldDetails field) {
		return associationAttributeHelper.isManyToOneLazy(field);
	}

	public boolean isManyToOneUpdatable(FieldDetails field) {
		return associationAttributeHelper.isManyToOneUpdatable(field);
	}

	public boolean isManyToOneInsertable(FieldDetails field) {
		return associationAttributeHelper.isManyToOneInsertable(field);
	}

	public boolean isManyToOneOptional(FieldDetails field) {
		return associationAttributeHelper.isManyToOneOptional(field);
	}

	// --- JoinColumn (shared by ManyToOne, OneToOne) ---

	public String getPropertyRef(FieldDetails field) {
		return associationAttributeHelper.getPropertyRef(field);
	}

	public String getJoinColumnName(FieldDetails field) {
		return associationAttributeHelper.getJoinColumnName(field);
	}

	public List<String> getJoinColumnNames(FieldDetails field) {
		return associationAttributeHelper.getJoinColumnNames(field);
	}

	// --- OneToOne ---

	public String getOneToOneMappedBy(FieldDetails field) {
		return associationAttributeHelper.getOneToOneMappedBy(field);
	}

	public String getOneToOneCascadeString(FieldDetails field) {
		return associationAttributeHelper.getOneToOneCascadeString(field);
	}

	public boolean isOneToOneConstrained(FieldDetails field) {
		return associationAttributeHelper.isOneToOneConstrained(field);
	}

	// --- OneToMany ---

	public String getOneToManyTargetEntity(FieldDetails field) {
		return associationAttributeHelper.getOneToManyTargetEntity(field);
	}

	public List<String> getKeyColumnNames(FieldDetails field) {
		return associationAttributeHelper.getKeyColumnNames(field);
	}

	public String getOneToManyCascadeString(FieldDetails field) {
		return associationAttributeHelper.getOneToManyCascadeString(field);
	}

	public boolean isOneToManyEager(FieldDetails field) {
		return associationAttributeHelper.isOneToManyEager(field);
	}

	// --- ManyToMany ---

	public String getManyToManyTargetEntity(FieldDetails field) {
		return associationAttributeHelper.getManyToManyTargetEntity(field);
	}

	public boolean isManyToManyInverse(FieldDetails field) {
		return associationAttributeHelper.isManyToManyInverse(field);
	}

	public String getJoinTableName(FieldDetails field) {
		return associationAttributeHelper.getJoinTableName(field);
	}

	public boolean hasJoinTable(FieldDetails field) {
		return associationAttributeHelper.hasJoinTable(field);
	}

	public String getJoinTableSchema(FieldDetails field) {
		return associationAttributeHelper.getJoinTableSchema(field);
	}

	public String getJoinTableCatalog(FieldDetails field) {
		return associationAttributeHelper.getJoinTableCatalog(field);
	}

	public String getJoinTableJoinColumnName(FieldDetails field) {
		return associationAttributeHelper.getJoinTableJoinColumnName(field);
	}

	public List<String> getJoinTableJoinColumnNames(FieldDetails field) {
		return associationAttributeHelper.getJoinTableJoinColumnNames(field);
	}

	public String getJoinTableInverseJoinColumnName(FieldDetails field) {
		return associationAttributeHelper.getJoinTableInverseJoinColumnName(field);
	}

	public List<String> getJoinTableInverseJoinColumnNames(FieldDetails field) {
		return associationAttributeHelper.getJoinTableInverseJoinColumnNames(field);
	}

	public String getManyToManyCascadeString(FieldDetails field) {
		return associationAttributeHelper.getManyToManyCascadeString(field);
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
		return fieldAttributeHelper.getEmbeddableClassName(field);
	}

	public List<AttributeOverrideInfo> getAttributeOverrides(FieldDetails field) {
		return associationAttributeHelper.getAttributeOverrides(field);
	}

	public record AttributeOverrideInfo(String fieldName, String columnName) {}

	// --- Properties groups (<properties> element) ---

	public List<PropertiesGroupInfo> getPropertiesGroups() {
		return fieldCategorizationHelper.getPropertiesGroups();
	}

	public boolean isBasicField(FieldDetails field) {
		return fieldCategorizationHelper.isBasicField(field);
	}

	public boolean isManyToOneField(FieldDetails field) {
		return fieldCategorizationHelper.isManyToOneField(field);
	}

	public record PropertiesGroupInfo(String name, boolean unique, boolean insert,
									boolean update, boolean optimisticLock,
									List<FieldDetails> fields) {}

	// --- Meta attributes ---

	public Map<String, List<String>> getMetaAttributes() {
		return classInfoHelper.getMetaAttributes();
	}

	public List<String> getMetaAttribute(String name) {
		return classInfoHelper.getMetaAttribute(name);
	}

	public Map<String, List<String>> getFieldMetaAttributes(FieldDetails field) {
		return fieldAttributeHelper.getFieldMetaAttributes(field);
	}

	public List<String> getFieldMetaAttribute(FieldDetails field, String name) {
		return fieldAttributeHelper.getFieldMetaAttribute(field, name);
	}

	// --- Imports ---

	public List<HbmClassInfoHelper.ImportInfo> getImports() {
		return classInfoHelper.getImports();
	}

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
		return CascadeUtil.formatJpaCascade(cascadeTypes);
	}

	String toGeneratorClass(GenerationType generationType) {
		return fieldAttributeHelper.toGeneratorClass(generationType);
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
