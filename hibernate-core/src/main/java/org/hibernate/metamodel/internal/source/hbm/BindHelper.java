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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.cfg.HbmBinder;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryCollectionReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryJoinReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn;
import org.hibernate.engine.spi.NamedQueryDefinitionBuilder;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinitionBuilder;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jaxb.spi.Origin;
import org.hibernate.jaxb.spi.hbm.JaxbLoadCollectionElement;
import org.hibernate.jaxb.spi.hbm.JaxbQueryElement;
import org.hibernate.jaxb.spi.hbm.JaxbResultsetElement;
import org.hibernate.jaxb.spi.hbm.JaxbReturnElement;
import org.hibernate.jaxb.spi.hbm.JaxbReturnJoinElement;
import org.hibernate.jaxb.spi.hbm.JaxbReturnPropertyElement;
import org.hibernate.jaxb.spi.hbm.JaxbReturnScalarElement;
import org.hibernate.jaxb.spi.hbm.JaxbSqlQueryElement;
import org.hibernate.jaxb.spi.hbm.ReturnElement;
import org.hibernate.metamodel.internal.source.hbm.parser.BasicQueryElementContentParser;
import org.hibernate.metamodel.internal.source.hbm.parser.SQLQueryElementContentParser;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.SingularAssociationAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.BindingContext;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.type.Type;

/**
 * @author Brett Meyer
 */
// Pulled primarily from HibernateMappingProcessor
public class BindHelper {

	public static void bindNamedQuery(final JaxbQueryElement queryElement,
			MetadataImplementor metadata) {
		final NamedQueryDefinitionBuilder builder
				= new NamedQueryDefinitionBuilder();
		BasicQueryElementContentParser parser
				= new BasicQueryElementContentParser();
		parser.parse( builder, queryElement );
		metadata.addNamedQuery( builder.createNamedQueryDefinition() );
	}

	public static void bindNamedSQLQuery(final JaxbSqlQueryElement queryElement,
			Origin origin, LocalBindingContext bindingContext,
			MetadataImplementor metadata) {
		final NamedSQLQueryDefinitionBuilder builder
				= new NamedSQLQueryDefinitionBuilder();
		SQLQueryElementContentParser parser = new SQLQueryElementContentParser();
		parser.parse( builder, queryElement );

		final boolean callable = queryElement.isCallable();
		final String resultSetRef = queryElement.getResultsetRef();
		builder.setCallable( callable ).setResultSetRef( resultSetRef );

		NamedSQLQueryDefinition namedQuery = null;
		if ( StringHelper.isNotEmpty( resultSetRef ) ) {
			namedQuery = builder.createNamedQueryDefinition();
		}
		else {
			namedQuery = parser.buildQueryReturns( queryElement.getName(),
					builder, origin, bindingContext, metadata );

		}
		metadata.addNamedNativeQuery( namedQuery );
	}

	public static NativeSQLQueryRootReturn bindReturn(
			JaxbReturnElement returnElement, int elementCount,
			Origin origin, MetadataImplementor metadata, LocalBindingContext bindingContext) {
		final String alias = getAlias( returnElement, elementCount );
		final String clazz = returnElement.getClazz();
		String entityName = returnElement.getEntityName();
		if ( StringHelper.isEmpty( clazz )
				&& StringHelper.isEmpty( entityName ) ) {
			throw bindingContext.makeMappingException(
					"<return alias='" + alias
							+ "'> must specify either a class or entity-name"
			);
		}
		final LockMode lockMode = Helper.interpretLockMode(
				returnElement.getLockMode(), origin
		);
		entityName = StringHelper.isNotEmpty( entityName ) ? entityName : bindingContext.qualifyClassName( clazz );
		final EntityBinding entityBinding = metadata.getEntityBinding(
				entityName
		);
//		if ( entityBinding == null ) {
//			throw bindingContext.makeMappingException( "Can't locate entitybinding" );
//		}
		return new NativeSQLQueryRootReturn(
				alias, entityName,
				bindPropertyResults( alias, returnElement, entityBinding ),
				lockMode
		);
	}

	public static void bindResultSetMappingDefinitions(
			JaxbResultsetElement element, Origin origin,
			LocalBindingContext bindingContext, MetadataImplementor metadata) {
		final ResultSetMappingDefinition definition
				= new ResultSetMappingDefinition( element.getName() );
		int cnt = 0;
		for ( final JaxbReturnScalarElement r : element.getReturnScalar() ) {
			String column = r.getColumn();
			String typeFromXML = r.getType();
			Type type = StringHelper.isNotEmpty( typeFromXML )
					? metadata.getTypeResolver().heuristicType( typeFromXML )
					: null;
			definition.addQueryReturn( new NativeSQLQueryScalarReturn(
					column, type ) );
		}
		for ( final JaxbReturnJoinElement r : element.getReturnJoin() ) {
			definition.addQueryReturn( bindReturnJoin( r, cnt++, origin ) );

		}
		for ( final JaxbLoadCollectionElement r
				: element.getLoadCollection() ) {
			definition.addQueryReturn( bindLoadCollection(
					r, cnt++, origin, bindingContext ) );

		}
		for ( final JaxbReturnElement r : element.getReturn() ) {
			definition.addQueryReturn( bindReturn(
					r, cnt++, origin, metadata, bindingContext ) );

		}
		metadata.addResultSetMapping( definition );
	}

