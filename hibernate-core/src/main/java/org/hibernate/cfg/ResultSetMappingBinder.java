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
package org.hibernate.cfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryCollectionReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryJoinReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.type.Type;

import org.dom4j.Element;

/**
 * @author Emmanuel Bernard
 */
public abstract class ResultSetMappingBinder {
	/**
	 * Build a ResultSetMappingDefinition given a containing element for the "return-XXX" elements
	 *
	 * @param resultSetElem The element containing the return definitions.
	 * @param path No clue...
	 * @param mappings The current processing state.
	 * @return The description of the mappings...
	 */
	protected static ResultSetMappingDefinition buildResultSetMappingDefinition(Element resultSetElem, String path, Mappings mappings) {
		String resultSetName = resultSetElem.attribute( "name" ).getValue();
		if ( path != null ) {
			resultSetName = path + '.' + resultSetName;
		}
		ResultSetMappingDefinition definition = new ResultSetMappingDefinition( resultSetName );

		int cnt = 0;
		Iterator returns = resultSetElem.elementIterator();
		while ( returns.hasNext() ) {
			cnt++;
			Element returnElem = (Element) returns.next();
			String name = returnElem.getName();
			if ( "return-scalar".equals( name ) ) {
				String column = returnElem.attributeValue( "column" );
				String typeFromXML = HbmBinder.getTypeFromXML( returnElem );
				Type type = null;
				if(typeFromXML!=null) {
					type = mappings.getTypeResolver().heuristicType( typeFromXML );
					if ( type == null ) {
						throw new MappingException( "could not determine type " + type );
					}
				}
				definition.addQueryReturn( new NativeSQLQueryScalarReturn( column, type ) );
			}
			else if ( "return".equals( name ) ) {
				definition.addQueryReturn( bindReturn( returnElem, mappings, cnt ) );
			}
			else if ( "return-join".equals( name ) ) {
				definition.addQueryReturn( bindReturnJoin( returnElem, mappings ) );
			}
			else if ( "load-collection".equals( name ) ) {
				definition.addQueryReturn( bindLoadCollection( returnElem, mappings ) );
			}
		}
		return definition;
	}

	private static NativeSQLQueryRootReturn bindReturn(Element returnElem, Mappings mappings, int elementCount) {
		String alias = returnElem.attributeValue( "alias" );
		if( StringHelper.isEmpty( alias )) {
			alias = "alias_" + elementCount; // hack/workaround as sqlquery impl depend on having a key.
		}

		String entityName = HbmBinder.getEntityName(returnElem, mappings);
		if(entityName==null) {
			throw new MappingException( "<return alias='" + alias + "'> must specify either a class or entity-name");
		}
		LockMode lockMode = getLockMode( returnElem.attributeValue( "lock-mode" ) );

		PersistentClass pc = mappings.getClass( entityName );
		java.util.Map propertyResults = bindPropertyResults(alias, returnElem, pc, mappings );

		return new NativeSQLQueryRootReturn(
				alias,
				entityName,
				propertyResults,
				lockMode
			);
	}

	private static NativeSQLQueryJoinReturn bindReturnJoin(Element returnElem, Mappings mappings) {
		String alias = returnElem.attributeValue( "alias" );
		String roleAttribute = returnElem.attributeValue( "property" );
		LockMode lockMode = getLockMode( returnElem.attributeValue( "lock-mode" ) );
		int dot = roleAttribute.lastIndexOf( '.' );
		if ( dot == -1 ) {
			throw new MappingException(
					"Role attribute for sql query return [alias=" + alias +
					"] not formatted correctly {owningAlias.propertyName}"
				);
		}
		String roleOwnerAlias = roleAttribute.substring( 0, dot );
		String roleProperty = roleAttribute.substring( dot + 1 );

		//FIXME: get the PersistentClass
		java.util.Map propertyResults = bindPropertyResults(alias, returnElem, null, mappings );

		return new NativeSQLQueryJoinReturn(
				alias,
				roleOwnerAlias,
				roleProperty,
				propertyResults, // TODO: bindpropertyresults(alias, returnElem)
				lockMode
			);
	}

