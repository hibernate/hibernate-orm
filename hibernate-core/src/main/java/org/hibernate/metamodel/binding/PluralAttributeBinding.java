/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.metamodel.binding;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import org.hibernate.FetchMode;
import org.hibernate.metamodel.relational.Table;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public abstract class PluralAttributeBinding extends AbstractAttributeBinding implements AttributeBinding {
	private Table collectionTable;

	private CollectionKey collectionKey;
	private CollectionElement collectionElement;

//	private String role;
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
	private final java.util.Map filters = new HashMap();
	private final java.util.Map manyToManyFilters = new HashMap();
	private final java.util.Set synchronizedTables = new HashSet();

	private CustomSQL customSQLInsert;
	private CustomSQL customSQLUpdate;
	private CustomSQL customSQLDelete;
	private CustomSQL customSQLDeleteAll;

	private String loaderName;

	protected PluralAttributeBinding(EntityBinding entityBinding) {
		super( entityBinding );
	}

	@Override
	public boolean isSimpleValue() {
		return false;
	}

	public Table getCollectionTable() {
		return collectionTable;
	}

	public void setCollectionTable(Table collectionTable) {
		this.collectionTable = collectionTable;
	}

	public CollectionKey getCollectionKey() {
		return collectionKey;
	}

	public void setCollectionKey(CollectionKey collectionKey) {
		this.collectionKey = collectionKey;
	}

	public CollectionElement getCollectionElement() {
		return collectionElement;
	}

	public void setCollectionElement(CollectionElement collectionElement) {
		this.collectionElement = collectionElement;
	}

	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public boolean isExtraLazy() {
		return extraLazy;
	}

	public void setExtraLazy(boolean extraLazy) {
		this.extraLazy = extraLazy;
	}

	public boolean isInverse() {
		return inverse;
	}

	public void setInverse(boolean inverse) {
		this.inverse = inverse;
	}

	public boolean isMutable() {
		return mutable;
	}

	public void setMutable(boolean mutable) {
		this.mutable = mutable;
	}

	public boolean isSubselectLoadable() {
		return subselectLoadable;
	}

	public void setSubselectLoadable(boolean subselectLoadable) {
		this.subselectLoadable = subselectLoadable;
	}

	public String getCacheConcurrencyStrategy() {
		return cacheConcurrencyStrategy;
	}

	public void setCacheConcurrencyStrategy(String cacheConcurrencyStrategy) {
		this.cacheConcurrencyStrategy = cacheConcurrencyStrategy;
	}

	public String getCacheRegionName() {
		return cacheRegionName;
	}

	public void setCacheRegionName(String cacheRegionName) {
		this.cacheRegionName = cacheRegionName;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
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

	public String getManyToManyOrderBy() {
		return manyToManyOrderBy;
	}

	public void setManyToManyOrderBy(String manyToManyOrderBy) {
		this.manyToManyOrderBy = manyToManyOrderBy;
	}

	public String getReferencedPropertyName() {
		return referencedPropertyName;
	}

	public void setReferencedPropertyName(String referencedPropertyName) {
		this.referencedPropertyName = referencedPropertyName;
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

	public boolean isSorted() {
		return sorted;
	}

	public void setSorted(boolean sorted) {
		this.sorted = sorted;
	}

	public Comparator getComparator() {
		return comparator;
	}

	public void setComparator(Comparator comparator) {
		this.comparator = comparator;
	}

	public String getComparatorClassName() {
		return comparatorClassName;
	}

	public void setComparatorClassName(String comparatorClassName) {
		this.comparatorClassName = comparatorClassName;
	}

	public boolean isOrphanDelete() {
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

	public boolean isEmbedded() {
		return embedded;
	}

	public void setEmbedded(boolean embedded) {
		this.embedded = embedded;
	}

	public boolean isOptimisticLocked() {
		return optimisticLocked;
	}

	public void setOptimisticLocked(boolean optimisticLocked) {
		this.optimisticLocked = optimisticLocked;
	}

	public Class getCollectionPersisterClass() {
		return collectionPersisterClass;
	}

	public void setCollectionPersisterClass(Class collectionPersisterClass) {
		this.collectionPersisterClass = collectionPersisterClass;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public CustomSQL getCustomSQLInsert() {
		return customSQLInsert;
	}

	public void setCustomSQLInsert(CustomSQL customSQLInsert) {
		this.customSQLInsert = customSQLInsert;
	}

	public CustomSQL getCustomSQLUpdate() {
		return customSQLUpdate;
	}

	public void setCustomSQLUpdate(CustomSQL customSQLUpdate) {
		this.customSQLUpdate = customSQLUpdate;
	}

	public CustomSQL getCustomSQLDelete() {
		return customSQLDelete;
	}

	public void setCustomSQLDelete(CustomSQL customSQLDelete) {
		this.customSQLDelete = customSQLDelete;
	}

	public CustomSQL getCustomSQLDeleteAll() {
		return customSQLDeleteAll;
	}

	public void setCustomSQLDeleteAll(CustomSQL customSQLDeleteAll) {
		this.customSQLDeleteAll = customSQLDeleteAll;
	}

	public String getLoaderName() {
		return loaderName;
	}

	public void setLoaderName(String loaderName) {
		this.loaderName = loaderName;
	}
}
