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
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.jboss.logging.Logger;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.relational.Table;
import org.hibernate.metamodel.source.hbm.HbmHelper;
import org.hibernate.metamodel.source.util.DomHelper;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public abstract class PluralAttributeBinding extends AbstractAttributeBinding {

	public static interface DomainState extends AbstractAttributeBinding.DomainState {
		FetchMode getFetchMode();
		boolean isExtraLazy();
		CollectionElement getCollectionElement(PluralAttributeBinding binding);
		boolean isInverse();
		boolean isMutable();
		boolean isSubselectLoadable();
		String getCacheConcurrencyStrategy();
		String getCacheRegionName();
		String getOrderBy();
		String getWhere();
		String getReferencedPropertyName();
		boolean isSorted();
		Comparator getComparator();
		String getComparatorClassName();
		boolean isOrphanDelete();
		int getBatchSize();
		boolean isEmbedded();
		boolean isOptimisticLocked();
		Class getCollectionPersisterClass();
		String getTypeName();
		java.util.Map getFilters();
		java.util.Set getSynchronizedTables();
		CustomSQL getCustomSQLInsert();
		CustomSQL getCustomSQLUpdate();
		CustomSQL getCustomSQLDelete();
		CustomSQL getCustomSQLDeleteAll();
		String getLoaderName();
	}

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class, PluralAttributeBinding.class.getName()
	);

	private Table collectionTable;

	private CollectionKey collectionKey;
	private CollectionElement collectionElement;

//	private String role;
	private FetchMode fetchMode;
	private boolean extraLazy;
	private boolean inverse;
	private boolean mutable = true;
	private boolean subselectLoadable;
	private String cacheConcurrencyStrategy;
	private String cacheRegionName;
	private String orderBy;
	private String where;
	private String referencedPropertyName;
	private boolean sorted;
	private Comparator comparator;
	private String comparatorClassName;
	private boolean orphanDelete;
	private int batchSize = -1;
	private boolean embedded = true;
	private boolean optimisticLocked = true;
	private Class collectionPersisterClass;
	private String typeName;
	private final java.util.Map filters = new HashMap();
	private final java.util.Set<String> synchronizedTables = new HashSet<String>();

	private CustomSQL customSQLInsert;
	private CustomSQL customSQLUpdate;
	private CustomSQL customSQLDelete;
	private CustomSQL customSQLDeleteAll;

	private String loaderName;

	protected PluralAttributeBinding(EntityBinding entityBinding) {
		super( entityBinding );
		collectionElement = new CollectionElement( this );
	}

	public void initialize(DomainState state) {
		super.initialize( state );
		fetchMode = state.getFetchMode();
		extraLazy = state.isExtraLazy();
		collectionElement = state.getCollectionElement( this );
		inverse = state.isInverse();
		mutable = state.isMutable();
		subselectLoadable = state.isSubselectLoadable();
		if ( isSubselectLoadable() ) {
			getEntityBinding().setSubselectLoadableCollections( true );
		}
		cacheConcurrencyStrategy = state.getCacheConcurrencyStrategy();
		cacheRegionName = state.getCacheRegionName();
		orderBy = state.getOrderBy();
		where = state.getWhere();
		referencedPropertyName = state.getReferencedPropertyName();
		sorted = state.isSorted();
		comparator = state.getComparator();
		comparatorClassName = state.getComparatorClassName();
		orphanDelete = state.isOrphanDelete();
		batchSize = state.getBatchSize();
		embedded = state.isEmbedded();
		optimisticLocked = state.isOptimisticLocked();
		collectionPersisterClass = state.getCollectionPersisterClass();
		typeName = state.getTypeName();
		filters.putAll( state.getFilters() );
		synchronizedTables.addAll( state.getSynchronizedTables() );
		customSQLInsert = state.getCustomSQLInsert();
		customSQLUpdate = state.getCustomSQLUpdate();
		customSQLDelete = state.getCustomSQLDelete();
		customSQLDeleteAll = state.getCustomSQLDeleteAll();
		loaderName = state.getLoaderName();
	}



	protected boolean isLazyDefault(MappingDefaults defaults) {
		return defaults.isDefaultLazy();
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
	public boolean isExtraLazy() {
		return extraLazy;
	}

	public boolean isInverse() {
		return inverse;
	}

	public boolean isMutable() {
		return mutable;
	}

	public boolean isSubselectLoadable() {
		return subselectLoadable;
	}

	public String getCacheConcurrencyStrategy() {
		return cacheConcurrencyStrategy;
	}

	public String getCacheRegionName() {
		return cacheRegionName;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public String getWhere() {
		return where;
	}

	public String getReferencedPropertyName() {
		return referencedPropertyName;
	}

	public boolean isSorted() {
		return sorted;
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

	public boolean isOrphanDelete() {
		return orphanDelete;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public boolean isOptimisticLocked() {
		return optimisticLocked;
	}

	public Class getCollectionPersisterClass() {
		return collectionPersisterClass;
	}

	public String getTypeName() {
		return typeName;
	}

	public void addFilter(String name, String condition) {
		filters.put( name, condition );
	}

	public java.util.Map getFilterMap() {
		return filters;
	}

	public CustomSQL getCustomSQLInsert() {
		return customSQLInsert;
	}

	public CustomSQL getCustomSQLUpdate() {
		return customSQLUpdate;
	}

	public CustomSQL getCustomSQLDelete() {
		return customSQLDelete;
	}

	public CustomSQL getCustomSQLDeleteAll() {
		return customSQLDeleteAll;
	}

	public String getLoaderName() {
		return loaderName;
	}
}
