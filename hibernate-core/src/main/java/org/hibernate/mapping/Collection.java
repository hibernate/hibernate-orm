/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.collection.internal.CustomCollectionTypeSemantics;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.FilterConfiguration;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CustomCollectionType;
import org.hibernate.type.MappingContext;
import org.hibernate.type.Type;
import org.hibernate.usertype.UserCollectionType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle.expectationConstructor;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_BOOLEAN_ARRAY;
import static org.hibernate.mapping.MappingHelper.classForName;
import static org.hibernate.mapping.MappingHelper.createUserTypeBean;

/**
 * A mapping model object representing a collection. Subclasses specialize to particular kinds of collection.
 *
 * @author Gavin King
 */
public abstract sealed class Collection
		implements Fetchable, Value, Filterable, SoftDeletable
		permits Set, Bag,
				IndexedCollection, // List, Map
				IdentifierCollection { // IdentifierBag only built-in implementation

	public static final String DEFAULT_ELEMENT_COLUMN_NAME = "elt";
	public static final String DEFAULT_KEY_COLUMN_NAME = "id";

	private final MetadataBuildingContext buildingContext;
	private final PersistentClass owner;

	private KeyValue key;
	private Value element;
	private Table collectionTable;
	private String role;
	private boolean lazy;
	private boolean extraLazy;
	private boolean inverse;
	private boolean mutable = true;
	private boolean subselectLoadable;
	private String cacheConcurrencyStrategy;
	private String cacheRegionName;
	private CacheLayout queryCacheLayout;
	private String orderBy;
	private String where;
	private String manyToManyWhere;
	private String manyToManyOrderBy;
	private String referencedPropertyName;
	private String mappedByProperty;
	private boolean sorted;
	private Comparator<?> comparator;
	private String comparatorClassName;
	private boolean orphanDelete;
	private int batchSize = -1;
	private FetchMode fetchMode;
	private boolean optimisticLocked = true;

	private String typeName;
	private Properties typeParameters;
	private Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver;
	private CollectionType cachedCollectionType;
	private CollectionSemantics<?,?> cachedCollectionSemantics;

	private final List<FilterConfiguration> filters = new ArrayList<>();
	private final List<FilterConfiguration> manyToManyFilters = new ArrayList<>();
	private final java.util.Set<String> synchronizedTables = new HashSet<>();

	private String customSQLInsert;
	private boolean customInsertCallable;
	private String customSQLUpdate;
	private boolean customUpdateCallable;
	private String customSQLDelete;
	private boolean customDeleteCallable;
	private String customSQLDeleteAll;
	private boolean customDeleteAllCallable;

	private Column softDeleteColumn;
	private SoftDeleteType softDeleteStrategy;

	private String loaderName;

	private Supplier<? extends Expectation> insertExpectation;
	private Supplier<? extends Expectation> updateExpectation;
	private Supplier<? extends Expectation> deleteExpectation;
	private Supplier<? extends Expectation> deleteAllExpectation;

	/**
	 * hbm.xml binding
	 */
	protected Collection(MetadataBuildingContext buildingContext, PersistentClass owner) {
		this.buildingContext = buildingContext;
		this.owner = owner;
	}

	/**
	 * Annotation binding
	 */
	protected Collection(
			Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver,
			PersistentClass owner,
			MetadataBuildingContext buildingContext) {
		this.customTypeBeanResolver = customTypeBeanResolver;
		this.owner = owner;
		this.buildingContext = buildingContext;
	}

	protected Collection(Collection original) {
		this.buildingContext = original.buildingContext;
		this.owner = original.owner;
		this.key = original.key == null ? null : (KeyValue) original.key.copy();
		this.element = original.element == null ? null : original.element.copy();
		this.collectionTable = original.collectionTable;
		this.role = original.role;
		this.lazy = original.lazy;
		this.extraLazy = original.extraLazy;
		this.inverse = original.inverse;
		this.mutable = original.mutable;
		this.subselectLoadable = original.subselectLoadable;
		this.cacheConcurrencyStrategy = original.cacheConcurrencyStrategy;
		this.cacheRegionName = original.cacheRegionName;
		this.orderBy = original.orderBy;
		this.where = original.where;
		this.manyToManyWhere = original.manyToManyWhere;
		this.manyToManyOrderBy = original.manyToManyOrderBy;
		this.referencedPropertyName = original.referencedPropertyName;
		this.mappedByProperty = original.mappedByProperty;
		this.sorted = original.sorted;
		this.comparator = original.comparator;
		this.comparatorClassName = original.comparatorClassName;
		this.orphanDelete = original.orphanDelete;
		this.batchSize = original.batchSize;
		this.fetchMode = original.fetchMode;
		this.optimisticLocked = original.optimisticLocked;
		this.typeName = original.typeName;
		this.typeParameters = original.typeParameters == null ? null : new Properties(original.typeParameters);
		this.customTypeBeanResolver = original.customTypeBeanResolver;
		this.filters.addAll( original.filters );
		this.manyToManyFilters.addAll( original.manyToManyFilters );
		this.synchronizedTables.addAll( original.synchronizedTables );
		this.customSQLInsert = original.customSQLInsert;
		this.customInsertCallable = original.customInsertCallable;
		this.customSQLUpdate = original.customSQLUpdate;
		this.customUpdateCallable = original.customUpdateCallable;
		this.customSQLDelete = original.customSQLDelete;
		this.customDeleteCallable = original.customDeleteCallable;
		this.customSQLDeleteAll = original.customSQLDeleteAll;
		this.customDeleteAllCallable = original.customDeleteAllCallable;
		this.insertExpectation = original.insertExpectation;
		this.updateExpectation = original.updateExpectation;
		this.deleteExpectation = original.deleteExpectation;
		this.deleteAllExpectation = original.deleteAllExpectation;
		this.loaderName = original.loaderName;
	}

	@Override
	public MetadataBuildingContext getBuildingContext() {
		return buildingContext;
	}

	BootstrapContext getBootstrapContext() {
		return getBuildingContext().getBootstrapContext();
	}

	public MetadataImplementor getMetadata() {
		return getBuildingContext().getMetadataCollector();
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return getMetadata().getMetadataBuildingOptions().getServiceRegistry();
	}

	public boolean isSet() {
		return false;
	}

	public KeyValue getKey() {
		return key;
	}

	public Value getElement() {
		return element;
	}

	public boolean isIndexed() {
		return false;
	}

	public Table getCollectionTable() {
		return collectionTable;
	}

	public void setCollectionTable(Table table) {
		this.collectionTable = table;
	}

	public boolean isSorted() {
		return sorted;
	}

	public Comparator<?> getComparator() {
		if ( comparator == null && comparatorClassName != null ) {
			final var clazz = classForName( Comparator.class, comparatorClassName, getBootstrapContext() );
			try {
				comparator = clazz.getConstructor().newInstance();
			}
			catch (Exception e) {
				throw new MappingException( "Could not instantiate comparator class ["
						+ comparatorClassName + "] for collection " + getRole() );
			}
		}
		return comparator;
	}

	@Override
	public boolean isLazy() {
		return lazy;
	}

	@Override
	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public String getRole() {
		return role;
	}

	public abstract CollectionType getDefaultCollectionType();

	public boolean isPrimitiveArray() {
		return false;
	}

	public boolean isArray() {
		return false;
	}

	@Override
	public boolean hasFormula() {
		return false;
	}

	public boolean isOneToMany() {
		return element instanceof OneToMany;
	}

	public boolean isInverse() {
		return inverse;
	}

	public String getOwnerEntityName() {
		return owner.getEntityName();
	}

	public String getOrderBy() {
		return orderBy;
	}

	public void setComparator(@SuppressWarnings("rawtypes") Comparator comparator) {
		this.comparator = comparator;
	}

	public void setElement(Value element) {
		this.element = element;
	}

	public void setKey(KeyValue key) {
		this.key = key;
	}

	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public void setSorted(boolean sorted) {
		this.sorted = sorted;
	}

	public void setInverse(boolean inverse) {
		this.inverse = inverse;
	}

	public PersistentClass getOwner() {
		return owner;
	}

	public String getWhere() {
		return where;
	}

	public void setWhere(String where) {
		this.where = where;
	}

	public String getManyToManyWhere() {
		return manyToManyWhere;
	}

	public void setManyToManyWhere(String manyToManyWhere) {
		this.manyToManyWhere = manyToManyWhere;
	}

	public String getManyToManyOrdering() {
		return manyToManyOrderBy;
	}

	public void setManyToManyOrdering(String orderFragment) {
		this.manyToManyOrderBy = orderFragment;
	}

	public boolean isIdentified() {
		return false;
	}

	public boolean hasOrphanDelete() {
		return orphanDelete;
	}

	public void setOrphanDelete(boolean orphanDelete) {
		this.orphanDelete = orphanDelete;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	@Override
	public FetchMode getFetchMode() {
		return fetchMode;
	}

	@Override
	public void setFetchMode(FetchMode fetchMode) {
		this.fetchMode = fetchMode;
	}

	public void validate(MappingContext mappingContext) throws MappingException {
		assert getKey() != null : "Collection key not bound : " + getRole();
		assert getElement() != null : "Collection element not bound : " + getRole();

		if ( !getKey().isValid( mappingContext ) ) {
			throw new MappingException(
					"collection foreign key mappingContext has wrong number of columns: "
							+ getRole()
							+ " type: "
							+ getKey().getType().getName()
			);
		}
		if ( !getElement().isValid( mappingContext ) ) {
			throw new MappingException(
					"collection element mappingContext has wrong number of columns: "
							+ getRole()
							+ " type: "
							+ getElement().getType().getName()
			);
		}

		checkColumnDuplication();
	}

	private void checkColumnDuplication() throws MappingException {
		final String owner = "collection '" + getReferencedPropertyName() + "'";
		final HashSet<String> cols = new HashSet<>();
		getKey().checkColumnDuplication( cols, owner );
		if ( isIndexed() ) {
			( (IndexedCollection) this ).getIndex().checkColumnDuplication( cols, owner );
		}
		if ( isIdentified() ) {
			( (IdentifierCollection) this ).getIdentifier().checkColumnDuplication( cols, owner );
		}
		if ( !isOneToMany() ) {
			getElement().checkColumnDuplication( cols, owner );
		}
	}

	@Override
	public List<Selectable> getSelectables() {
		return emptyList();
	}

	@Override
	public List<Column> getColumns() {
		return emptyList();
	}

	@Override
	public int getColumnSpan() {
		return 0;
	}

	@Override
	public Type getType() throws MappingException {
		return getCollectionType();
	}

	public CollectionSemantics<?,?> getCollectionSemantics() {
		if ( cachedCollectionSemantics == null ) {
			cachedCollectionSemantics = resolveCollectionSemantics();
		}
		return cachedCollectionSemantics;
	}

	private CollectionSemantics<?, ?> resolveCollectionSemantics() {
		final CollectionType collectionType;
		if ( cachedCollectionType == null ) {
			collectionType = resolveCollectionType();
			cachedCollectionType = collectionType;
		}
		else {
			collectionType = cachedCollectionType;
		}

		return new CustomCollectionTypeSemantics<>( collectionType );
	}

	private CollectionType resolveCollectionType() {
		if ( cachedCollectionType != null ) {
			return cachedCollectionType;
		}
		else if ( customTypeBeanResolver != null ) {
			return new CustomCollectionType( customTypeBeanResolver.get(), role, referencedPropertyName );
		}
		else if ( typeName == null ) {
			return getDefaultCollectionType();
		}
		else {
			return new CustomCollectionType( userTypeBean(), role, referencedPropertyName );
		}
	}

	private ManagedBean<? extends UserCollectionType> userTypeBean() {
		final var bootstrapContext = getBootstrapContext();
		return createUserTypeBean(
				role,
				classForName( UserCollectionType.class, typeName, bootstrapContext ),
				PropertiesHelper.map( typeParameters ),
				bootstrapContext,
				getMetadata().getMetadataBuildingOptions().isAllowExtensionsInCdi()
		);
	}

	public CollectionType getCollectionType() {
		if ( cachedCollectionType == null ) {
			cachedCollectionType = resolveCollectionType();
		}
		return cachedCollectionType;
	}

	@Override
	public boolean isNullable() {
		return true;
	}

	@Override
	public boolean isAlternateUniqueKey() {
		return false;
	}

	@Override
	public Table getTable() {
		return owner.getTable();
	}

	@Override
	public void createForeignKey() {
	}

	@Override
	public void createUniqueKey(MetadataBuildingContext context) {
	}

	@Override
	public boolean isSimpleValue() {
		return false;
	}

	@Override
	public boolean isValid(MappingContext mappingContext) {
		return true;
	}

	@Override
	public boolean isSame(Value other) {
		return this == other
			|| other instanceof Collection collection && isSame( collection );
	}

	protected static boolean isSame(Value v1, Value v2) {
		return v1 == v2 || v1 != null && v2 != null && v1.isSame( v2 );
	}

	public boolean isSame(Collection other) {
		return this == other || isSame( key, other.key )
			&& isSame( element, other.element )
			&& Objects.equals( collectionTable, other.collectionTable )
			&& Objects.equals( where, other.where )
			&& Objects.equals( manyToManyWhere, other.manyToManyWhere )
			&& Objects.equals( referencedPropertyName, other.referencedPropertyName )
			&& Objects.equals( mappedByProperty, other.mappedByProperty )
			&& Objects.equals( typeName, other.typeName )
			&& Objects.equals( typeParameters, other.typeParameters );
	}

	private void createForeignKeys() throws MappingException {
		// if ( !isInverse() ) { // for inverse collections, let the "other end" handle it
		final String entityName = getOwner().getEntityName();
		if ( referencedPropertyName == null ) {
			getElement().createForeignKey();
			key.createForeignKeyOfEntity( entityName );
		}
		else {
			final var property = owner.getProperty( referencedPropertyName );
			assert property != null;
			key.createForeignKeyOfEntity( entityName,
					property.getValue().getConstraintColumns() );
		}
		// }
	}

	abstract void createPrimaryKey();

	public void createAllKeys() throws MappingException {
		createForeignKeys();
		if ( !isInverse() ) {
			createPrimaryKey();
		}
	}

	public String getCacheConcurrencyStrategy() {
		return cacheConcurrencyStrategy;
	}

	public void setCacheConcurrencyStrategy(String cacheConcurrencyStrategy) {
		this.cacheConcurrencyStrategy = cacheConcurrencyStrategy;
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) {
	}

	public String getCacheRegionName() {
		return cacheRegionName == null ? role : cacheRegionName;
	}

	public void setCacheRegionName(String cacheRegionName) {
		this.cacheRegionName = StringHelper.nullIfEmpty( cacheRegionName );
	}

	public CacheLayout getQueryCacheLayout() {
		return queryCacheLayout;
	}

	public void setQueryCacheLayout(CacheLayout queryCacheLayout) {
		this.queryCacheLayout = queryCacheLayout;
	}

	public void setCustomSQLInsert(String customSQLInsert, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLInsert = customSQLInsert;
		this.customInsertCallable = callable;
		this.insertExpectation = expectationConstructor( checkStyle );
	}

	public String getCustomSQLInsert() {
		return customSQLInsert;
	}

	public boolean isCustomInsertCallable() {
		return customInsertCallable;
	}

	public void setCustomSQLUpdate(String customSQLUpdate, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLUpdate = customSQLUpdate;
		this.customUpdateCallable = callable;
		this.updateExpectation = expectationConstructor( checkStyle );
	}

	public String getCustomSQLUpdate() {
		return customSQLUpdate;
	}

	public boolean isCustomUpdateCallable() {
		return customUpdateCallable;
	}

	public void setCustomSQLDelete(String customSQLDelete, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLDelete = customSQLDelete;
		this.customDeleteCallable = callable;
		this.deleteExpectation = expectationConstructor( checkStyle );
	}

	public String getCustomSQLDelete() {
		return customSQLDelete;
	}

	public boolean isCustomDeleteCallable() {
		return customDeleteCallable;
	}

	public void setCustomSQLDeleteAll(
			String customSQLDeleteAll,
			boolean callable,
			ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLDeleteAll = customSQLDeleteAll;
		this.customDeleteAllCallable = callable;
		this.deleteAllExpectation = expectationConstructor( checkStyle );
	}

	public String getCustomSQLDeleteAll() {
		return customSQLDeleteAll;
	}

	public boolean isCustomDeleteAllCallable() {
		return customDeleteAllCallable;
	}

	@Override
	public void addFilter(
			String name,
			String condition,
			boolean autoAliasInjection,
			java.util.Map<String, String> aliasTableMap,
			java.util.Map<String, String> aliasEntityMap) {
		filters.add( new FilterConfiguration(
				name,
				condition,
				autoAliasInjection,
				aliasTableMap,
				aliasEntityMap,
				null
		) );
	}

	@Override
	public List<FilterConfiguration> getFilters() {
		return filters;
	}

	public void addManyToManyFilter(
			String name,
			String condition,
			boolean autoAliasInjection,
			java.util.Map<String, String> aliasTableMap,
			java.util.Map<String, String> aliasEntityMap) {
		manyToManyFilters.add( new FilterConfiguration(
				name,
				condition,
				autoAliasInjection,
				aliasTableMap,
				aliasEntityMap,
				null
		) );
	}

	public List<FilterConfiguration> getManyToManyFilters() {
		return manyToManyFilters;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '(' + getRole() + ')';
	}

	public java.util.Set<String> getSynchronizedTables() {
		return synchronizedTables;
	}

	public void addSynchronizedTable(String table) {
		synchronizedTables.add( table );
	}

	public String getLoaderName() {
		return loaderName;
	}

	public void setLoaderName(String name) {
		this.loaderName = name;
	}

	public String getReferencedPropertyName() {
		return referencedPropertyName;
	}

	public void setReferencedPropertyName(String propertyRef) {
		this.referencedPropertyName = propertyRef;
	}

	public boolean isOptimisticLocked() {
		return optimisticLocked;
	}

	public void setOptimisticLocked(boolean optimisticLocked) {
		this.optimisticLocked = optimisticLocked;
	}

	public boolean isMap() {
		return false;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	@Deprecated(since = "7.0", forRemoval = true)
	public Properties getTypeParameters() {
		return typeParameters;
	}

	@Deprecated(since = "7.0", forRemoval = true)
	public void setTypeParameters(Properties parameterMap) {
		this.typeParameters = parameterMap;
	}

	public void setTypeParameters(java.util.Map<String,String> typeParameters) {
		this.typeParameters = new Properties();
		this.typeParameters.putAll( typeParameters );
	}

	@Override
	public boolean[] getColumnInsertability() {
		return EMPTY_BOOLEAN_ARRAY;
	}

	@Override
	public boolean hasAnyInsertableColumns() {
		return true;
	}

	@Override
	public boolean[] getColumnUpdateability() {
		return EMPTY_BOOLEAN_ARRAY;
	}

	@Override
	public boolean hasAnyUpdatableColumns() {
		return false;
	}

	public boolean isSubselectLoadable() {
		return subselectLoadable;
	}

	public void setSubselectLoadable(boolean subqueryLoadable) {
		this.subselectLoadable = subqueryLoadable;
	}

	public boolean isMutable() {
		return mutable;
	}

	public void setMutable(boolean mutable) {
		this.mutable = mutable;
	}

	public boolean isExtraLazy() {
		return extraLazy;
	}

	public void setExtraLazy(boolean extraLazy) {
		this.extraLazy = extraLazy;
	}

	public boolean hasOrder() {
		return orderBy != null || manyToManyOrderBy != null;
	}

	public void setComparatorClassName(String comparatorClassName) {
		this.comparatorClassName = comparatorClassName;
	}

	public String getComparatorClassName() {
		return comparatorClassName;
	}

	public String getMappedByProperty() {
		return mappedByProperty;
	}

	public void setMappedByProperty(String mappedByProperty) {
		this.mappedByProperty = mappedByProperty;
	}

	@Override
	public boolean isColumnInsertable(int index) {
		return false;
	}

	@Override
	public boolean isColumnUpdateable(int index) {
		return false;
	}

	@Override
	public void enableSoftDelete(Column indicatorColumn, SoftDeleteType strategy) {
		this.softDeleteColumn = indicatorColumn;
		this.softDeleteStrategy = strategy;
	}

	@Override
	public SoftDeleteType getSoftDeleteStrategy() {
		return softDeleteStrategy;
	}

	@Override
	public Column getSoftDeleteColumn() {
		return softDeleteColumn;
	}

	public Supplier<? extends Expectation> getInsertExpectation() {
		return insertExpectation;
	}

	public void setInsertExpectation(Supplier<? extends Expectation> insertExpectation) {
		this.insertExpectation = insertExpectation;
	}

	public Supplier<? extends Expectation> getUpdateExpectation() {
		return updateExpectation;
	}

	public void setUpdateExpectation(Supplier<? extends Expectation> updateExpectation) {
		this.updateExpectation = updateExpectation;
	}

	public Supplier<? extends Expectation> getDeleteExpectation() {
		return deleteExpectation;
	}

	public void setDeleteExpectation(Supplier<? extends Expectation> deleteExpectation) {
		this.deleteExpectation = deleteExpectation;
	}

	public Supplier<? extends Expectation> getDeleteAllExpectation() {
		return deleteAllExpectation;
	}

	public void setDeleteAllExpectation(Supplier<? extends Expectation> deleteAllExpectation) {
		this.deleteAllExpectation = deleteAllExpectation;
	}

	@Override
	public boolean isPartitionKey() {
		return false;
	}
}
