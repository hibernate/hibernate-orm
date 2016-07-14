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
import java.util.Iterator;
import java.util.Properties;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.FilterConfiguration;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * Mapping for a collection. Subclasses specialize to particular collection styles.
 *
 * @author Gavin King
 */
public abstract class Collection implements Fetchable, Value, Filterable {

	public static final String DEFAULT_ELEMENT_COLUMN_NAME = "elt";
	public static final String DEFAULT_KEY_COLUMN_NAME = "id";

	private final MetadataImplementor metadata;
	private PersistentClass owner;

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
	private String orderBy;
	private String where;
	private String manyToManyWhere;
	private String manyToManyOrderBy;
	private String referencedPropertyName;
	private String mappedByProperty;
	private boolean sorted;
	private Comparator comparator;
	private String comparatorClassName;
	private boolean orphanDelete;
	private int batchSize = -1;
	private FetchMode fetchMode;
	private boolean embedded = true;
	private boolean optimisticLocked = true;
	private Class collectionPersisterClass;
	private String typeName;
	private Properties typeParameters;
	private final java.util.List filters = new ArrayList();
	private final java.util.List manyToManyFilters = new ArrayList();
	private final java.util.Set<String> synchronizedTables = new HashSet<String>();

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

	private String loaderName;

	protected Collection(MetadataImplementor metadata, PersistentClass owner) {
		this.metadata = metadata;
		this.owner = owner;
	}

	public MetadataImplementor getMetadata() {
		return metadata;
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

	public Comparator getComparator() {
		if ( comparator == null && comparatorClassName != null ) {
			try {
				final ClassLoaderService classLoaderService = getMetadata().getMetadataBuildingOptions()
						.getServiceRegistry()
						.getService( ClassLoaderService.class );
				setComparator( (Comparator) classLoaderService.classForName( comparatorClassName ).newInstance() );
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

	public void setComparator(Comparator comparator) {
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

	/**
	 * @param owner The owner
	 *
	 * @deprecated Inject the owner into constructor.
	 */
	@Deprecated
	public void setOwner(PersistentClass owner) {
		this.owner = owner;
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

	public void setBatchSize(int i) {
		batchSize = i;
	}

	public FetchMode getFetchMode() {
		return fetchMode;
	}

	public void setFetchMode(FetchMode fetchMode) {
		this.fetchMode = fetchMode;
	}

	public void setCollectionPersisterClass(Class persister) {
		this.collectionPersisterClass = persister;
	}

	public Class getCollectionPersisterClass() {
		return collectionPersisterClass;
	}

	public void validate(Mapping mapping) throws MappingException {
		assert getKey() != null : "Collection key not bound : " + getRole();
		assert getElement() != null : "Collection element not bound : " + getRole();

		if ( getKey().isCascadeDeleteEnabled() && ( !isInverse() || !isOneToMany() ) ) {
			throw new MappingException(
					"only inverse one-to-many associations may use on-delete=\"cascade\": "
							+ getRole()
			);
		}
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

	private void checkColumnDuplication(java.util.Set distinctColumns, Value value)
			throws MappingException {
		final boolean[] insertability = value.getColumnInsertability();
		final boolean[] updatability = value.getColumnUpdateability();
		final Iterator<Selectable> iterator = value.getColumnIterator();
		int i = 0;
		while ( iterator.hasNext() ) {
			Selectable s = iterator.next();
			// exclude formulas and coluns that are not insertable or updatable
			// since these values can be be repeated (HHH-5393)
			if ( !s.isFormula() && ( insertability[i] || updatability[i] ) ) {
				Column col = (Column) s;
				if ( !distinctColumns.add( col.getName() ) ) {
					throw new MappingException(
							"Repeated column in mapping for collection: "
									+ getRole()
									+ " column: "
									+ col.getName()
					);
				}
			}
			i++;
		}
	}

	private void checkColumnDuplication() throws MappingException {
		HashSet cols = new HashSet();
		checkColumnDuplication( cols, getKey() );
		if ( isIndexed() ) {
			checkColumnDuplication(
					cols,
					( (IndexedCollection) this ).getIndex()
			);
		}
		if ( isIdentified() ) {
			checkColumnDuplication(
					cols,
					( (IdentifierCollection) this ).getIdentifier()
			);
		}
		if ( !isOneToMany() ) {
			checkColumnDuplication( cols, getElement() );
		}
	}

	public Iterator<Selectable> getColumnIterator() {
		return Collections.<Selectable>emptyList().iterator();
	}

	public int getColumnSpan() {
		return 0;
	}

	public Type getType() throws MappingException {
		return getCollectionType();
	}

	public CollectionType getCollectionType() {
		if ( typeName == null ) {
			return getDefaultCollectionType();
		}
		else {
			return metadata.getTypeResolver()
					.getTypeFactory()
					.customCollection( typeName, typeParameters, role, referencedPropertyName );
		}
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

	public boolean isSimpleValue() {
		return false;
	}

	public boolean isValid(Mapping mapping) throws MappingException {
		return true;
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
		this.cacheRegionName = cacheRegionName;
	}


	public void setCustomSQLInsert(String customSQLInsert, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLInsert = customSQLInsert;
		this.customInsertCallable = callable;
		this.insertCheckStyle = checkStyle;
	}

	public String getCustomSQLInsert() {
		return customSQLInsert;
	}

	public boolean isCustomInsertCallable() {
		return customInsertCallable;
	}

	public ExecuteUpdateResultCheckStyle getCustomSQLInsertCheckStyle() {
		return insertCheckStyle;
	}

	public void setCustomSQLUpdate(String customSQLUpdate, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLUpdate = customSQLUpdate;
		this.customUpdateCallable = callable;
		this.updateCheckStyle = checkStyle;
	}

	public String getCustomSQLUpdate() {
		return customSQLUpdate;
	}

	public boolean isCustomUpdateCallable() {
		return customUpdateCallable;
	}

	public ExecuteUpdateResultCheckStyle getCustomSQLUpdateCheckStyle() {
		return updateCheckStyle;
	}

	public void setCustomSQLDelete(String customSQLDelete, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLDelete = customSQLDelete;
		this.customDeleteCallable = callable;
		this.deleteCheckStyle = checkStyle;
	}

	public String getCustomSQLDelete() {
		return customSQLDelete;
	}

	public boolean isCustomDeleteCallable() {
		return customDeleteCallable;
	}

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

	public java.util.List getFilters() {
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

	public java.util.List getManyToManyFilters() {
		return manyToManyFilters;
	}

	@Override
	public String toString() {
		return getClass().getName() + '(' + getRole() + ')';
	}

	public java.util.Set<String> getSynchronizedTables() {
		return synchronizedTables;
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

	public void setTypeParameters(java.util.Map parameterMap) {
		if ( parameterMap instanceof Properties ) {
			this.typeParameters = (Properties) parameterMap;
		}
		else {
			this.typeParameters = new Properties();
			typeParameters.putAll( parameterMap );
		}
	}

	public boolean[] getColumnInsertability() {
		return ArrayHelper.EMPTY_BOOLEAN_ARRAY;
	}

	public boolean[] getColumnUpdateability() {
		return ArrayHelper.EMPTY_BOOLEAN_ARRAY;
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
}
