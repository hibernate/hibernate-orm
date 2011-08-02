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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.PluralAttribute;
import org.hibernate.metamodel.relational.Table;
import org.hibernate.metamodel.relational.TableSpecification;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public abstract class AbstractPluralAttributeBinding extends AbstractAttributeBinding implements PluralAttributeBinding {
	private final CollectionKey collectionKey;
	private final AbstractCollectionElement collectionElement;

	private Table collectionTable;

	private CascadeStyle cascadeStyle;
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
	private final java.util.Map filters = new HashMap();
	private final java.util.Set<String> synchronizedTables = new HashSet<String>();

	private CustomSQL customSQLInsert;
	private CustomSQL customSQLUpdate;
	private CustomSQL customSQLDelete;
	private CustomSQL customSQLDeleteAll;

	private String loaderName;

	protected AbstractPluralAttributeBinding(
			AttributeBindingContainer container,
			PluralAttribute attribute,
			CollectionElementNature collectionElementNature) {
		super( container, attribute );
		this.collectionKey = new CollectionKey( this );
		this.collectionElement = interpretNature( collectionElementNature );
	}

	private AbstractCollectionElement interpretNature(CollectionElementNature collectionElementNature) {
		switch ( collectionElementNature ) {
			case BASIC: {
				return new BasicCollectionElement( this );
			}
			case COMPOSITE: {
				return new CompositeCollectionElement( this );
			}
			case ONE_TO_MANY: {
				return new OneToManyCollectionElement( this );
			}
			case MANY_TO_MANY: {
				return new ManyToManyCollectionElement( this );
			}
			case MANY_TO_ANY: {
				return new ManyToAnyCollectionElement( this );
			}
			default: {
				throw new AssertionFailure( "Unknown collection element nature : " + collectionElementNature );
			}
		}
	}

//	protected void initializeBinding(PluralAttributeBindingState state) {
//		super.initialize( state );
//		fetchMode = state.getFetchMode();
//		extraLazy = state.isExtraLazy();
//		collectionElement.setNodeName( state.getElementNodeName() );
//		collectionElement.setTypeName( state.getElementTypeName() );
//		inverse = state.isInverse();
//		mutable = state.isMutable();
//		subselectLoadable = state.isSubselectLoadable();
//		if ( isSubselectLoadable() ) {
//			getEntityBinding().setSubselectLoadableCollections( true );
//		}
//		cacheConcurrencyStrategy = state.getCacheConcurrencyStrategy();
//		cacheRegionName = state.getCacheRegionName();
//		orderBy = state.getOrderBy();
//		where = state.getWhere();
//		referencedPropertyName = state.getReferencedPropertyName();
//		sorted = state.isSorted();
//		comparator = state.getComparator();
//		comparatorClassName = state.getComparatorClassName();
//		orphanDelete = state.isOrphanDelete();
//		batchSize = state.getBatchSize();
//		embedded = state.isEmbedded();
//		optimisticLocked = state.isOptimisticLocked();
//		collectionPersisterClass = state.getCollectionPersisterClass();
//		filters.putAll( state.getFilters() );
//		synchronizedTables.addAll( state.getSynchronizedTables() );
//		customSQLInsert = state.getCustomSQLInsert();
//		customSQLUpdate = state.getCustomSQLUpdate();
//		customSQLDelete = state.getCustomSQLDelete();
//		customSQLDeleteAll = state.getCustomSQLDeleteAll();
//		loaderName = state.getLoaderName();
//	}

	@Override
	public PluralAttribute getAttribute() {
		return (PluralAttribute) super.getAttribute();
	}

	@Override
	public boolean isAssociation() {
		return collectionElement.getCollectionElementNature() == CollectionElementNature.MANY_TO_ANY
				|| collectionElement.getCollectionElementNature() == CollectionElementNature.MANY_TO_MANY
				|| collectionElement.getCollectionElementNature() == CollectionElementNature.ONE_TO_MANY;
	}

	public TableSpecification getCollectionTable() {
		return collectionTable;
	}

	public void setCollectionTable(Table collectionTable) {
		this.collectionTable = collectionTable;
	}

	public CollectionKey getCollectionKey() {
		return collectionKey;
	}

	public AbstractCollectionElement getCollectionElement() {
		return collectionElement;
	}

	@Override
	public CascadeStyle getCascadeStyle() {
		return cascadeStyle;
	}

	@Override
	public void setCascadeStyles(Iterable<CascadeStyle> cascadeStyles) {
		List<CascadeStyle> cascadeStyleList = new ArrayList<CascadeStyle>();
		for ( CascadeStyle style : cascadeStyles ) {
			if ( style != CascadeStyle.NONE ) {
				cascadeStyleList.add( style );
			}
		}
		if ( cascadeStyleList.isEmpty() ) {
			cascadeStyle = CascadeStyle.NONE;
		}
		else if ( cascadeStyleList.size() == 1 ) {
			cascadeStyle = cascadeStyleList.get( 0 );
		}
		else {
			cascadeStyle = new CascadeStyle.MultipleCascadeStyle(
					cascadeStyleList.toArray( new CascadeStyle[ cascadeStyleList.size() ] )
			);
		}
	}

	@Override
	public FetchMode getFetchMode() {
		return fetchMode;
	}

	@Override
	public void setFetchMode(FetchMode fetchMode) {
		this.fetchMode = fetchMode;
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

	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

	public String getWhere() {
		return where;
	}

	public void setWhere(String where) {
		this.where = where;
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

	public boolean isEmbedded() {
		return embedded;
	}

	public Class getCollectionPersisterClass() {
		return collectionPersisterClass;
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