	public static NativeSQLQueryReturn bindReturnJoin(
			JaxbReturnJoinElement returnJoinElement, int elementCount,
			Origin origin) {
		final String alias = getAlias( returnJoinElement, elementCount );
		final String roleAttribute = returnJoinElement.getProperty();
		final LockMode lockMode = Helper.interpretLockMode(
				returnJoinElement.getLockMode(), origin );
		int dot = roleAttribute.lastIndexOf( '.' );
		if ( dot == -1 ) {
			throw new MappingException( "Role attribute for sql query return "
							+ "[alias=" + alias + "] not formatted correctly "
							+ "{owningAlias.propertyName}",
					origin );
		}
		final String roleOwnerAlias = roleAttribute.substring( 0, dot );
		final String roleProperty = roleAttribute.substring( dot + 1 );
		return new NativeSQLQueryJoinReturn( alias, roleOwnerAlias,
				roleProperty, bindPropertyResults( alias, returnJoinElement,
						null, origin ), lockMode );
	}

	public static NativeSQLQueryReturn bindLoadCollection(
			JaxbLoadCollectionElement returnElement, int elementCount,
			Origin origin, BindingContext bindingContext) {
		final String alias = getAlias( returnElement, elementCount );
		final String collectionAttribute = returnElement.getRole();
		final LockMode lockMode = Helper.interpretLockMode(
				returnElement.getLockMode(), origin );
		int dot = collectionAttribute.lastIndexOf( '.' );
		if ( dot == -1 ) {
			throw new MappingException( "Collection attribute for sql query return [alias=" + alias
							+ "] not formatted correctly {OwnerClassName.propertyName}",
					origin );
		}
		final String ownerClassName = HbmBinder.getClassName(
				collectionAttribute.substring( 0, dot ), bindingContext
				.getMappingDefaults().getPackageName() );
		final String ownerPropertyName = collectionAttribute.substring(
				dot + 1 );
		return new NativeSQLQueryCollectionReturn( alias, ownerClassName,
				ownerPropertyName, bindPropertyResults(
						alias, returnElement, null ), lockMode );
	}

	private static String getAlias(ReturnElement element, int elementCount) {
		return StringHelper.isEmpty( element.getAlias() ) ? "alias_"
				+ elementCount : element.getAlias();
	}


	private static Map bindPropertyResults(String alias,
			JaxbReturnJoinElement returnJoinElement,
			EntityBinding entityBinding,
			Origin origin) {
		HashMap<String, String[]> propertyresults
				= new HashMap<String, String[]>();
		returnJoinElement.getReturnProperty();


		return propertyresults.isEmpty() ? Collections.EMPTY_MAP
				: propertyresults;
	}

	// and org.hibernate.cfg.ResultSetMappingBinder.bindPropertyResults()
	private static Map<String, String[]> bindPropertyResults(String alias,
			JaxbReturnElement returnElement, EntityBinding entityBinding) {
		HashMap<String, String[]> propertyresults
				= new HashMap<String, String[]>();
		JaxbReturnElement.JaxbReturnDiscriminator discriminator
				= returnElement.getReturnDiscriminator();
		if ( discriminator != null && StringHelper.isNotEmpty(
				discriminator.getColumn() ) ) {
			String discriminatorColumn = StringHelper.unquote(
					discriminator.getColumn() );
			propertyresults.put( "class", new String[] { discriminatorColumn } );
		}
		List<JaxbReturnPropertyElement> returnPropertyElements
				= returnElement.getReturnProperty();

		return propertyresults.isEmpty() ? Collections.<String, String[]>emptyMap()
				: propertyresults;
	}