	private static NativeSQLQueryCollectionReturn bindLoadCollection(Element returnElem, Mappings mappings) {
		String alias = returnElem.attributeValue( "alias" );
		String collectionAttribute = returnElem.attributeValue( "role" );
		LockMode lockMode = getLockMode( returnElem.attributeValue( "lock-mode" ) );
		int dot = collectionAttribute.lastIndexOf( '.' );
		if ( dot == -1 ) {
			throw new MappingException(
					"Collection attribute for sql query return [alias=" + alias +
					"] not formatted correctly {OwnerClassName.propertyName}"
				);
		}
		String ownerClassName = HbmBinder.getClassName( collectionAttribute.substring( 0, dot ), mappings );
		String ownerPropertyName = collectionAttribute.substring( dot + 1 );

		//FIXME: get the PersistentClass
		java.util.Map propertyResults = bindPropertyResults(alias, returnElem, null, mappings );

		return new NativeSQLQueryCollectionReturn(
				alias,
				ownerClassName,
				ownerPropertyName,
				propertyResults,
				lockMode
			);
	}

	private static java.util.Map bindPropertyResults(
			String alias, Element returnElement, PersistentClass pc, Mappings mappings
	) {

		HashMap propertyresults = new HashMap(); // maybe a concrete SQLpropertyresult type, but Map is exactly what is required at the moment

		Element discriminatorResult = returnElement.element("return-discriminator");
		if(discriminatorResult!=null) {
			ArrayList resultColumns = getResultColumns(discriminatorResult);
			propertyresults.put("class", ArrayHelper.toStringArray( resultColumns ) );
		}
		Iterator iterator = returnElement.elementIterator("return-property");
		List properties = new ArrayList();
		List propertyNames = new ArrayList();
		while ( iterator.hasNext() ) {
			Element propertyresult = (Element) iterator.next();
			String name = propertyresult.attributeValue("name");
			if ( pc == null || name.indexOf( '.') == -1) { //if dotted and not load-collection nor return-join
				//regular property
				properties.add(propertyresult);
				propertyNames.add(name);
			}
			else {
				/**
				 * Reorder properties
				 * 1. get the parent property
				 * 2. list all the properties following the expected one in the parent property
				 * 3. calculate the lowest index and insert the property
				 */
				if (pc == null)
					throw new MappingException("dotted notation in <return-join> or <load_collection> not yet supported");
				int dotIndex = name.lastIndexOf( '.' );
				String reducedName = name.substring( 0, dotIndex );
				Value value = pc.getRecursiveProperty( reducedName ).getValue();
				Iterator parentPropIter;
				if ( value instanceof Component ) {
					Component comp = (Component) value;
					parentPropIter = comp.getPropertyIterator();
				}
				else if ( value instanceof ToOne ) {
					ToOne toOne = (ToOne) value;
					PersistentClass referencedPc = mappings.getClass( toOne.getReferencedEntityName() );
					if ( toOne.getReferencedPropertyName() != null ) {
						try {
							parentPropIter = ( (Component) referencedPc.getRecursiveProperty( toOne.getReferencedPropertyName() ).getValue() ).getPropertyIterator();
						} catch (ClassCastException e) {
							throw new MappingException("dotted notation reference neither a component nor a many/one to one", e);
						}
					}
					else {
						try {
							if ( referencedPc.getIdentifierMapper() == null ) {
								parentPropIter = ( (Component) referencedPc.getIdentifierProperty().getValue() ).getPropertyIterator();
							}
							else {
								parentPropIter = referencedPc.getIdentifierMapper().getPropertyIterator();
							}
						}
						catch (ClassCastException e) {
							throw new MappingException("dotted notation reference neither a component nor a many/one to one", e);
						}
					}
				}
				else {
					throw new MappingException("dotted notation reference neither a component nor a many/one to one");
				}
				boolean hasFollowers = false;
				List followers = new ArrayList();
				while ( parentPropIter.hasNext() ) {
					String currentPropertyName = ( (Property) parentPropIter.next() ).getName();
					String currentName = reducedName + '.' + currentPropertyName;
					if (hasFollowers) {
						followers.add( currentName );
					}
					if ( name.equals( currentName ) ) hasFollowers = true;
				}

				int index = propertyNames.size();
				int followersSize = followers.size();
				for (int loop = 0 ; loop < followersSize ; loop++) {
					String follower = (String) followers.get(loop);
					int currentIndex = getIndexOfFirstMatchingProperty(propertyNames, follower);
					index = currentIndex != -1 && currentIndex < index ? currentIndex : index;
				}
				propertyNames.add(index, name);
				properties.add(index, propertyresult);
			}
		}

		Set uniqueReturnProperty = new HashSet();
		iterator = properties.iterator();
		while ( iterator.hasNext() ) {
			Element propertyresult = (Element) iterator.next();
			String name = propertyresult.attributeValue("name");
			if ( "class".equals(name) ) {
				throw new MappingException(
						"class is not a valid property name to use in a <return-property>, use <return-discriminator> instead"
					);
			}
			//TODO: validate existing of property with the chosen name. (secondpass )
			ArrayList allResultColumns = getResultColumns(propertyresult);

			if ( allResultColumns.isEmpty() ) {
				throw new MappingException(
						"return-property for alias " + alias +
						" must specify at least one column or return-column name"
					);
			}
			if ( uniqueReturnProperty.contains( name ) ) {
				throw new MappingException(
						"duplicate return-property for property " + name +
						" on alias " + alias
					);
			}
			uniqueReturnProperty.add(name);

			// the issue here is that for <return-join/> representing an entity collection,
			// the collection element values (the property values of the associated entity)
			// are represented as 'element.{propertyname}'.  Thus the StringHelper.root()
			// here puts everything under 'element' (which additionally has significant
			// meaning).  Probably what we need to do is to something like this instead:
			//      String root = StringHelper.root( name );
			//      String key = root; // by default
			//      if ( !root.equals( name ) ) {
			//	        // we had a dot
			//          if ( !root.equals( alias ) {
			//              // the root does not apply to the specific alias
			//              if ( "elements".equals( root ) {
			//                  // we specifically have a <return-join/> representing an entity collection
			//                  // and this <return-property/> is one of that entity's properties
			//                  key = name;
			//              }
			//          }
			//      }
			// but I am not clear enough on the intended purpose of this code block, especially
			// in relation to the "Reorder properties" code block above...
//			String key = StringHelper.root( name );
			String key = name;
			ArrayList intermediateResults = (ArrayList) propertyresults.get( key );
			if (intermediateResults == null) {
				propertyresults.put( key, allResultColumns );
			}
			else {
				intermediateResults.addAll( allResultColumns );
			}
		}

		Iterator entries = propertyresults.entrySet().iterator();
		while ( entries.hasNext() ) {
			Map.Entry entry = (Map.Entry) entries.next();
			if (entry.getValue() instanceof ArrayList) {
				ArrayList list = (ArrayList) entry.getValue();
				entry.setValue( list.toArray( new String[ list.size() ] ) );
			}
		}
		return propertyresults.isEmpty() ? Collections.EMPTY_MAP : propertyresults;
	}

