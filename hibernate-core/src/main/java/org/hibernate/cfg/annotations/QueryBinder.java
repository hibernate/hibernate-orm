/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.EntityResult;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.annotations.CacheModeType;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.annotations.QueryHints;
import org.hibernate.boot.model.query.internal.NamedHqlQueryDefinitionImpl;
import org.hibernate.boot.model.query.internal.NamedNativeQueryDefinitionImpl;
import org.hibernate.boot.model.query.spi.NamedHqlQueryDefinition;
import org.hibernate.boot.model.resultset.internal.EntityResultDefinitionImpl;
import org.hibernate.boot.model.resultset.internal.InstantiationResultDefinitionImpl;
import org.hibernate.boot.model.resultset.internal.ResultSetMappingDefinitionImpl;
import org.hibernate.boot.model.resultset.internal.ScalarResultDefinitionImpl;
import org.hibernate.boot.model.resultset.spi.ResultSetMappingDefinition;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * Query binder
 *
 * @author Emmanuel Bernard
 */
public abstract class QueryBinder {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, QueryBinder.class.getName());

	public static void bindQuery(
			NamedQuery queryAnn,
			MetadataBuildingContext context,
			boolean isDefault) {
		if ( queryAnn == null ) {
			return;
		}

		if ( BinderHelper.isEmptyAnnotationValue( queryAnn.name() ) ) {
			throw new AnnotationException( "A named HQL/JPQL query must have a name when used in class or package level" );
		}

		QueryHintDefinition hints = new QueryHintDefinition( queryAnn.hints() );
		final String queryName = queryAnn.name();
		final String hqlString = queryAnn.query();

		final NamedHqlQueryDefinition queryDefinition = new NamedHqlQueryDefinitionImpl.Builder( queryName, hqlString )
				.setLockOptions( hints.determineLockOptions( queryAnn ) )
				.setCacheable( hints.getBoolean( queryName, QueryHints.CACHEABLE ) )
				.setCacheRegion( hints.getString( queryName, QueryHints.CACHE_REGION ) )
				.setTimeout( hints.getTimeout( queryName ) )
				.setFetchSize( hints.getInteger( queryName, QueryHints.FETCH_SIZE ) )
				.setFlushMode( hints.getFlushMode( queryName ) )
				.setCacheMode( hints.getCacheMode( queryName ) )
				.setReadOnly( hints.getBoolean( queryName, QueryHints.READ_ONLY ) )
				.setComment( hints.getString( queryName, QueryHints.COMMENT ) )
				.build();

		if ( isDefault ) {
			context.getMetadataCollector().addDefaultNamedHqlQuery( queryDefinition );
		}
		else {
			context.getMetadataCollector().addNamedHqlQuery( queryDefinition );
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Generated and registered NamedHqlQueryDefinition : `%s` => `%s`", queryDefinition.getName(), queryDefinition.getQueryString() );
		}
	}



	public static void bindNativeQuery(
			NamedNativeQuery queryAnn,
			MetadataBuildingContext context,
			boolean isDefault) {
		if ( queryAnn == null ) {
			return;
		}

		if ( BinderHelper.isEmptyAnnotationValue( queryAnn.name() ) ) {
			throw new AnnotationException( "A named native query must have a name when used in class or package level" );
		}

		final String queryName = queryAnn.name();
		final String sqlString = queryAnn.query();

		QueryHintDefinition hints = new QueryHintDefinition( queryAnn.hints() );
		if ( hints.getBoolean( sqlString, QueryHints.CALLABLE ) ) {
			LOG.warnNativeQueryAsCallable();
		}

		NamedNativeQueryDefinitionImpl.Builder builder = new NamedNativeQueryDefinitionImpl.Builder( queryName )
				.setSqlString( sqlString )
				.setCacheable( hints.getBoolean( sqlString, QueryHints.CACHEABLE ) )
				.setCacheRegion( hints.getString( sqlString, QueryHints.CACHE_REGION ) )
				.setTimeout( hints.getTimeout( sqlString ) )
				.setFetchSize( hints.getInteger( sqlString, QueryHints.FETCH_SIZE ) )
				.setFlushMode( hints.getFlushMode( sqlString ) )
				.setCacheMode( hints.getCacheMode( sqlString ) )
				.setReadOnly( hints.getBoolean( sqlString, QueryHints.READ_ONLY ) )
				.setComment( hints.getString( sqlString, QueryHints.COMMENT ) );

		final String resultSetMappingName = queryAnn.resultSetMapping();
		if ( !BinderHelper.isEmptyAnnotationValue( resultSetMappingName ) ) {
			builder.setResultSetMapping( resultSetMappingName );
		}
		else if ( !void.class.equals( queryAnn.resultClass() ) ) {
			final ResultSetMappingDefinitionImpl inLineResultMapping = new ResultSetMappingDefinitionImpl(
					"inline-result-mapping:" + ++inlineResultMappingCount
			);

			final EntityResultDefinitionImpl entityResultDefinition = new EntityResultDefinitionImpl (
					null,
					queryAnn.resultClass().getName(),
					"alias1"
			);
			entityResultDefinition.setLockMode( LockMode.READ );

			inLineResultMapping.addResult( entityResultDefinition );

			context.getMetadataCollector().addResultSetMapping( inLineResultMapping );
			builder.setResultSetMapping( inLineResultMapping.getName() );
		}

		final NamedNativeQueryDefinitionImpl queryDefinition = builder.build();

		if ( isDefault ) {
			context.getMetadataCollector().addDefaultNamedNativeQuery( queryDefinition );
		}
		else {
			context.getMetadataCollector().addNamedNativeQuery( queryDefinition );
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Generated and registered NamedHqlQueryDefinition : `%s` => `%s`", queryDefinition.getName(), queryDefinition.getQueryString() );
		}
	}

	private static int inlineResultMappingCount = 0;

	public static void bindNativeQuery(
			org.hibernate.annotations.NamedNativeQuery queryAnn,
			MetadataBuildingContext context) {
		if ( queryAnn == null ) {
			return;
		}

		if ( BinderHelper.isEmptyAnnotationValue( queryAnn.name() ) ) {
			throw new AnnotationException( "A named native query must have a name when used in class or package level" );
		}

		final String queryName = queryAnn.name();
		final String sqlString = queryAnn.query();

		final NamedNativeQueryDefinitionImpl.Builder builder = new NamedNativeQueryDefinitionImpl.Builder( queryName )
				.setSqlString( sqlString )
				.setCacheable( queryAnn.cacheable() )
				.setCacheRegion(
						BinderHelper.isEmptyAnnotationValue( queryAnn.cacheRegion() )
								? null
								: queryAnn.cacheRegion()
				)
				.setTimeout( queryAnn.timeout() < 0 ? null : queryAnn.timeout() )
				.setFetchSize( queryAnn.fetchSize() < 0 ? null : queryAnn.fetchSize() )
				.setFlushMode( getFlushMode( queryAnn.flushMode() ) )
				.setCacheMode( getCacheMode( queryAnn.cacheMode() ) )
				.setReadOnly( queryAnn.readOnly() )
				.setComment( BinderHelper.isEmptyAnnotationValue( queryAnn.comment() ) ? null : queryAnn.comment() );

		final String resultSetMappingName = queryAnn.resultSetMapping();

		if ( !BinderHelper.isEmptyAnnotationValue( resultSetMappingName ) ) {
			builder.setResultSetMapping( resultSetMappingName );
		}
		else if ( !void.class.equals( queryAnn.resultClass() ) ) {
			final ResultSetMappingDefinitionImpl resultSetMappingDefinition = new ResultSetMappingDefinitionImpl(
					"inline-result-mapping:" + ++inlineResultMappingCount
			);

			final String entityClassName = queryAnn.resultClass().getName();
			resultSetMappingDefinition.addResult( new EntityResultDefinitionImpl( null, entityClassName, "alias1" ) );

			context.getMetadataCollector().addResultSetMapping( resultSetMappingDefinition );
			builder.setResultSetMapping( resultSetMappingDefinition.getName() );
		}
		else {
			throw new NotYetImplementedException( "Pure native scalar queries are not yet supported" );
		}

		context.getMetadataCollector().addNamedNativeQuery( builder.build() );

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding named native query: %s => %s", queryAnn.name(), queryAnn.query() );
		}
	}

	public static void bindQueries(NamedQueries queriesAnn, MetadataBuildingContext context, boolean isDefault) {
		if ( queriesAnn == null ) {
			return;
		}

		for (NamedQuery q : queriesAnn.value()) {
			bindQuery( q, context, isDefault );
		}
	}

	public static void bindNativeQueries(
			NamedNativeQueries queriesAnn,
			MetadataBuildingContext context,
			boolean isDefault) {
		if ( queriesAnn == null ) {
			return;
		}

		for (NamedNativeQuery q : queriesAnn.value()) {
			bindNativeQuery( q, context, isDefault );
		}
	}

	public static void bindNativeQueries(
			org.hibernate.annotations.NamedNativeQueries queriesAnn,
			MetadataBuildingContext context) {
		if ( queriesAnn == null ) {
			return;
		}

		for (org.hibernate.annotations.NamedNativeQuery q : queriesAnn.value()) {
			bindNativeQuery( q, context );
		}
	}

	public static void bindQuery(
			org.hibernate.annotations.NamedQuery queryAnn,
			MetadataBuildingContext context) {
		if ( queryAnn == null ) {
			return;
		}

		if ( BinderHelper.isEmptyAnnotationValue( queryAnn.name() ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}

		final String queryName = queryAnn.name();
		final String hqlString = queryAnn.query();

		FlushMode flushMode;
		flushMode = getFlushMode( queryAnn.flushMode() );

		final NamedHqlQueryDefinitionImpl.Builder builder = new NamedHqlQueryDefinitionImpl.Builder( queryName, hqlString )
				.setCacheable( queryAnn.cacheable() )
				.setCacheRegion(
						BinderHelper.isEmptyAnnotationValue( queryAnn.cacheRegion() ) ?
								null :
								queryAnn.cacheRegion()
				)
				.setTimeout( queryAnn.timeout() < 0 ? null : queryAnn.timeout() )
				.setFetchSize( queryAnn.fetchSize() < 0 ? null : queryAnn.fetchSize() )
				.setFlushMode( flushMode )
				.setCacheMode( getCacheMode( queryAnn.cacheMode() ) )
				.setReadOnly( queryAnn.readOnly() )
				.setComment( BinderHelper.isEmptyAnnotationValue( queryAnn.comment() ) ? null : queryAnn.comment() );

		context.getMetadataCollector().addNamedHqlQuery( builder.build() );

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding named HQL/JPQL query: %s => %s", queryName, hqlString );
		}
	}

	private static FlushMode getFlushMode(FlushModeType flushModeType) {
		FlushMode flushMode;
		switch ( flushModeType ) {
			case ALWAYS:
				flushMode = FlushMode.ALWAYS;
				break;
			case AUTO:
				flushMode = FlushMode.AUTO;
				break;
			case COMMIT:
				flushMode = FlushMode.COMMIT;
				break;
			case NEVER:
				flushMode = FlushMode.MANUAL;
				break;
			case MANUAL:
				flushMode = FlushMode.MANUAL;
				break;
			case PERSISTENCE_CONTEXT:
				flushMode = null;
				break;
			default:
				throw new AssertionFailure( "Unknown flushModeType: " + flushModeType );
		}
		return flushMode;
	}

	private static CacheMode getCacheMode(CacheModeType cacheModeType) {
		switch ( cacheModeType ) {
			case GET:
				return CacheMode.GET;
			case IGNORE:
				return CacheMode.IGNORE;
			case NORMAL:
				return CacheMode.NORMAL;
			case PUT:
				return CacheMode.PUT;
			case REFRESH:
				return CacheMode.REFRESH;
			default:
				throw new AssertionFailure( "Unknown cacheModeType: " + cacheModeType );
		}
	}


	public static void bindQueries(
			org.hibernate.annotations.NamedQueries queriesAnn,
			MetadataBuildingContext context) {
		if ( queriesAnn == null ) {
			return;
		}

		for (org.hibernate.annotations.NamedQuery q : queriesAnn.value()) {
			bindQuery( q, context );
		}
	}

	public static void bindNamedStoredProcedureQuery(
			NamedStoredProcedureQuery annotation,
			MetadataBuildingContext context,
			boolean isDefault) {
		if ( annotation == null ) {
			return;
		}

		if ( BinderHelper.isEmptyAnnotationValue( annotation.name() ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}

		final NamedProcedureCallDefinition def = new NamedProcedureCallDefinition( annotation );

		if (isDefault) {
			context.getMetadataCollector().addDefaultNamedProcedureCallDefinition( def );
		}
		else {
			context.getMetadataCollector().addNamedProcedureCallDefinition( def );
		}
		LOG.debugf( "Bound named stored procedure query : %s => %s", def.getRegisteredName(), def.getProcedureName() );
	}

	public static void bindSqlResultSetMappings(
			SqlResultSetMappings ann,
			MetadataBuildingContext context) {
		if ( ann == null ) {
			return;
		}

		for (SqlResultSetMapping rs : ann.value()) {
			bindSqlResultSetMapping( rs, context, true );
		}
	}

	public static void bindSqlResultSetMapping(
			SqlResultSetMapping ann,
			MetadataBuildingContext context,
			boolean isDefault) {
		final ResultSetMappingDefinition resultSetMapping = extractMapping( ann, context );
		if ( isDefault ) {
			context.getMetadataCollector().addDefaultResultSetMapping( resultSetMapping );
		}
		else {
			context.getMetadataCollector().addResultSetMapping( resultSetMapping );
		}
	}

	private static ResultSetMappingDefinition extractMapping(
			SqlResultSetMapping ann,
			MetadataBuildingContext context) {
		final ResultSetMappingDefinitionImpl mapping = new ResultSetMappingDefinitionImpl( ann.name() );

		LOG.debugf( "Binding @SqlResultSetMapping( name=\"" + ann.name() + "\")" );

		int entityAliasIndex = 0;


		for ( EntityResult entity : ann.entities() ) {
			mapping.addResult( extractEntityResult( entity, entityAliasIndex, context )  );

		}

		for ( ColumnResult column : ann.columns() ) {
			mapping.addResult( extractScalarResult( column, context ) );
		}

		for ( ConstructorResult constructorResult : ann.classes() ) {
			mapping.addResult( extractInstantiationResult( constructorResult, context ) );
		}

		return mapping;
	}

	private static ResultSetMappingDefinition.InstantiationResult extractInstantiationResult(
			ConstructorResult constructorResult,
			MetadataBuildingContext context) {

		final InstantiationResultDefinitionImpl instantiation = new InstantiationResultDefinitionImpl(
				constructorResult.targetClass().getName()
		);

		for ( ColumnResult columnResult : constructorResult.columns() ) {
			instantiation.addArgument(
					new InstantiationResultDefinitionImpl.ArgumentImpl(
							extractScalarResult( columnResult, context ),
							columnResult.name()
					)
			);
		}

		return instantiation;
	}

	private static ResultSetMappingDefinition.Result extractEntityResult(
			EntityResult entity,
			int entityAliasIndex,
			MetadataBuildingContext context) {
		return new EntityResultDefinitionImpl(
				entity.entityClass().getName(),
				null,
				null
		);

//		List<FieldResult> properties = new ArrayList<>();
//		List<String> propertyNames = new ArrayList<>();
//		for (FieldResult field : entity.fields()) {
//			//use an ArrayList cause we might have several columns per root property
//			String name = field.name();
//			if ( name.indexOf( '.' ) == -1 ) {
//				//regular property
//				properties.add( field );
//				propertyNames.add( name );
//			}
//			else {
//				/**
//				 * Reorder properties
//				 * 1. get the parent property
//				 * 2. list all the properties following the expected one in the parent property
//				 * 3. calculate the lowest index and insert the property
//				 */
//				PersistentClass pc = context.getMetadataCollector().getEntityBinding(
//						entity.entityClass().getName()
//				);
//				if ( pc == null ) {
//					throw new MappingException(
//							String.format(
//									Locale.ENGLISH,
//									"Could not resolve entity [%s] referenced in SqlResultSetMapping [%s]",
//									entity.entityClass().getName(),
//									ann.name()
//							)
//					);
//				}
//				int dotIndex = name.lastIndexOf( '.' );
//				String reducedName = name.substring( 0, dotIndex );
//				Iterator parentPropItr = getSubPropertyIterator( pc, reducedName );
//				List<String> followers = getFollowers( parentPropItr, reducedName, name );
//
//				int index = propertyNames.size();
//				for ( String follower : followers ) {
//					int currentIndex = getIndexOfFirstMatchingProperty( propertyNames, follower );
//					index = currentIndex != -1 && currentIndex < index ? currentIndex : index;
//				}
//				propertyNames.add( index, name );
//				properties.add( index, field );
//			}
//		}
//
//		Set<String> uniqueReturnProperty = new HashSet<String>();
//		Map<String, ArrayList<String>> propertyResultsTmp = new HashMap<String, ArrayList<String>>();
//		for ( Object property : properties ) {
//			final FieldResult propertyresult = ( FieldResult ) property;
//			final String name = propertyresult.name();
//			if ( "class".equals( name ) ) {
//				throw new MappingException(
//						"class is not a valid property name to use in a @FieldResult, use @Entity(discriminatorColumn) instead"
//				);
//			}
//
//			if ( uniqueReturnProperty.contains( name ) ) {
//				throw new MappingException(
//						"duplicate @FieldResult for property " + name +
//								" on @Entity " + entity.entityClass().getName() + " in " + ann.name()
//				);
//			}
//			uniqueReturnProperty.add( name );
//
//			final String quotingNormalizedColumnName = normalizeColumnQuoting( propertyresult.column() );
//
//			String key = StringHelper.root( name );
//			ArrayList<String> intermediateResults = propertyResultsTmp.get( key );
//			if ( intermediateResults == null ) {
//				intermediateResults = new ArrayList<String>();
//				propertyResultsTmp.put( key, intermediateResults );
//			}
//			intermediateResults.add( quotingNormalizedColumnName );
//		}
//
//		Map<String, String[]> propertyResults = new HashMap<String,String[]>();
//		for ( Map.Entry<String, ArrayList<String>> entry : propertyResultsTmp.entrySet() ) {
//			propertyResults.put(
//					entry.getKey(),
//					entry.getValue().toArray( new String[ entry.getValue().size() ] )
//			);
//		}
//
//		if ( !BinderHelper.isEmptyAnnotationValue( entity.discriminatorColumn() ) ) {
//			final String quotingNormalizedName = normalizeColumnQuoting( entity.discriminatorColumn() );
//			propertyResults.put( "class", new String[] { quotingNormalizedName } );
//		}
//
//		if ( propertyResults.isEmpty() ) {
//			propertyResults = java.util.Collections.emptyMap();
//		}
//
//		NativeSQLQueryRootReturn result = new NativeSQLQueryRootReturn(
//				"alias" + entityAliasIndex++,
//				entity.entityClass().getName(),
//				propertyResults,
//				LockMode.READ
//		);
//		definition.addResultBuilder( result );
	}

	private static ResultSetMappingDefinition.Result extractScalarResult(
			ColumnResult column,
			MetadataBuildingContext context) {
		return new ScalarResultDefinitionImpl(
				column.name(),
				column.type().getName()
		);
	}

}