	private static Map<String, String[]> bindPropertyResults(
			String alias, JaxbLoadCollectionElement element,
			EntityBinding entityBinding) {
		List<JaxbReturnPropertyElement> returnPropertyElements
				= element.getReturnProperty();
		List<JaxbReturnPropertyElement> properties
				= new ArrayList<JaxbReturnPropertyElement>();
		List<String> propertyNames = new ArrayList<String>();
		HashMap propertyresults = new HashMap();
		for ( JaxbReturnPropertyElement propertyElement
				: returnPropertyElements ) {
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
							"dotted notation in <return-join> or <load_collection> not yet supported" );
				}
				int dotIndex = name.lastIndexOf( '.' );
				String reducedName = name.substring( 0, dotIndex );
				AttributeBinding value = getRecursiveAttributeBinding(
						entityBinding, reducedName );
				Iterable<AttributeBinding> parentPropIter;
				if ( CompositeAttributeBinding.class.isInstance( value ) ) {
					CompositeAttributeBinding comp
							= (CompositeAttributeBinding) value;
					parentPropIter = comp.attributeBindings();
				}
				else if ( SingularAssociationAttributeBinding.class.isInstance(
						value ) ) {
					SingularAssociationAttributeBinding toOne
							= SingularAssociationAttributeBinding.class.cast(
									value );
					EntityBinding referencedEntityBinding
							= toOne.getReferencedEntityBinding();
					SingularAttributeBinding referencedAttributeBinding
							= toOne.getReferencedAttributeBinding();
					try {
						parentPropIter = CompositeAttributeBinding.class.cast(
								referencedAttributeBinding ).attributeBindings();
					}
					catch ( ClassCastException e ) {
						throw new org.hibernate.MappingException(
								"dotted notation reference neither a component nor a many/one to one", e );
					}
				}
				else {
					throw new org.hibernate.MappingException(
							"dotted notation reference neither a component nor a many/one to one" );
				}
				boolean hasFollowers = false;
				List followers = new ArrayList();
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
					String follower = (String) followers.get( loop );
					int currentIndex = getIndexOfFirstMatchingProperty(
							propertyNames, follower );
					index = currentIndex != -1 && currentIndex < index
							? currentIndex : index;
				}
				propertyNames.add( index, name );
				properties.add( index, propertyElement );
			}
		}
		Set<String> uniqueReturnProperty = new HashSet<String>();
		for ( JaxbReturnPropertyElement propertyElement : properties ) {
			final String name = propertyElement.getName();
			if ( "class".equals( name ) ) {
				throw new org.hibernate.MappingException(
						"class is not a valid property name to use in a <return-property>, use <return-discriminator> instead" );
			}
			// TODO: validate existing of property with the chosen name. (secondpass )
			ArrayList<String> allResultColumns = getResultColumns( propertyElement );

			if ( allResultColumns.isEmpty() ) {
				throw new org.hibernate.MappingException( "return-property for alias " + alias
						+ " must specify at least one column or return-column name" );
			}
			if ( uniqueReturnProperty.contains( name ) ) {
				throw new org.hibernate.MappingException( "duplicate return-property for property " + name
						+ " on alias " + alias );
			}
			String key = name;
			ArrayList intermediateResults
					= (ArrayList) propertyresults.get( key );
			if ( intermediateResults == null ) {
				propertyresults.put( key, allResultColumns );
			}
			else {
				intermediateResults.addAll( allResultColumns );
			}
		}
		Iterator entries = propertyresults.entrySet().iterator();
		while ( entries.hasNext() ) {
			Map.Entry entry = (Map.Entry) entries.next();
			if ( entry.getValue() instanceof ArrayList ) {
				ArrayList list = (ArrayList) entry.getValue();
				entry.setValue( list.toArray( new String[list.size()] ) );
			}
		}
		return propertyresults.isEmpty() ? Collections.EMPTY_MAP
				: propertyresults;
	}

	private static AttributeBinding getRecursiveAttributeBinding(
			EntityBinding entityBinding, String propertyPath) {
//		Iterable<AttributeBinding> attributeBindings
//				= entityBinding.getAttributeBindingClosure();
//		StringTokenizer st = new StringTokenizer( propertyPath, "." );
		AttributeBinding attributeBinding = null;
//		while ( st.hasMoreElements() ) {
//			String element = st.nextToken();
//			for ( AttributeBinding binding : attributeBindings ) {
//
//			}
//		}
		return attributeBinding;

	}

	private static ArrayList<String> getResultColumns(
			JaxbReturnPropertyElement propertyresult) {
		String column = StringHelper.unquote( propertyresult.getColumn() );
		ArrayList<String> allResultColumns = new ArrayList<String>();
		if ( column != null ) {
			allResultColumns.add( column );
		}
		List<JaxbReturnPropertyElement.JaxbReturnColumn> resultColumns
				= propertyresult.getReturnColumn();
		for ( JaxbReturnPropertyElement.JaxbReturnColumn column1
				: resultColumns ) {
			allResultColumns.add( StringHelper.unquote( column1.getName() ) );
		}
		return allResultColumns;
	}

	private static int getIndexOfFirstMatchingProperty(List propertyNames,
			String follower) {
		int propertySize = propertyNames.size();
		for ( int propIndex = 0; propIndex < propertySize; propIndex++ ) {
			if ( ( (String) propertyNames.get( propIndex ) ).startsWith(
					follower ) ) {
				return propIndex;
			}
		}
		return -1;
	}
}
