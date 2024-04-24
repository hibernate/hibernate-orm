/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.collection.internal.CustomCollectionTypeSemantics;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.FilterConfiguration;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CustomCollectionType;
import org.hibernate.type.Type;
import org.hibernate.usertype.UserCollectionType;

import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_BOOLEAN_ARRAY;
import static org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle.expectationConstructor;

/**
 * A mapping model object representing a collection. Subclasses specialize to particular kinds of collection.
 *
 * @author Gavin King
 */
public abstract class Collection implements Fetchable, Value, Filterable, SoftDeletable {

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

	private Class<? extends CollectionPersister> collectionPersisterClass;

	private final List<FilterConfiguration> filters = new ArrayList<>();
	private final List<FilterConfiguration> manyToManyFilters = new ArrayList<>();
	private final java.util.Set<String> synchronizedTables = new HashSet<>();

	private String customSQLInsert;
	private boolean customInsertCallable;
	private ExecuteUpdateResultCheckStyle insertCheckStyle;
	private String customSQLUpdate;
	private boolean customUpdateCallable;
	private ExecuteUpdateResultCheckStyle updateCheckStyle;
	private String customSQLDelete;
	private boolean customDeleteCallable;
	private ExecuteUpdateResultCheckStyle deleteCheckStyle;
	private String customSQLDeleteAll;
	private boolean customDeleteAllCallable;
	private ExecuteUpdateResultCheckStyle deleteAllCheckStyle;

	private Column softDeleteColumn;

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
		this.collectionPersisterClass = original.collectionPersisterClass;
		this.filters.addAll( original.filters );
		this.manyToManyFilters.addAll( original.manyToManyFilters );
		this.synchronizedTables.addAll( original.synchronizedTables );
		this.customSQLInsert = original.customSQLInsert;
		this.customInsertCallable = original.customInsertCallable;
		this.insertCheckStyle = original.insertCheckStyle;
		this.customSQLUpdate = original.customSQLUpdate;
		this.customUpdateCallable = original.customUpdateCallable;
		this.updateCheckStyle = original.updateCheckStyle;
		this.customSQLDelete = original.customSQLDelete;
		this.customDeleteCallable = original.customDeleteCallable;
		this.deleteCheckStyle = original.deleteCheckStyle;
		this.customSQLDeleteAll = original.customSQLDeleteAll;
		this.customDeleteAllCallable = original.customDeleteAllCallable;
		this.deleteAllCheckStyle = original.deleteAllCheckStyle;
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

