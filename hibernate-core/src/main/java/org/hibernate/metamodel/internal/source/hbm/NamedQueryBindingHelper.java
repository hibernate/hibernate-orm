/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.metamodel.internal.source.hbm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.spi.NamedQueryDefinitionBuilder;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinitionBuilder;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jaxb.spi.hbm.JaxbLoadCollectionElement;
import org.hibernate.jaxb.spi.hbm.JaxbQueryElement;
import org.hibernate.jaxb.spi.hbm.JaxbReturnElement;
import org.hibernate.jaxb.spi.hbm.JaxbReturnPropertyElement;
import org.hibernate.jaxb.spi.hbm.JaxbSqlQueryElement;
import org.hibernate.metamodel.internal.source.hbm.parser.BasicQueryElementContentParser;
import org.hibernate.metamodel.internal.source.hbm.parser.SQLQueryElementContentParser;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.SingularAssociationAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.LocalBindingContext;

/**
 * Helper class used to bind named query elements.
 *
 * @author Brett Meyer
 * @author Strong Liu
 */
public class NamedQueryBindingHelper {

	/**
	 * Helper method used to bind {@code <query/>} element.
	 *
	 * @param queryElement The {@code <query/>} element.
	 * @param metadata  The {@link org.hibernate.metamodel.Metadata} which this named query will be added to.
	 */
	public static void bindNamedQuery(
			final JaxbQueryElement queryElement,
			final MetadataImplementor metadata) {
		final NamedQueryDefinitionBuilder builder = new NamedQueryDefinitionBuilder();
		final BasicQueryElementContentParser parser = new BasicQueryElementContentParser();
		parser.parse( builder, queryElement );
		metadata.addNamedQuery( builder.createNamedQueryDefinition() );
	}

	public static void bindNamedSQLQuery(
			final JaxbSqlQueryElement queryElement,
			final LocalBindingContext bindingContext,
			final MetadataImplementor metadata) {
		final NamedSQLQueryDefinitionBuilder builder = new NamedSQLQueryDefinitionBuilder();
		final SQLQueryElementContentParser parser = new SQLQueryElementContentParser();

		parser.parse( builder, queryElement );

		final boolean callable = queryElement.isCallable();
		final String resultSetRef = queryElement.getResultsetRef();
		//TODO: check if the refereneced result set mapping definition exist or not?
		builder.setCallable( callable ).setResultSetRef( resultSetRef );

		NamedSQLQueryDefinition namedQuery;
		//query returns are defined in the <resultset/> element.
		if ( StringHelper.isNotEmpty( resultSetRef ) ) {
			namedQuery = builder.createNamedQueryDefinition();
		}
		else {
			namedQuery = parser.buildQueryReturns( queryElement.getName(),builder, bindingContext, metadata );
		}
		metadata.addNamedNativeQuery( namedQuery );
	}



	private static Map<String, String[]> bindPropertyResults(
			String alias, JaxbLoadCollectionElement element,
			EntityBinding entityBinding) {
		List<JaxbReturnPropertyElement> returnPropertyElements
				= element.getReturnProperty();
		List<JaxbReturnPropertyElement> properties
				= new ArrayList<JaxbReturnPropertyElement>();
		List<String> propertyNames = new ArrayList<String>();
		HashMap<String, ArrayList<String>> propertyResults = new HashMap<String, ArrayList<String>>();
		for ( JaxbReturnPropertyElement propertyElement : returnPropertyElements ) {
			String name = propertyElement.getName();
			if ( entityBinding == null || name.indexOf( '.' ) == -1 ) {
				properties.add( propertyElement );
				propertyNames.add( name );
			}
			else {
				/**
				 * Reorder properties
				 * 1. get the parent property
				 * 2. list all the properties following the expected one in the parent property
				 * 3. calculate the lowest index and insert the property
				 */
				if ( entityBinding == null ) {
					throw new org.hibernate.MappingException(
							"dotted notation in <return-join> or <load_collection> not yet supported"
					);
				}
				int dotIndex = name.lastIndexOf( '.' );
				String reducedName = name.substring( 0, dotIndex );
				AttributeBinding value = null;// getRecursiveAttributeBinding(entityBinding, reducedName );
				Iterable<AttributeBinding> parentPropIter;
				if ( CompositeAttributeBinding.class.isInstance( value ) ) {
					CompositeAttributeBinding comp
							= (CompositeAttributeBinding) value;
					parentPropIter = comp.attributeBindings();
				}
				else if ( SingularAssociationAttributeBinding.class.isInstance(
						value
				) ) {
					SingularAssociationAttributeBinding toOne
							= SingularAssociationAttributeBinding.class.cast(
							value
					);
					EntityBinding referencedEntityBinding
							= toOne.getReferencedEntityBinding();
					SingularAttributeBinding referencedAttributeBinding
							= toOne.getReferencedAttributeBinding();
					try {
						parentPropIter = CompositeAttributeBinding.class.cast(
								referencedAttributeBinding
						).attributeBindings();
					}
					catch ( ClassCastException e ) {
						throw new org.hibernate.MappingException(
								"dotted notation reference neither a component nor a many/one to one", e
						);
					}
				}
				else {
					throw new org.hibernate.MappingException(
							"dotted notation reference neither a component nor a many/one to one"
					);
				}
				boolean hasFollowers = false;
				List<String> followers = new ArrayList<String>();
				for ( AttributeBinding binding : parentPropIter ) {
					String currentPropertyName
							= binding.getAttribute().getName();
					String currentName = reducedName + '.'
							+ currentPropertyName;
					if ( hasFollowers ) {
						followers.add( currentName );
					}
					if ( name.equals( currentName ) ) {
						hasFollowers = true;
					}
				}

				int index = propertyNames.size();
				int followersSize = followers.size();
				for ( int loop = 0; loop < followersSize; loop++ ) {
					String follower = followers.get( loop );
					int currentIndex = getIndexOfFirstMatchingProperty(
							propertyNames, follower
					);
					index = currentIndex != -1 && currentIndex < index
							? currentIndex : index;
				}
				propertyNames.add( index, name );
				properties.add( index, propertyElement );
			}
		}

		Map<String, String[]> results = new HashMap<String, String[]>( propertyResults.size() );
		return results;
	}



	private static int getIndexOfFirstMatchingProperty(List propertyNames,
													   String follower) {
		int propertySize = propertyNames.size();
		for ( int propIndex = 0; propIndex < propertySize; propIndex++ ) {
			if ( ( (String) propertyNames.get( propIndex ) ).startsWith(
					follower
			) ) {
				return propIndex;
			}
		}
		return -1;
	}
}
