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

import org.dom4j.Element;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.metamodel.binding.CollectionElement;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.ElementCollectionElement;
import org.hibernate.metamodel.binding.MappingDefaults;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.source.hbm.HbmHelper;
import org.hibernate.metamodel.source.util.DomHelper;

/**
 * @author Gail Badner
 */
public class HbmPluralAttributeDomainState extends AbstractHbmAttributeDomainState implements PluralAttributeBinding.DomainState {
	public HbmPluralAttributeDomainState(MappingDefaults defaults,
										 Element element,
										 Attribute attribute) {
		super( defaults, element, attribute );
	}

	public FetchMode getFetchMode() {
		FetchMode fetchMode;
		org.dom4j.Attribute fetchModeAttribute = getElement().attribute( "fetch" );
		if ( fetchModeAttribute != null ) {
			fetchMode = "join".equals( fetchModeAttribute.getValue() ) ? FetchMode.JOIN : FetchMode.SELECT;
		}
		else {
			org.dom4j.Attribute jfNode = getElement().attribute( "outer-join" );
			String jfNodeValue = ( jfNode == null ? "auto" : jfNode.getValue() );
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
				DomHelper.extractBooleanAttributeValue( getElement(), "lazy", getDefaults().isDefaultLazy() );
	}

	public boolean isExtraLazy() {
		String lazyString = DomHelper.extractAttributeValue( getElement(), "lazy" );
		return ( "extra".equals( lazyString ) );
	}
	public CollectionElement getCollectionElement(PluralAttributeBinding binding) {
		Element element = getElement().element( "element" );
		if ( element != null ) {
			ElementCollectionElement collectionElement = new ElementCollectionElement( binding );
			collectionElement.initialize( new HbmCollectionElementDomainState( element ) );
		}
		// TODO: implement other types of collection elements
		return null;
	}

	public boolean isInverse() {
		return DomHelper.extractBooleanAttributeValue( getElement(), "inverse", false );
	}
	public boolean isMutable() {
		return DomHelper.extractBooleanAttributeValue( getElement(), "mutable", true );
	}
	public boolean isSubselectLoadable() {
		return "subselect".equals( getElement().attributeValue( "fetch" ) );
	}
	public String getCacheConcurrencyStrategy() {
		Element cacheElement = getElement().element( "cache" );
		return cacheElement == null ? null : cacheElement.attributeValue( "usage" );
	}
	public String getCacheRegionName() {
		Element cacheElement = getElement().element( "cache" );
		return cacheElement == null ? null : cacheElement.attributeValue( "region" );
	}
	public String getOrderBy() {
		return DomHelper.extractAttributeValue( getElement(), "order-by", null );
	}
	public String getWhere() {
		return DomHelper.extractAttributeValue( getElement(), "where", null );
	}
	public String getReferencedPropertyName() {
		return getElement().element( "key" ).attributeValue( "property-ref" );
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
		return DomHelper.extractAttributeValue( getElement(), "sort", "unsorted" );
	}
	public boolean isOrphanDelete() {
		// ORPHAN DELETE (used for programmer error detection)
		return ( getCascade().indexOf( "delete-orphan" ) >= 0 );
	}
	public int getBatchSize() {
		return DomHelper.extractIntAttributeValue( getElement(), "batch-size", 0 );
	}
	public boolean isEmbedded() {
		return DomHelper.extractBooleanAttributeValue( getElement(), "embed-xml", true );
	}
	public boolean isOptimisticLocked() {
		return true;
	}

	public Class getCollectionPersisterClass() {
		try {
			return DomHelper.extractClassAttributeValue( getElement(), "persister" );
		}
		catch (ClassNotFoundException cnfe) {
			throw new MappingException( "Could not find collection persister class: "
				+ getElement().attributeValue( "persister" ) );
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
		Iterator tables = getElement().elementIterator( "synchronize" );
		while ( tables.hasNext() ) {
			synchronizedTables.add( ( ( Element ) tables.next() ).attributeValue( "table" ) );
		}
		return synchronizedTables;
	}

	public CustomSQL getCustomSQLInsert() {
		return HbmHelper.getCustomSql( getElement().element( "sql-insert" ) );
	}
	public CustomSQL getCustomSQLUpdate() {
		return HbmHelper.getCustomSql( getElement().element( "sql-update" ) );
	}
	public CustomSQL getCustomSQLDelete() {
		return HbmHelper.getCustomSql( getElement().element( "sql-delete" ) );

	}
	public CustomSQL getCustomSQLDeleteAll() {
		return HbmHelper.getCustomSql( getElement().element( "sql-delete-all" ) );
	}
	public String getLoaderName() {
		return DomHelper.extractAttributeValue( getElement().element( "loader" ), "query-ref" );
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