	public MetadataImplementor getMetadata() {
		return getBuildingContext().getMetadataCollector();
	}

//	public TypeConfiguration getTypeConfiguration() {
//		return getBuildingContext().getBootstrapContext().getTypeConfiguration();
//	}

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
			try {
				final ClassLoaderService classLoaderService = getMetadata().getMetadataBuildingOptions()
						.getServiceRegistry()
						.requireService( ClassLoaderService.class );
				setComparator( (Comparator<?>) classLoaderService.classForName( comparatorClassName ).getConstructor().newInstance() );
			}
			catch (Exception e) {
				throw new MappingException(
						"Could not instantiate comparator class [" + comparatorClassName
								+ "] for collection " + getRole()
				);
			}
		}
		return comparator;
	}

	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public String getRole() {
		return role;
	}

	public abstract CollectionType getDefaultCollectionType() throws MappingException;

	public boolean isPrimitiveArray() {
		return false;
	}

	public boolean isArray() {
		return false;
	}

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

	public FetchMode getFetchMode() {
		return fetchMode;
	}

	public void setFetchMode(FetchMode fetchMode) {
		this.fetchMode = fetchMode;
	}

	public void setCollectionPersisterClass(Class<? extends CollectionPersister> persister) {
		this.collectionPersisterClass = persister;
	}

	public Class<? extends CollectionPersister> getCollectionPersisterClass() {
		return collectionPersisterClass;
	}

	public void validate(Mapping mapping) throws MappingException {
		assert getKey() != null : "Collection key not bound : " + getRole();
		assert getElement() != null : "Collection element not bound : " + getRole();

		if ( !getKey().isValid( mapping ) ) {
			throw new MappingException(
					"collection foreign key mapping has wrong number of columns: "
							+ getRole()
							+ " type: "
							+ getKey().getType().getName()
			);
		}
		if ( !getElement().isValid( mapping ) ) {
			throw new MappingException(
					"collection element mapping has wrong number of columns: "
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
		return Collections.emptyList();
	}

	@Override
	public List<Column> getColumns() {
		return Collections.emptyList();
	}

	public int getColumnSpan() {
		return 0;
	}

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
		final CollectionType collectionType;
		if ( cachedCollectionType != null ) {
			collectionType = cachedCollectionType;
		}
		else if ( customTypeBeanResolver != null ) {
			collectionType = new CustomCollectionType(
					customTypeBeanResolver.get(),
					role,
					referencedPropertyName
			);
		}
		else if ( typeName == null ) {
			collectionType = getDefaultCollectionType();
		}
		else {
			collectionType = MappingHelper.customCollection(
					typeName,
					typeParameters,
					role,
					referencedPropertyName,
					getMetadata()
			);
		}
		return collectionType;
	}

	public CollectionType getCollectionType() {
		if ( cachedCollectionType == null ) {
			cachedCollectionType = resolveCollectionType();
		}
		return cachedCollectionType;
	}

	public boolean isNullable() {
		return true;
	}

	public boolean isAlternateUniqueKey() {
		return false;
	}

	public Table getTable() {
		return owner.getTable();
	}

	public void createForeignKey() {
	}

	@Override
	public void createUniqueKey(MetadataBuildingContext context) {
	}

	public boolean isSimpleValue() {
		return false;
	}

	public boolean isValid(Mapping mapping) {
		return true;
	}

	@Override
	public boolean isSame(Value other) {
		return this == other
			|| other instanceof Collection && isSame( (Collection) other );
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
		if ( referencedPropertyName == null ) {
			getElement().createForeignKey();
			key.createForeignKeyOfEntity( getOwner().getEntityName() );
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
		this.insertCheckStyle = checkStyle;
		this.insertExpectation = expectationConstructor( checkStyle );
	}

	public String getCustomSQLInsert() {
		return customSQLInsert;
	}

	public boolean isCustomInsertCallable() {
		return customInsertCallable;
	}

	/**
	 * @deprecated use {@link #getInsertExpectation()}
	 */
	@Deprecated(since = "6.5", forRemoval = true)
	public ExecuteUpdateResultCheckStyle getCustomSQLInsertCheckStyle() {
		return insertCheckStyle;
	}

	public void setCustomSQLUpdate(String customSQLUpdate, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLUpdate = customSQLUpdate;
		this.customUpdateCallable = callable;
		this.updateCheckStyle = checkStyle;
		this.updateExpectation = expectationConstructor( checkStyle );
	}

	public String getCustomSQLUpdate() {
		return customSQLUpdate;
	}

	public boolean isCustomUpdateCallable() {
		return customUpdateCallable;
	}

	/**
	 * @deprecated use {@link #getUpdateExpectation()}
	 */
	@Deprecated(since = "6.5", forRemoval = true)
	public ExecuteUpdateResultCheckStyle getCustomSQLUpdateCheckStyle() {
		return updateCheckStyle;
	}

	public void setCustomSQLDelete(String customSQLDelete, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLDelete = customSQLDelete;
		this.customDeleteCallable = callable;
		this.deleteCheckStyle = checkStyle;
		this.deleteExpectation = expectationConstructor( checkStyle );
	}

	public String getCustomSQLDelete() {
		return customSQLDelete;
	}

	public boolean isCustomDeleteCallable() {
		return customDeleteCallable;
	}

	/**
	 * @deprecated use {@link #getDeleteExpectation()}
	 */
	@Deprecated(since = "6.5", forRemoval = true)
	public ExecuteUpdateResultCheckStyle getCustomSQLDeleteCheckStyle() {
		return deleteCheckStyle;
	}

	public void setCustomSQLDeleteAll(
			String customSQLDeleteAll,
			boolean callable,
			ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLDeleteAll = customSQLDeleteAll;
		this.customDeleteAllCallable = callable;
		this.deleteAllCheckStyle = checkStyle;
		this.deleteAllExpectation = expectationConstructor( checkStyle );
	}

	public String getCustomSQLDeleteAll() {
		return customSQLDeleteAll;
	}

	public boolean isCustomDeleteAllCallable() {
		return customDeleteAllCallable;
	}

	public ExecuteUpdateResultCheckStyle getCustomSQLDeleteAllCheckStyle() {
		return deleteAllCheckStyle;
	}

	public void addFilter(
			String name,
			String condition,
			boolean autoAliasInjection,
			java.util.Map<String, String> aliasTableMap,
			java.util.Map<String, String> aliasEntityMap) {
		filters.add(
				new FilterConfiguration(
						name,
						condition,
						autoAliasInjection,
						aliasTableMap,
						aliasEntityMap,
						null
				)
		);
	}

	public List<FilterConfiguration> getFilters() {
		return filters;
	}

	public void addManyToManyFilter(
			String name,
			String condition,
			boolean autoAliasInjection,
			java.util.Map<String, String> aliasTableMap,
			java.util.Map<String, String> aliasEntityMap) {
		manyToManyFilters.add(
				new FilterConfiguration(
						name,
						condition,
						autoAliasInjection,
						aliasTableMap,
						aliasEntityMap,
						null
				)
		);
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

	public Properties getTypeParameters() {
		return typeParameters;
	}

	public void setTypeParameters(Properties parameterMap) {
		this.typeParameters = parameterMap;
	}

	@SuppressWarnings("rawtypes")
	public void setTypeParameters(java.util.Map typeParameters) {
		if ( typeParameters instanceof Properties ) {
			this.typeParameters = (Properties) typeParameters;
		}
		else {
			this.typeParameters = new Properties();
			this.typeParameters.putAll( typeParameters );
		}
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
	public void enableSoftDelete(Column indicatorColumn) {
		this.softDeleteColumn = indicatorColumn;
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
}
