/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.hbm.state.domain;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.metamodel.binding.CollectionElement;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.ElementCollectionElement;
import org.hibernate.metamodel.binding.MappingDefaults;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.source.hbm.HbmHelper;
import org.hibernate.metamodel.source.util.MappingHelper;


/**
 * @author Gail Badner
 */
public class HbmPluralAttributeDomainState extends AbstractHbmAttributeDomainState implements PluralAttributeBinding.DomainState {
	private final org.hibernate.metamodel.source.hbm.xml.mapping.Bag collection;

	public HbmPluralAttributeDomainState(MappingDefaults defaults,
										 org.hibernate.metamodel.source.hbm.xml.mapping.Bag collection,
										 Map<String, MetaAttribute> entityMetaAttributes,
										 Attribute attribute) {
		super( defaults, attribute, entityMetaAttributes, collection );
		this.collection = collection;
	}

	public FetchMode getFetchMode() {
		FetchMode fetchMode;
		if ( collection.getFetch() != null ) {
			fetchMode = "join".equals( collection.getFetch() ) ? FetchMode.JOIN : FetchMode.SELECT;
		}
		else {
			String jfNodeValue = ( collection.getOuterJoin() == null ? "auto" : collection.getOuterJoin() );
			if ( "auto".equals( jfNodeValue ) ) {
				fetchMode = FetchMode.DEFAULT;
			}
			else if ( "true".equals( jfNodeValue ) ) {
				fetchMode = FetchMode.JOIN;
			}
			else {
				fetchMode = FetchMode.SELECT;
			}
		}
		return fetchMode;
	}

	public boolean isLazy() {
		return isExtraLazy() ||
				MappingHelper.getBooleanValue( collection.getLazy(), getDefaults().isDefaultLazy());
	}

	public boolean isExtraLazy() {
		return  ( "extra".equals( collection.getLazy() ) );
	}

	public CollectionElement getCollectionElement(PluralAttributeBinding binding) {
		ElementCollectionElement collectionElement = new ElementCollectionElement( binding );
		collectionElement.initialize( new HbmCollectionElementDomainState( collection.getElement() ) );
		return collectionElement;
	}

	public boolean isInverse() {
		return MappingHelper.getBooleanValue( collection.getInverse(), false );
	}

	public boolean isMutable() {
		return MappingHelper.getBooleanValue( collection.getMutable(), true );
	}

	public boolean isSubselectLoadable() {
		return "subselect".equals( collection.getFetch() );
	}
	public String getCacheConcurrencyStrategy() {
		return collection.getCache() == null ?
				null :
				collection.getCache().getUsage();
	}
	public String getCacheRegionName() {
		return collection.getCache() == null ?
				null :
				collection.getCache().getRegion();
	}
	public String getOrderBy() {
		return collection.getOrderBy();
	}
	public String getWhere() {
		return collection.getWhere();
	}
	public String getReferencedPropertyName() {
		return collection.getKey().getPropertyRef();
	}
	public boolean isSorted() {
		// SORT
		// unsorted, natural, comparator.class.name
		return ( ! "unsorted".equals( getSortString() ) );
	}
	public Comparator getComparator() {
		return null;
	}

	public String getComparatorClassName() {
		String sortString = getSortString();
		return (
				isSorted() && ! "natural".equals( sortString ) ?
						sortString :
						null
		);
	}

	private String getSortString() {
		//TODO: Bag does not define getSort(); update this when there is a Collection subtype
		// collection.getSort() == null ? "unsorted" : collection.getSort();
		return "unsorted";
	}
	public boolean isOrphanDelete() {
		// ORPHAN DELETE (used for programmer error detection)
		return ( getCascade().indexOf( "delete-orphan" ) >= 0 );
	}
	public int getBatchSize() {
		return MappingHelper.getIntValue( collection.getBatchSize(), 0 );
	}
	public boolean isEmbedded() {
		return MappingHelper.getBooleanValue( collection.getEmbedXml(), true );
	}
	public boolean isOptimisticLocked() {
		return MappingHelper.getBooleanValue( collection.getOptimisticLock(), true );
	}

