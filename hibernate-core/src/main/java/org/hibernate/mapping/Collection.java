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

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.boot.model.domain.NotYetResolvedException;
import org.hibernate.boot.model.relational.ForeignKeyExporter;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedForeignKey;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.FilterConfiguration;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.java.internal.CollectionJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Mapping for a collection. Subclasses specialize to particular collection styles.
 *
 * @author Gavin King
 */
public abstract class Collection implements Fetchable, Value, ForeignKeyExporter, Filterable {

	public static final String DEFAULT_ELEMENT_COLUMN_NAME = "elt";
	public static final String DEFAULT_KEY_COLUMN_NAME = "id";

	private final MetadataBuildingContext buildingContext;
	private PersistentClass owner;

	private KeyValue key;
	private Value element;
	private MappedTable collectionTable;
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

	private String loaderName;
	private MappedForeignKey foreignKey;

	protected Collection(MetadataBuildingContext buildingContext, PersistentClass owner) {
		this.buildingContext = buildingContext;
		this.owner = owner;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return getMetadataBuildingContext()
				.getBuildingOptions()
				.getServiceRegistry();
	}

	public boolean isSet() {
		return false;
	}

	public KeyValue getKey() {
		return key;
	}

	public Value<?> getElement() {
		return element;
	}

	public boolean isIndexed() {
		return false;
	}

	@Override
	public MappedForeignKey getForeignKey() {
		return foreignKey;
	}

	public void setForeignKey(MappedForeignKey foreignKey) {
		this.foreignKey = foreignKey;
	}

	/**
	 * @deprecated since 6.0, use {@link #getMappedTable()}.
	 */
	@Deprecated
	public Table getCollectionTable() {
		return (Table) collectionTable;
	}

	public void setCollectionTable(MappedTable table) {
		this.collectionTable = table;
	}

	@Override
	public MappedTable getMappedTable() {
		return collectionTable;
	}

	@Override
	public List<MappedColumn> getMappedColumns() {
		return Collections.emptyList();
	}

	public boolean isSorted() {
		return sorted;
	}

	public Comparator getComparator() {
		if ( comparator == null && comparatorClassName != null ) {
			try {
				final ClassLoaderService classLoaderService = getMetadataBuildingContext()
						.getBuildingOptions()
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

	@Override
	public MetadataBuildingContext getMetadataBuildingContext() {
		return buildingContext;
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

	public void validate() throws MappingException {
		assert getKey() != null : "Collection key not bound : " + getRole();
		assert getElement() != null : "Collection element not bound : " + getRole();

		if ( !getKey().isValid() ) {
			throw new MappingException(
					"collection foreign key mapping has wrong number of columns: "
							+ getRole()
							+ " type: "
							+ getKey().getJavaTypeMapping().getTypeName()
			);
		}
		if ( !getElement().isValid() ) {
			throw new MappingException(
					"collection element mapping has wrong number of columns: "
							+ getRole()
							+ " type: "
							+ getElement().getJavaTypeMapping().getTypeName()
			);
		}

		checkColumnDuplication();
	}

	private void checkColumnDuplication(java.util.Set distinctColumns, Value value)
			throws MappingException {
		final boolean[] insertability = value.getColumnInsertability();
		final boolean[] updatability = value.getColumnUpdateability();
		List<MappedColumn> mappedColumns = value.getMappedColumns();
		for ( int i = 0; i < mappedColumns.size(); i++ ) {
			MappedColumn s = mappedColumns.get( i );
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

	public int getColumnSpan() {
		return 0;
	}

//	public Type getType() throws MappingException {
//		return getCollectionType();
//	}
//
//	public CollectionType getCollectionType() {
//		if ( typeName == null ) {
//			return getDefaultCollectionType();
//		}
//		else {
//			return metadata.getTypeResolver()
//					.getTypeFactory()
//					.customCollection( typeName, typeParameters, role, referencedPropertyName );
//		}
//	}

	public boolean isNullable() {
		return true;
	}

	public boolean isAlternateUniqueKey() {
		return false;
	}

	public Table getTable() {
		return owner.getTable();
	}

	public boolean isSimpleValue() {
		return false;
	}

	@Override
	public boolean isValid() throws MappingException {
		return true;
	}

	@Override
	public boolean isSame(Value other) {
		return this == other || other instanceof Collection && isSame( (Collection) other );
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
			foreignKey = key.createForeignKeyOfEntity( getOwner().getEntityName() );
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

	public abstract <C> CollectionSemantics<C> getCollectionSemantics();

	protected static class CollectionJavaTypeMapping implements JavaTypeMapping {
		private final TypeConfiguration typeConfiguration;
		private final Class javaClass;

		private CollectionJavaDescriptor resolvedDescriptor;

		public CollectionJavaTypeMapping(TypeConfiguration typeConfiguration, Class javaClass) {
			this.typeConfiguration = typeConfiguration;
			this.javaClass = javaClass;
		}

		@Override
		public String getTypeName() {
			return javaClass.getTypeName();
		}

		@Override
		public CollectionJavaDescriptor getJavaTypeDescriptor() throws NotYetResolvedException {
			if ( resolvedDescriptor == null ) {
				resolvedDescriptor = (CollectionJavaDescriptor) typeConfiguration.getJavaTypeDescriptorRegistry()
						.getDescriptor( javaClass );
			}
			return resolvedDescriptor;
		}
	}
}
