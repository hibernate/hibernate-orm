/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.query.SqlResultSetMappingDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.PersistentClass;

import jakarta.persistence.SqlResultSetMapping;

/**
 * @author Emmanuel Bernard
 */
public class ResultSetMappingSecondPass implements QuerySecondPass {
//	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( ResultsetMappingSecondPass.class );

	private final SqlResultSetMapping annotation;
	private final MetadataBuildingContext context;
	private final boolean isDefault;

	public ResultSetMappingSecondPass(SqlResultSetMapping annotation, MetadataBuildingContext context, boolean isDefault) {
		this.annotation = annotation;
		this.context = context;
		this.isDefault = isDefault;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		if ( annotation == null ) {
			return;
		}

		final var mappingDefinition = SqlResultSetMappingDescriptor.from( annotation );

		if ( isDefault ) {
			context.getMetadataCollector().addDefaultResultSetMapping( mappingDefinition );
		}
		else {
			context.getMetadataCollector().addResultSetMapping( mappingDefinition );
		}

		//TODO add parameters checkings
//		if ( ann == null ) return;
//		ResultSetMappingDescriptor definition = new ResultSetMappingDescriptor( ann.name() );
//		LOG.tracef( "Binding result set mapping: %s", definition.getName() );
//
//		int entityAliasIndex = 0;
//
//		for (EntityResult entity : ann.entities()) {
//			//TODO parameterize lock mode?
//			List<FieldResult> properties = new ArrayList<FieldResult>();
//			List<String> propertyNames = new ArrayList<String>();
//			for (FieldResult field : entity.fields()) {
//				//use an ArrayList cause we might have several columns per root property
//				String name = field.name();
//				if ( name.indexOf( '.' ) == -1 ) {
//					//regular property
//					properties.add( field );
//					propertyNames.add( name );
//				}
//				else {
//					/**
//					 * Reorder properties
//					 * 1. get the parent property
//					 * 2. list all the properties following the expected one in the parent property
//					 * 3. calculate the lowest index and insert the property
//					 */
//					PersistentClass pc = context.getMetadataCollector().getEntityBinding(
//							entity.entityClass().getName()
//					);
//					if ( pc == null ) {
//						throw new MappingException(
//								String.format(
//										Locale.ENGLISH,
//										"Could not resolve entity [%s] referenced in SqlResultSetMapping [%s]",
//										entity.entityClass().getName(),
//										ann.name()
//								)
//						);
//					}
//					int dotIndex = name.lastIndexOf( '.' );
//					String reducedName = name.substring( 0, dotIndex );
//					Iterator parentPropItr = getSubPropertyIterator( pc, reducedName );
//					List<String> followers = getFollowers( parentPropItr, reducedName, name );
//
//					int index = propertyNames.size();
//					for ( String follower : followers ) {
//						int currentIndex = getIndexOfFirstMatchingProperty( propertyNames, follower );
//						index = currentIndex != -1 && currentIndex < index ? currentIndex : index;
//					}
//					propertyNames.add( index, name );
//					properties.add( index, field );
//				}
//			}
//
//			Set<String> uniqueReturnProperty = new HashSet<String>();
//			Map<String, ArrayList<String>> propertyResultsTmp = new HashMap<String, ArrayList<String>>();
//			for ( Object property : properties ) {
//				final FieldResult propertyresult = ( FieldResult ) property;
//				final String name = propertyresult.name();
//				if ( "class".equals( name ) ) {
//					throw new MappingException(
//							"class is not a valid property name to use in a @FieldResult, use @Entity(discriminatorColumn) instead"
//					);
//				}
//
//				if ( uniqueReturnProperty.contains( name ) ) {
//					throw new MappingException(
//							"duplicate @FieldResult for property " + name +
//									" on @Entity " + entity.entityClass().getName() + " in " + ann.name()
//					);
//				}
//				uniqueReturnProperty.add( name );
//
//				final String quotingNormalizedColumnName = normalizeColumnQuoting( propertyresult.column() );
//
//				String key = StringHelper.root( name );
//				ArrayList<String> intermediateResults = propertyResultsTmp.get( key );
//				if ( intermediateResults == null ) {
//					intermediateResults = new ArrayList<String>();
//					propertyResultsTmp.put( key, intermediateResults );
//				}
//				intermediateResults.add( quotingNormalizedColumnName );
//			}
//
//			Map<String, String[]> propertyResults = new HashMap<String,String[]>();
//			for ( Map.Entry<String, ArrayList<String>> entry : propertyResultsTmp.entrySet() ) {
//				propertyResults.put(
//						entry.getKey(),
//						entry.getValue().toArray( new String[ entry.getValue().size() ] )
//				);
//			}
//
//			if ( !BinderHelper.isEmptyAnnotationValue( entity.discriminatorColumn() ) ) {
//				final String quotingNormalizedName = normalizeColumnQuoting( entity.discriminatorColumn() );
//				propertyResults.put( "class", new String[] { quotingNormalizedName } );
//			}
//
//			if ( propertyResults.isEmpty() ) {
//				propertyResults = java.util.Collections.emptyMap();
//			}
//
//			NativeSQLQueryRootReturn result = new NativeSQLQueryRootReturn(
//					"alias" + entityAliasIndex++,
//					entity.entityClass().getName(),
//					propertyResults,
//					LockMode.READ
//			);
//			definition.addQueryReturn( result );
//		}
//
//		for ( ColumnResult column : ann.columns() ) {
//			definition.addQueryReturn(
//					new NativeSQLQueryScalarReturn(
//							normalizeColumnQuoting( column.name() ),
//							column.type() != null ? context.getMetadataCollector().getTypeResolver().heuristicType( column.type().getName() ) : null
//					)
//			);
//		}
//
//		for ( ConstructorResult constructorResult : ann.classes() ) {
//			List<NativeSQLQueryScalarReturn> columnReturns = new ArrayList<NativeSQLQueryScalarReturn>();
//			for ( ColumnResult columnResult : constructorResult.columns() ) {
//				columnReturns.add(
//						new NativeSQLQueryScalarReturn(
//								normalizeColumnQuoting( columnResult.name() ),
//								columnResult.type() != null ? context.getMetadataCollector().getTypeResolver().heuristicType( columnResult.type().getName() ) : null
//						)
//				);
//			}
//			definition.addQueryReturn(
//					new NativeSQLQueryConstructorReturn( constructorResult.targetClass(), columnReturns )
//			);
//		}
//
//		if ( isDefault ) {
//			context.getMetadataCollector().addDefaultResultSetMapping( definition );
//		}
//		else {
//			context.getMetadataCollector().addResultSetMapping( definition );
//		}
	}
//
//	private String normalizeColumnQuoting(String name) {
//		return context.getMetadataCollector().getDatabase().toIdentifier( name ).render();
//	}
//
//	private List<String> getFollowers(Iterator<Property> parentPropIter, String reducedName, String name) {
//		boolean hasFollowers = false;
//		List<String> followers = new ArrayList<>();
//		while ( parentPropIter.hasNext() ) {
//			String currentPropertyName = parentPropIter.next().getName();
//			String currentName = reducedName + '.' + currentPropertyName;
//			if ( hasFollowers ) {
//				followers.add( currentName );
//			}
//			if ( name.equals( currentName ) ) {
//				hasFollowers = true;
//			}
//		}
//		return followers;
//	}
//
//	private Iterator getSubPropertyIterator(PersistentClass pc, String reducedName) {
//		Value value = pc.getRecursiveProperty( reducedName ).getValue();
//		Iterator parentPropIter;
//		if ( value instanceof Component ) {
//			Component comp = (Component) value;
//			parentPropIter = comp.getPropertyIterator();
//		}
//		else if ( value instanceof ToOne ) {
//			ToOne toOne = (ToOne) value;
//			PersistentClass referencedPc = context.getMetadataCollector().getEntityBinding( toOne.getReferencedEntityName() );
//			if ( toOne.getReferencedPropertyName() != null ) {
//				try {
//					parentPropIter = ( (Component) referencedPc.getRecursiveProperty(
//							toOne.getReferencedPropertyName()
//					).getValue() ).getPropertyIterator();
//				}
//				catch (ClassCastException e) {
//					throw new MappingException(
//							"dotted notation references neither a component nor a many/one to one", e
//					);
//				}
//			}
//			else {
//				try {
//					if ( referencedPc.getIdentifierMapper() == null ) {
//						parentPropIter = ( (Component) referencedPc.getIdentifierProperty()
//								.getValue() ).getPropertyIterator();
//					}
//					else {
//						parentPropIter = referencedPc.getIdentifierMapper().getPropertyIterator();
//					}
//				}
//				catch (ClassCastException e) {
//					throw new MappingException(
//							"dotted notation references neither a component nor a many/one to one", e
//					);
//				}
//			}
//		}
//		else {
//			throw new MappingException( "dotted notation references neither a component nor a many/one to one" );
//		}
//		return parentPropIter;
//	}
//
//	private static int getIndexOfFirstMatchingProperty(List propertyNames, String follower) {
//		int propertySize = propertyNames.size();
//		for (int propIndex = 0; propIndex < propertySize; propIndex++) {
//			if ( ( (String) propertyNames.get( propIndex ) ).startsWith( follower ) ) {
//				return propIndex;
//			}
//		}
//		return -1;
//	}
}
