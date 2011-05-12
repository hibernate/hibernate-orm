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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.binding.MappingDefaults;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.source.hbm.HbmHelper;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLBagElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSqlDeleteAllElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSqlDeleteElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSqlInsertElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSqlUpdateElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSynchronizeElement;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.metamodel.source.util.MappingHelper;
import org.hibernate.metamodel.state.domain.CollectionElementDomainState;
import org.hibernate.metamodel.state.domain.PluralAttributeDomainState;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.classloading.spi.ClassLoadingException;


/**
 * @author Gail Badner
 */
public class HbmPluralAttributeDomainState extends AbstractHbmAttributeDomainState
		implements PluralAttributeDomainState {
	private final XMLBagElement collection;
	private final HibernateTypeDescriptor hibernateTypeDescriptor = new HibernateTypeDescriptor();
	private final String cascade;

	public HbmPluralAttributeDomainState(
			MetadataImpl metadata,
			MappingDefaults mappingDefaults,
			XMLBagElement collection,
			Map<String, MetaAttribute> entityMetaAttributes,
			Attribute attribute) {
		super(
				metadata,
				mappingDefaults,
				attribute,
				collection.getNode(),
				HbmHelper.extractMetas( collection.getMeta(), entityMetaAttributes ),
				HbmHelper.getPropertyAccessorName(
						collection.getAccess(), collection.isEmbedXml(), mappingDefaults.getDefaultAccess()
				),
				collection.isOptimisticLock()
		);
		this.collection = collection;
		// TODO: is collection.getCollectionType() correct here?
		this.hibernateTypeDescriptor.setTypeName( collection.getCollectionType() );
		this.cascade = MappingHelper.getStringValue( collection.getCascade(), mappingDefaults.getDefaultCascade() );
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
		//TODO: fix this!!!
		this.hibernateTypeDescriptor.setTypeName( collection.getCollectionType() );
	}

	public FetchMode getFetchMode() {
		FetchMode fetchMode;
		if ( collection.getFetch() != null ) {
			fetchMode = "join".equals( collection.getFetch() ) ? FetchMode.JOIN : FetchMode.SELECT;
		}
		else {
			String jfNodeValue = ( collection.getOuterJoin().value() == null ? "auto" : collection.getOuterJoin()
					.value() );
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
				MappingHelper.getBooleanValue( collection.getLazy().value(), getDefaults().isDefaultLazy() );
	}

	public boolean isExtraLazy() {
		return ( "extra".equals( collection.getLazy() ) );
	}

	public CollectionElementDomainState getCollectionElementDomainState() {
		return new HbmCollectionElementDomainState( collection.getElement() );
	}

	public boolean isInverse() {
		return collection.isInverse();
	}

	public boolean isMutable() {
		return collection.isMutable();
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
		return ( !"unsorted".equals( getSortString() ) );
	}

	public Comparator getComparator() {
		return null;
	}

	public String getComparatorClassName() {
		String sortString = getSortString();
		return (
				isSorted() && !"natural".equals( sortString ) ?
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

	@Override
	public boolean isEmbedded() {
		return collection.isEmbedXml();
	}

	public boolean isOptimisticLocked() {
		return collection.isOptimisticLock();
	}

	public Class getCollectionPersisterClass() {
		String className = collection.getPersister();
		ClassLoaderService classLoaderService = getMetadata().getServiceRegistry()
				.getService( ClassLoaderService.class );
		try {
			return classLoaderService.classForName( className );
		}
		catch ( ClassLoadingException e ) {
			throw new MappingException(
					"Could not find collection persister class: "
							+ collection.getPersister()
			);
		}
	}

	public HibernateTypeDescriptor getHibernateTypeDescriptor() {
		return hibernateTypeDescriptor;
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
		for ( XMLSynchronizeElement sync : collection.getSynchronize() ) {
			synchronizedTables.add( sync.getTable() );
		}
		return synchronizedTables;
	}

	public CustomSQL getCustomSQLInsert() {
		XMLSqlInsertElement sqlInsert = collection.getSqlInsert();
		return sqlInsert == null ?
				null :
				HbmHelper.getCustomSql(
						collection.getSqlInsert().getValue(),
						collection.getSqlInsert().isCallable(),
						collection.getSqlInsert().getCheck().value()
				);
	}

	public CustomSQL getCustomSQLUpdate() {
		XMLSqlUpdateElement sqlUpdate = collection.getSqlUpdate();
		return sqlUpdate == null ?
				null :
				HbmHelper.getCustomSql(
						collection.getSqlUpdate().getValue(),
						collection.getSqlUpdate().isCallable(),
						collection.getSqlUpdate().getCheck().value()
				);
	}

	public CustomSQL getCustomSQLDelete() {
		XMLSqlDeleteElement sqlDelete = collection.getSqlDelete();
		return sqlDelete == null ?
				null :
				HbmHelper.getCustomSql(
						collection.getSqlDelete().getValue(),
						collection.getSqlDelete().isCallable(),
						collection.getSqlDelete().getCheck().value()
				);
	}

	public CustomSQL getCustomSQLDeleteAll() {
		XMLSqlDeleteAllElement sqlDeleteAll = collection.getSqlDeleteAll();
		return sqlDeleteAll == null ?
				null :
				HbmHelper.getCustomSql(
						collection.getSqlDeleteAll().getValue(),
						collection.getSqlDeleteAll().isCallable(),
						collection.getSqlDeleteAll().getCheck().value()
				);
	}

	public String getLoaderName() {
		return collection.getLoader() == null ?
				null :
				collection.getLoader().getQueryRef();
	}

	public String getCascade() {
		return cascade;
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