	public Class getCollectionPersisterClass() {
		try {
			return MappingHelper.getClassValue( collection.getPersister() );
		}
		catch (ClassNotFoundException cnfe) {
			throw new MappingException( "Could not find collection persister class: "
				+ collection.getPersister() );
		}
	}
	public String getTypeName() {
		// TODO: does this go here???
		//Attribute typeNode = collectionElement.attribute( "collection-type" );
		//if ( typeNode != null ) {
			// TODO: implement when typedef binding is implemented
			/*
			String typeName = typeNode.getValue();
			TypeDef typeDef = mappings.getTypeDef( typeName );
			if ( typeDef != null ) {
				collectionBinding.setTypeName( typeDef.getTypeClass() );
				collectionBinding.setTypeParameters( typeDef.getParameters() );
			}
			else {
				collectionBinding.setTypeName( typeName );
			}
			*/
		//}
		return null;
	}

	public java.util.Map getFilters() {
		// TODO: IMPLEMENT
		//Iterator iter = collectionElement.elementIterator( "filter" );
		//while ( iter.hasNext() ) {
		//	final Element filter = (Element) iter.next();
		//	parseFilter( filter, collectionElement, collectionBinding );
		//}
		return new HashMap();
	}
	public java.util.Set getSynchronizedTables() {
		java.util.Set<String> synchronizedTables = new HashSet<String>();
		for ( org.hibernate.metamodel.source.hbm.xml.mapping.Synchronize sync : collection.getSynchronize() ) {
			synchronizedTables.add( sync.getTable() );
		}
		return synchronizedTables;
	}

	public CustomSQL getCustomSQLInsert() {
		org.hibernate.metamodel.source.hbm.xml.mapping.SqlInsert sqlInsert = collection.getSqlInsert();
		return sqlInsert == null ?
				null :
				HbmHelper.getCustomSql(
						collection.getSqlInsert().getContent(),
						MappingHelper.getBooleanValue( collection.getSqlInsert().getCallable(), false ),
						collection.getSqlInsert().getCheck()
				);
	}
	public CustomSQL getCustomSQLUpdate() {
		org.hibernate.metamodel.source.hbm.xml.mapping.SqlUpdate sqlUpdate = collection.getSqlUpdate();
		return sqlUpdate == null ?
				null :
				HbmHelper.getCustomSql(
						collection.getSqlUpdate().getContent(),
						MappingHelper.getBooleanValue( collection.getSqlUpdate().getCallable(), false ),
						collection.getSqlUpdate().getCheck()
				);
	}
	public CustomSQL getCustomSQLDelete() {
		org.hibernate.metamodel.source.hbm.xml.mapping.SqlDelete sqlDelete = collection.getSqlDelete();
		return sqlDelete == null ?
				null :
				HbmHelper.getCustomSql(
						collection.getSqlDelete().getContent(),
						MappingHelper.getBooleanValue( collection.getSqlDelete().getCallable(), false ),
						collection.getSqlDelete().getCheck()
				);
	}
	public CustomSQL getCustomSQLDeleteAll() {
		org.hibernate.metamodel.source.hbm.xml.mapping.SqlDeleteAll sqlDeleteAll = collection.getSqlDeleteAll();
		return sqlDeleteAll == null ?
				null :
				HbmHelper.getCustomSql(
						collection.getSqlDeleteAll().getContent(),
						MappingHelper.getBooleanValue( collection.getSqlDeleteAll().getCallable(), false ),
						collection.getSqlDeleteAll().getCheck()
				);
	}
	public String getLoaderName() {
		return collection.getLoader() == null ?
				null :
				collection.getLoader().getQueryRef();
	}

	public boolean isKeyCasadeDeleteEnabled() {
		//TODO: implement
		return false;
	}
	public String getUnsavedValue() {
		//TODO: implement
		return null;
	}
}
