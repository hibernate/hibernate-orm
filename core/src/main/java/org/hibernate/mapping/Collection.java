/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.mapping;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.ExecuteUpdateResultCheckStyle;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.EmptyIterator;
import org.hibernate.util.ReflectHelper;

/**
 * Mapping for a collection. Subclasses specialize to particular collection styles.
 * 
 * @author Gavin King
 */
public abstract class Collection implements Fetchable, Value, Filterable {

	public static final String DEFAULT_ELEMENT_COLUMN_NAME = "elt";
	public static final String DEFAULT_KEY_COLUMN_NAME = "id";

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
	private PersistentClass owner;
	private String referencedPropertyName;
	private String nodeName;
	private String elementNodeName;
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
	private final java.util.Map filters = new HashMap();
	private final java.util.Map manyToManyFilters = new HashMap();
	private final java.util.Set synchronizedTables = new HashSet();

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

	protected Collection(PersistentClass owner) {
		this.owner = owner;
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
				setComparator( (Comparator) ReflectHelper.classForName( comparatorClassName ).newInstance() );
			}
			catch ( Exception e ) {
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
		this.role = role==null ? null : role.intern();
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
		if ( getKey().isCascadeDeleteEnabled() && ( !isInverse() || !isOneToMany() ) ) {
			throw new MappingException(
				"only inverse one-to-many associations may use on-delete=\"cascade\": " 
				+ getRole() );
		}
		if ( !getKey().isValid( mapping ) ) {
			throw new MappingException(
				"collection foreign key mapping has wrong number of columns: "
				+ getRole()
				+ " type: "
				+ getKey().getType().getName() );
		}
		if ( !getElement().isValid( mapping ) ) {
			throw new MappingException( 
				"collection element mapping has wrong number of columns: "
				+ getRole()
				+ " type: "
				+ getElement().getType().getName() );
		}

		checkColumnDuplication();
		
		if ( elementNodeName!=null && elementNodeName.startsWith("@") ) {
			throw new MappingException("element node must not be an attribute: " + elementNodeName );
		}
		if ( elementNodeName!=null && elementNodeName.equals(".") ) {
			throw new MappingException("element node must not be the parent: " + elementNodeName );
		}
		if ( nodeName!=null && nodeName.indexOf('@')>-1 ) {
			throw new MappingException("collection node must not be an attribute: " + elementNodeName );
		}
	}

	private void checkColumnDuplication(java.util.Set distinctColumns, Iterator columns)
			throws MappingException {
		while ( columns.hasNext() ) {
			Selectable s = (Selectable) columns.next();
			if ( !s.isFormula() ) {
				Column col = (Column) s;
				if ( !distinctColumns.add( col.getName() ) ) {
					throw new MappingException( "Repeated column in mapping for collection: "
						+ getRole()
						+ " column: "
						+ col.getName() );
				}
			}
		}
	}

	private void checkColumnDuplication() throws MappingException {
		HashSet cols = new HashSet();
		checkColumnDuplication( cols, getKey().getColumnIterator() );
		if ( isIndexed() ) {
			checkColumnDuplication( cols, ( (IndexedCollection) this )
				.getIndex()
				.getColumnIterator() );
		}
		if ( isIdentified() ) {
			checkColumnDuplication( cols, ( (IdentifierCollection) this )
				.getIdentifier()
				.getColumnIterator() );
		}
		if ( !isOneToMany() ) {
			checkColumnDuplication( cols, getElement().getColumnIterator() );
		}
	}

	public Iterator getColumnIterator() {
		return EmptyIterator.INSTANCE;
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
			return TypeFactory.customCollection( typeName, typeParameters, role, referencedPropertyName, isEmbedded() );
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
		if ( !isInverse() ) createPrimaryKey();
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

	public void setCustomSQLDeleteAll(String customSQLDeleteAll, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
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

	public void addFilter(String name, String condition) {
		filters.put( name, condition );
	}

	public java.util.Map getFilterMap() {
		return filters;
	}

	public void addManyToManyFilter(String name, String condition) {
		manyToManyFilters.put( name, condition );
	}

	public java.util.Map getManyToManyFilterMap() {
		return manyToManyFilters;
	}

	public String toString() {
		return getClass().getName() + '(' + getRole() + ')';
	}

	public java.util.Set getSynchronizedTables() {
		return synchronizedTables;
	}

	public String getLoaderName() {
		return loaderName;
	}

	public void setLoaderName(String name) {
		this.loaderName = name==null ? null : name.intern();
	}

	public String getReferencedPropertyName() {
		return referencedPropertyName;
	}

	public void setReferencedPropertyName(String propertyRef) {
		this.referencedPropertyName = propertyRef==null ? null : propertyRef.intern();
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

	public boolean[] getColumnInsertability() {
		return ArrayHelper.EMPTY_BOOLEAN_ARRAY;
	}

	public boolean[] getColumnUpdateability() {
		return ArrayHelper.EMPTY_BOOLEAN_ARRAY;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getElementNodeName() {
		return elementNodeName;
	}

	public void setElementNodeName(String elementNodeName) {
		this.elementNodeName = elementNodeName;
	}

	public boolean isEmbedded() {
		return embedded;
	}

	public void setEmbedded(boolean embedded) {
		this.embedded = embedded;
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
		return orderBy!=null || manyToManyOrderBy!=null;
	}

	public void setComparatorClassName(String comparatorClassName) {
		this.comparatorClassName = comparatorClassName;		
	}
	
	public String getComparatorClassName() {
		return comparatorClassName;
	}
}