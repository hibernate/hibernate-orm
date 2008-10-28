//$Id$
package org.hibernate.cfg.annotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.ColumnResult;
import javax.persistence.EntityResult;
import javax.persistence.FieldResult;
import javax.persistence.SqlResultSetMapping;

import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.ExtendedMappings;
import org.hibernate.cfg.QuerySecondPass;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.query.sql.NativeSQLQueryScalarReturn;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.util.CollectionHelper;
import org.hibernate.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class ResultsetMappingSecondPass implements QuerySecondPass {
	private Logger log = LoggerFactory.getLogger( ResultsetMappingSecondPass.class );
	private SqlResultSetMapping ann;
	private ExtendedMappings mappings;
	private boolean isDefault;

	public ResultsetMappingSecondPass(SqlResultSetMapping ann, ExtendedMappings mappings, boolean isDefault) {
		this.ann = ann;
		this.mappings = mappings;
		this.isDefault = isDefault;
	}

	public void doSecondPass(Map persistentClasses) throws MappingException {
		//TODO add parameters checkings
		if ( ann == null ) return;
		ResultSetMappingDefinition definition = new ResultSetMappingDefinition( ann.name() );
		log.info( "Binding resultset mapping: {}", definition.getName() );

		int entityAliasIndex = 0;

		for (EntityResult entity : ann.entities()) {
			//TODO parameterize lock mode?
			List properties = new ArrayList();
			List propertyNames = new ArrayList();
			Map propertyresults = new HashMap();
			for (FieldResult field : entity.fields()) {
				//use an ArrayList cause we might have several columns per root property
				String name = field.name();
				if ( name.indexOf( '.' ) == -1 ) {
					//regular property
					properties.add( field );
					propertyNames.add( name );
				}
				else {
					/**
					 * Reorder properties
					 * 1. get the parent property
					 * 2. list all the properties following the expected one in the parent property
					 * 3. calculate the lowest index and insert the property
					 */
					PersistentClass pc = mappings.getClass( entity.entityClass().getName() );
					if ( pc == null ) {
						throw new MappingException(
								"Entity not found " + entity.entityClass().getName()
										+ " in SqlResultsetMapping " + ann.name()
						);
					}
					int dotIndex = name.lastIndexOf( '.' );
					String reducedName = name.substring( 0, dotIndex );
					Iterator parentPropIter = getSubPropertyIterator( pc, reducedName );
					List followers = getFollowers( parentPropIter, reducedName, name );

					int index = propertyNames.size();
					int followersSize = followers.size();
					for (int loop = 0; loop < followersSize; loop++) {
						String follower = (String) followers.get( loop );
						int currentIndex = getIndexOfFirstMatchingProperty( propertyNames, follower );
						index = currentIndex != -1 && currentIndex < index ? currentIndex : index;
					}
					propertyNames.add( index, name );
					properties.add( index, field );
				}
			}

			Set uniqueReturnProperty = new HashSet();
			Iterator iterator = properties.iterator();
			while ( iterator.hasNext() ) {
				FieldResult propertyresult = (FieldResult) iterator.next();
				String name = propertyresult.name();
				if ( "class".equals( name ) ) {
					throw new MappingException(
							"class is not a valid property name to use in a @FieldResult, use @Entity(discriminatorColumn) instead"
					);
				}
				ArrayList allResultColumns = new ArrayList();
				allResultColumns.add( propertyresult.column() );

				if ( uniqueReturnProperty.contains( name ) ) {
					throw new MappingException(
							"duplicate @FieldResult for property " + name +
									" on @Entity " + entity.entityClass().getName() + " in " + ann.name()
					);
				}
				uniqueReturnProperty.add( name );
				String key = StringHelper.root( name );
				ArrayList intermediateResults = (ArrayList) propertyresults.get( key );
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

			if ( !BinderHelper.isDefault( entity.discriminatorColumn() ) ) {
				propertyresults.put( "class", new String[] { entity.discriminatorColumn() } );
			}

			propertyresults = propertyresults.isEmpty() ? CollectionHelper.EMPTY_MAP : propertyresults;
			NativeSQLQueryRootReturn result =
					new NativeSQLQueryRootReturn(
							"alias" + entityAliasIndex++, entity.entityClass().getName(), propertyresults, LockMode.READ
					);
			definition.addQueryReturn( result );
		}

		for (ColumnResult column : ann.columns()) {
			definition.addQueryReturn( new NativeSQLQueryScalarReturn( column.name(), null ) );
		}

		if ( isDefault ) {
			mappings.addDefaultResultSetMapping( definition );
		}
		else {
			mappings.addResultSetMapping( definition );
		}
	}

	private List getFollowers(Iterator parentPropIter, String reducedName, String name) {
		boolean hasFollowers = false;
		List followers = new ArrayList();
		while ( parentPropIter.hasNext() ) {
			String currentPropertyName = ( (Property) parentPropIter.next() ).getName();
			String currentName = reducedName + '.' + currentPropertyName;
			if ( hasFollowers ) {
				followers.add( currentName );
			}
			if ( name.equals( currentName ) ) hasFollowers = true;
		}
		return followers;
	}

	private Iterator getSubPropertyIterator(PersistentClass pc, String reducedName) {
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
					parentPropIter = ( (Component) referencedPc.getRecursiveProperty(
							toOne.getReferencedPropertyName()
					).getValue() ).getPropertyIterator();
				}
				catch (ClassCastException e) {
					throw new MappingException(
							"dotted notation reference neither a component nor a many/one to one", e
					);
				}
			}
			else {
				try {
					if ( referencedPc.getIdentifierMapper() == null ) {
						parentPropIter = ( (Component) referencedPc.getIdentifierProperty()
								.getValue() ).getPropertyIterator();
					}
					else {
						parentPropIter = referencedPc.getIdentifierMapper().getPropertyIterator();
					}
				}
				catch (ClassCastException e) {
					throw new MappingException(
							"dotted notation reference neither a component nor a many/one to one", e
					);
				}
			}
		}
		else {
			throw new MappingException( "dotted notation reference neither a component nor a many/one to one" );
		}
		return parentPropIter;
	}

	private static int getIndexOfFirstMatchingProperty(List propertyNames, String follower) {
		int propertySize = propertyNames.size();
		for (int propIndex = 0; propIndex < propertySize; propIndex++) {
			if ( ( (String) propertyNames.get( propIndex ) ).startsWith( follower ) ) {
				return propIndex;
			}
		}
		return -1;
	}
}