	private static int getIndexOfFirstMatchingProperty(List propertyNames, String follower) {
		int propertySize = propertyNames.size();
		for (int propIndex = 0 ; propIndex < propertySize ; propIndex++) {
			if ( ( (String) propertyNames.get(propIndex) ).startsWith( follower ) ) {
				return propIndex;
			}
		}
		return -1;
	}

	private static ArrayList getResultColumns(Element propertyresult) {
		String column = unquote(propertyresult.attributeValue("column"));
		ArrayList allResultColumns = new ArrayList();
		if(column!=null) allResultColumns.add(column);
		Iterator resultColumns = propertyresult.elementIterator("return-column");
		while ( resultColumns.hasNext() ) {
			Element element = (Element) resultColumns.next();
			allResultColumns.add( unquote(element.attributeValue("name")) );
		}
		return allResultColumns;
	}

	private static String unquote(String name) {
		if (name!=null && name.charAt(0)=='`') {
			name=name.substring( 1, name.length()-1 );
		}
		return name;
	}

	private static LockMode getLockMode(String lockMode) {
		if ( lockMode == null || "read".equals( lockMode ) ) {
			return LockMode.READ;
		}
		else if ( "none".equals( lockMode ) ) {
			return LockMode.NONE;
		}
		else if ( "upgrade".equals( lockMode ) ) {
			return LockMode.UPGRADE;
		}
		else if ( "upgrade-nowait".equals( lockMode ) ) {
			return LockMode.UPGRADE_NOWAIT;
		}
		else if ( "upgrade-skiplocked".equals( lockMode )) {
			return LockMode.UPGRADE_SKIPLOCKED;
		}
		else if ( "write".equals( lockMode ) ) {
			return LockMode.WRITE;
		}
		else if ( "force".equals( lockMode ) ) {
			return LockMode.FORCE;
		}
		else if ( "optimistic".equals( lockMode ) ) {
			return LockMode.OPTIMISTIC;
		}
		else if ( "optimistic_force_increment".equals( lockMode ) ) {
			return LockMode.OPTIMISTIC_FORCE_INCREMENT;
		}
		else if ( "pessimistic_read".equals( lockMode ) ) {
			return LockMode.PESSIMISTIC_READ;
		}
		else if ( "pessimistic_write".equals( lockMode ) ) {
			return LockMode.PESSIMISTIC_WRITE;
		}
		else if ( "pessimistic_force_increment".equals( lockMode ) ) {
			return LockMode.PESSIMISTIC_FORCE_INCREMENT;
		}
		else {
			throw new MappingException( "unknown lockmode" );
		}
	}
}
