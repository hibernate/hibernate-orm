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
package org.hibernate.metamodel.source.hbm.state.binding;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.FetchMode;
import org.hibernate.metamodel.binding.CascadeType;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.state.PluralAttributeBindingState;
import org.hibernate.metamodel.source.BindingContext;
import org.hibernate.metamodel.source.MetaAttributeContext;
import org.hibernate.metamodel.source.hbm.Helper;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLBagElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLSqlDeleteAllElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLSqlDeleteElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLSqlInsertElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLSqlUpdateElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLSynchronizeElement;

/**
 * @author Gail Badner
 */
public class HbmPluralAttributeBindingState extends AbstractHbmAttributeBindingState
		implements PluralAttributeBindingState {
	private final XMLBagElement collection;
	private final Class collectionPersisterClass;
	private final Set<CascadeType> cascadeTypes;

	private final String explicitHibernateCollectionTypeName;
	private final Class javaType;

	public HbmPluralAttributeBindingState(
			String ownerClassName,
			BindingContext bindingContext,
			MetaAttributeContext parentMetaAttributeContext,
			XMLBagElement collection) {
		super(
				ownerClassName,
				collection.getName(),
				bindingContext,
				collection.getNode(),
				Helper.extractMetaAttributeContext( collection.getMeta(), parentMetaAttributeContext ),
				Helper.getPropertyAccessorName(
						collection.getAccess(),
						collection.isEmbedXml(),
						bindingContext.getMappingDefaults().getPropertyAccessorName()
				),
				collection.isOptimisticLock()
		);
		this.collection = collection;
		this.collectionPersisterClass = Helper.classForName(
				collection.getPersister(), getBindingContext().getServiceRegistry()
		);
		this.cascadeTypes = determineCascadeTypes( collection.getCascade() );

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
		this.explicitHibernateCollectionTypeName = collection.getCollectionType();
		this.javaType = java.util.Collection.class;
	}

	public FetchMode getFetchMode() {
		FetchMode fetchMode;
		if ( collection.getFetch() != null ) {
			fetchMode = "join".equals( collection.getFetch().value() ) ? FetchMode.JOIN : FetchMode.SELECT;
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
				Helper.getBooleanValue(
						collection.getLazy().value(), getBindingContext().getMappingDefaults().areAssociationsLazy()
				);
	}

	public boolean isExtraLazy() {
		return ( "extra".equals( collection.getLazy().value() ) );
	}

	public String getElementTypeName() {
		return collection.getElement().getTypeAttribute();

	}

	public String getElementNodeName() {
		return collection.getElement().getNode();
	}

	public boolean isInverse() {
		return collection.isInverse();
	}

	public boolean isMutable() {
		return collection.isMutable();
	}

	public boolean isSubselectLoadable() {
		return "subselect".equals( collection.getFetch().value() );
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
		return true;
		//return ( getCascade().indexOf( "delete-orphan" ) >= 0 );
	}

	public int getBatchSize() {
		return Helper.getIntValue( collection.getBatchSize(), 0 );
	}

	@Override
	public boolean isEmbedded() {
		return collection.isEmbedXml();
	}

	public boolean isOptimisticLocked() {
		return collection.isOptimisticLock();
	}

	public Class getCollectionPersisterClass() {
		return collectionPersisterClass;
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
		return Helper.buildCustomSql( sqlInsert );
	}

	public CustomSQL getCustomSQLUpdate() {
		XMLSqlUpdateElement sqlUpdate = collection.getSqlUpdate();
		return Helper.buildCustomSql( sqlUpdate );
	}

	public CustomSQL getCustomSQLDelete() {
		XMLSqlDeleteElement sqlDelete = collection.getSqlDelete();
		return Helper.buildCustomSql( sqlDelete );
	}

	public CustomSQL getCustomSQLDeleteAll() {
		XMLSqlDeleteAllElement sqlDeleteAll = collection.getSqlDeleteAll();
		return Helper.buildCustomSql( sqlDeleteAll );
	}

	public String getLoaderName() {
		return collection.getLoader() == null ?
				null :
				collection.getLoader().getQueryRef();
	}

	public Set<CascadeType> getCascadeTypes() {
		return cascadeTypes;
	}

	public boolean isKeyCascadeDeleteEnabled() {
		//TODO: implement
		return false;
	}

	public String getUnsavedValue() {
		//TODO: implement
		return null;
	}

	public String getExplicitHibernateTypeName() {
		return explicitHibernateCollectionTypeName;
	}

	@Override
	public String getJavaTypeName() {
		return javaType.getName();
	}
}
