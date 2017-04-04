/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.util.HashMap;
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
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedQueryDefinitionBuilder;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinitionBuilder;
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
		if ( queryAnn == null ) return;
		if ( BinderHelper.isEmptyAnnotationValue( queryAnn.name() ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}
		//EJBQL Query
		QueryHintDefinition hints = new QueryHintDefinition( queryAnn.hints() );
		String queryName = queryAnn.query();
		NamedQueryDefinition queryDefinition = new NamedQueryDefinitionBuilder( queryAnn.name() )
				.setLockOptions( hints.determineLockOptions( queryAnn ) )
				.setQuery( queryName )
				.setCacheable( hints.getBoolean( queryName, QueryHints.CACHEABLE ) )
				.setCacheRegion( hints.getString( queryName, QueryHints.CACHE_REGION ) )
				.setTimeout( hints.getTimeout( queryName ) )
				.setFetchSize( hints.getInteger( queryName, QueryHints.FETCH_SIZE ) )
				.setFlushMode( hints.getFlushMode( queryName ) )
				.setCacheMode( hints.getCacheMode( queryName ) )
				.setReadOnly( hints.getBoolean( queryName, QueryHints.READ_ONLY ) )
				.setComment( hints.getString( queryName, QueryHints.COMMENT ) )
				.setParameterTypes( null )
				.createNamedQueryDefinition();

		if ( isDefault ) {
			context.getMetadataCollector().addDefaultQuery( queryDefinition );
		}
		else {
			context.getMetadataCollector().addNamedQuery( queryDefinition );
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding named query: %s => %s", queryDefinition.getName(), queryDefinition.getQueryString() );
		}
	}



	public static void bindNativeQuery(
			NamedNativeQuery queryAnn,
			MetadataBuildingContext context,
			boolean isDefault) {
		if ( queryAnn == null ) return;
		//ResultSetMappingDefinition mappingDefinition = mappings.getResultSetMapping( queryAnn.resultSetMapping() );
		if ( BinderHelper.isEmptyAnnotationValue( queryAnn.name() ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}
		String resultSetMapping = queryAnn.resultSetMapping();
		QueryHintDefinition hints = new QueryHintDefinition( queryAnn.hints() );
		String queryName = queryAnn.query();
		
		NamedSQLQueryDefinitionBuilder builder = new NamedSQLQueryDefinitionBuilder( queryAnn.name() )
				.setQuery( queryName )
				.setQuerySpaces( null )
				.setCacheable( hints.getBoolean( queryName, QueryHints.CACHEABLE ) )
				.setCacheRegion( hints.getString( queryName, QueryHints.CACHE_REGION ) )
				.setTimeout( hints.getTimeout( queryName ) )
				.setFetchSize( hints.getInteger( queryName, QueryHints.FETCH_SIZE ) )
				.setFlushMode( hints.getFlushMode( queryName ) )
				.setCacheMode( hints.getCacheMode( queryName ) )
				.setReadOnly( hints.getBoolean( queryName, QueryHints.READ_ONLY ) )
				.setComment( hints.getString( queryName, QueryHints.COMMENT ) )
				.setParameterTypes( null )
				.setCallable( hints.getBoolean( queryName, QueryHints.CALLABLE ) );
		
		if ( !BinderHelper.isEmptyAnnotationValue( resultSetMapping ) ) {
			//sql result set usage
			builder.setResultSetRef( resultSetMapping )
					.createNamedQueryDefinition();
		}
		else if ( !void.class.equals( queryAnn.resultClass() ) ) {
			//class mapping usage
			//FIXME should be done in a second pass due to entity name?
			final NativeSQLQueryRootReturn entityQueryReturn =
					new NativeSQLQueryRootReturn( "alias1", queryAnn.resultClass().getName(), new HashMap(), LockMode.READ );
			builder.setQueryReturns( new NativeSQLQueryReturn[] {entityQueryReturn} );
		}
		else {
			builder.setQueryReturns( new NativeSQLQueryReturn[0] );
		}
		
		NamedSQLQueryDefinition query = builder.createNamedQueryDefinition();
		
		if ( isDefault ) {
			context.getMetadataCollector().addDefaultNamedNativeQuery( query );
		}
		else {
			context.getMetadataCollector().addNamedNativeQuery( query );
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding named native query: %s => %s", queryAnn.name(), queryAnn.query() );
		}
	}

	public static void bindNativeQuery(
			org.hibernate.annotations.NamedNativeQuery queryAnn,
			MetadataBuildingContext context) {
		if ( queryAnn == null ) {
			return;
		}

		//ResultSetMappingDefinition mappingDefinition = mappings.getResultSetMapping( queryAnn.resultSetMapping() );
		if ( BinderHelper.isEmptyAnnotationValue( queryAnn.name() ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}

		NamedSQLQueryDefinition query;
		String resultSetMapping = queryAnn.resultSetMapping();
		if ( !BinderHelper.isEmptyAnnotationValue( resultSetMapping ) ) {
			//sql result set usage
			query = new NamedSQLQueryDefinitionBuilder().setName( queryAnn.name() )
					.setQuery( queryAnn.query() )
					.setResultSetRef( resultSetMapping )
					.setQuerySpaces( null )
					.setCacheable( queryAnn.cacheable() )
					.setCacheRegion(
							BinderHelper.isEmptyAnnotationValue( queryAnn.cacheRegion() ) ?
									null :
									queryAnn.cacheRegion()
					)
					.setTimeout( queryAnn.timeout() < 0 ? null : queryAnn.timeout() )
					.setFetchSize( queryAnn.fetchSize() < 0 ? null : queryAnn.fetchSize() )
					.setFlushMode( getFlushMode( queryAnn.flushMode() ) )
					.setCacheMode( getCacheMode( queryAnn.cacheMode() ) )
					.setReadOnly( queryAnn.readOnly() )
					.setComment( BinderHelper.isEmptyAnnotationValue( queryAnn.comment() ) ? null : queryAnn.comment() )
					.setParameterTypes( null )
					.setCallable( queryAnn.callable() )
					.createNamedQueryDefinition();
		}
		else if ( !void.class.equals( queryAnn.resultClass() ) ) {
			//class mapping usage
			//FIXME should be done in a second pass due to entity name?
			final NativeSQLQueryRootReturn entityQueryReturn =
					new NativeSQLQueryRootReturn( "alias1", queryAnn.resultClass().getName(), new HashMap(), LockMode.READ );
			query = new NamedSQLQueryDefinitionBuilder().setName( queryAnn.name() )
					.setQuery( queryAnn.query() )
					.setQueryReturns( new NativeSQLQueryReturn[] {entityQueryReturn} )
					.setQuerySpaces( null )
					.setCacheable( queryAnn.cacheable() )
					.setCacheRegion(
							BinderHelper.isEmptyAnnotationValue( queryAnn.cacheRegion() ) ?
									null :
									queryAnn.cacheRegion()
					)
					.setTimeout( queryAnn.timeout() < 0 ? null : queryAnn.timeout() )
					.setFetchSize( queryAnn.fetchSize() < 0 ? null : queryAnn.fetchSize() )
					.setFlushMode( getFlushMode( queryAnn.flushMode() ) )
					.setCacheMode( getCacheMode( queryAnn.cacheMode() ) )
					.setReadOnly( queryAnn.readOnly() )
					.setComment( BinderHelper.isEmptyAnnotationValue( queryAnn.comment() ) ? null : queryAnn.comment() )
					.setParameterTypes( null )
					.setCallable( queryAnn.callable() )
					.createNamedQueryDefinition();
		}
		else {
			throw new NotYetImplementedException( "Pure native scalar queries are not yet supported" );
		}
		context.getMetadataCollector().addNamedNativeQuery( query );
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding named native query: %s => %s", query.getName(), queryAnn.query() );
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

		FlushMode flushMode;
		flushMode = getFlushMode( queryAnn.flushMode() );

		NamedQueryDefinition query = new NamedQueryDefinitionBuilder().setName( queryAnn.name() )
				.setQuery( queryAnn.query() )
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
				.setComment( BinderHelper.isEmptyAnnotationValue( queryAnn.comment() ) ? null : queryAnn.comment() )
				.setParameterTypes( null )
				.createNamedQueryDefinition();

		context.getMetadataCollector().addNamedQuery( query );
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding named query: %s => %s", query.getName(), query.getQueryString() );
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
			MetadataBuildingContext context,
			boolean isDefault) {
		if ( ann == null ) {
			return;
		}

		for (SqlResultSetMapping rs : ann.value()) {
			//no need to handle inSecondPass
			context.getMetadataCollector().addSecondPass( new ResultsetMappingSecondPass( rs, context, true ) );
		}
	}

	public static void bindSqlResultSetMapping(
			SqlResultSetMapping ann,
			MetadataBuildingContext context,
			boolean isDefault) {
		//no need to handle inSecondPass
		context.getMetadataCollector().addSecondPass( new ResultsetMappingSecondPass( ann, context, isDefault ) );
	}


}
