/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

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
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.annotations.CacheModeType;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.annotations.QueryHints;
import org.hibernate.boot.internal.NamedHqlQueryDefinitionImpl;
import org.hibernate.boot.internal.NamedNativeQueryDefinitionImpl;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.NamedHqlQueryDefinition;
import org.hibernate.boot.spi.NamedNativeQueryDefinition;
import org.hibernate.cfg.BinderHelper;
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
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}

		final String queryName = queryAnn.name();
		final String queryString = queryAnn.query();

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding named query: %s => %s", queryName, queryString );
		}

		final QueryHintDefinition hints = new QueryHintDefinition( queryName, queryAnn.hints() );

		final NamedHqlQueryDefinition queryMapping = new NamedHqlQueryDefinitionImpl.Builder( queryName )
				.setHqlString( queryString )
				.setCacheable( hints.getCacheability() )
				.setCacheMode( hints.getCacheMode() )
				.setCacheRegion( hints.getString( QueryHints.CACHE_REGION ) )
				.setTimeout( hints.getTimeout() )
				.setFetchSize( hints.getInteger( QueryHints.FETCH_SIZE ) )
				.setFlushMode( hints.getFlushMode() )
				.setReadOnly( hints.getBoolean( QueryHints.READ_ONLY ) )
				.setLockOptions( hints.determineLockOptions( queryAnn ) )
				.setComment( hints.getString( QueryHints.COMMENT ) )
				.build();

		if ( isDefault ) {
			context.getMetadataCollector().addDefaultQuery( queryMapping );
		}
		else {
			context.getMetadataCollector().addNamedQuery( queryMapping );
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
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}

		final String registrationName = queryAnn.name();
		final String queryString = queryAnn.query();

		final QueryHintDefinition hints = new QueryHintDefinition( registrationName, queryAnn.hints() );

		final String resultSetMappingName = queryAnn.resultSetMapping();
		final String resultSetMappingClassName = void.class.equals( queryAnn.resultClass() )
				? null
				: queryAnn.resultClass().getName();

		final NamedNativeQueryDefinition.Builder builder = new NamedNativeQueryDefinitionImpl.Builder( registrationName )
				.setSqlString( queryString )
				.setResultSetMappingName( resultSetMappingName )
				.setResultSetMappingClassName( resultSetMappingClassName )
				.setQuerySpaces( null )
				.setCacheable( hints.getCacheability() )
				.setCacheMode( hints.getCacheMode() )
				.setCacheRegion( hints.getString( QueryHints.CACHE_REGION ) )
				.setTimeout( hints.getTimeout() )
				.setFetchSize( hints.getInteger( QueryHints.FETCH_SIZE ) )
				.setFlushMode( hints.getFlushMode() )
				.setReadOnly( hints.getBoolean( QueryHints.READ_ONLY ) )
				.setComment( hints.getString( QueryHints.COMMENT ) )
				.addHints( hints.getHintsMap() );


		final NamedNativeQueryDefinition queryDefinition = builder.build();

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding named native query: %s => %s", queryDefinition.getRegistrationName(), queryDefinition.getSqlQueryString() );
		}

		if ( isDefault ) {
			context.getMetadataCollector().addDefaultNamedNativeQuery( queryDefinition );
		}
		else {
			context.getMetadataCollector().addNamedNativeQuery( queryDefinition );
		}
	}

	public static void bindNativeQuery(
			org.hibernate.annotations.NamedNativeQuery queryAnn,
			MetadataBuildingContext context) {
		if ( queryAnn == null ) {
			return;
		}

		final String registrationName = queryAnn.name();

		//ResultSetMappingDefinition mappingDefinition = mappings.getJdbcValuesMappingProducer( queryAnn.resultSetMapping() );
		if ( BinderHelper.isEmptyAnnotationValue( registrationName ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}

		final String resultSetMappingName = queryAnn.resultSetMapping();
		final String resultSetMappingClassName = void.class.equals( queryAnn.resultClass() )
				? null
				: queryAnn.resultClass().getName();

		final NamedNativeQueryDefinition.Builder builder = new NamedNativeQueryDefinitionImpl.Builder( registrationName )
				.setSqlString( queryAnn.query() )
				.setResultSetMappingName( resultSetMappingName )
				.setResultSetMappingClassName( resultSetMappingClassName )
				.setQuerySpaces( null )
				.setCacheable( queryAnn.cacheable() )
				.setCacheRegion( BinderHelper.getAnnotationValueStringOrNull( queryAnn.cacheRegion() ) )
				.setCacheMode( getCacheMode( queryAnn.cacheMode() ) )
				.setTimeout( queryAnn.timeout() < 0 ? null : queryAnn.timeout() )
				.setFetchSize( queryAnn.fetchSize() < 0 ? null : queryAnn.fetchSize() )
				.setFlushMode( getFlushMode( queryAnn.flushMode() ) )
				.setReadOnly( queryAnn.readOnly() )
				.setComment( BinderHelper.getAnnotationValueStringOrNull( queryAnn.comment() ) );

		final NamedNativeQueryDefinition queryDefinition = builder.build();

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding named native query: %s => %s", queryDefinition.getRegistrationName(), queryDefinition.getSqlQueryString() );
		}

		context.getMetadataCollector().addNamedNativeQuery( queryDefinition );

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

		final String registrationName = queryAnn.name();

		//ResultSetMappingDefinition mappingDefinition = mappings.getJdbcValuesMappingProducer( queryAnn.resultSetMapping() );
		if ( BinderHelper.isEmptyAnnotationValue( registrationName ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}


		final NamedHqlQueryDefinition.Builder builder = new NamedHqlQueryDefinition.Builder( registrationName )
				.setHqlString( queryAnn.query() )
				.setCacheable( queryAnn.cacheable() )
				.setCacheRegion( BinderHelper.getAnnotationValueStringOrNull( queryAnn.cacheRegion() ) )
				.setCacheMode( getCacheMode( queryAnn.cacheMode() ) )
				.setTimeout( queryAnn.timeout() < 0 ? null : queryAnn.timeout() )
				.setFetchSize( queryAnn.fetchSize() < 0 ? null : queryAnn.fetchSize() )
				.setFlushMode( getFlushMode( queryAnn.flushMode() ) )
				.setReadOnly( queryAnn.readOnly() )
				.setComment( BinderHelper.isEmptyAnnotationValue( queryAnn.comment() ) ? null : queryAnn.comment() );

		final NamedHqlQueryDefinitionImpl hqlQueryDefinition = builder.build();

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding named query: %s => %s", hqlQueryDefinition.getRegistrationName(), hqlQueryDefinition.getHqlString() );
		}

		context.getMetadataCollector().addNamedQuery( hqlQueryDefinition );
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

		final String registrationName = annotation.name();

		if ( BinderHelper.isEmptyAnnotationValue( registrationName ) ) {
			throw new AnnotationException( "A named query must have a name when used in class or package level" );
		}

		throw new NotYetImplementedFor6Exception();
//		NamedProcedureCallDefinition.
//		final NamedProcedureCallDefinitionImpl def = new NamedProcedureCallDefinitionImpl( annotation );
//
//		if (isDefault) {
//			context.getMetadataCollector().addDefaultNamedProcedureCall( def );
//		}
//		else {
//			context.getMetadataCollector().addNamedProcedureCallDefinition( def );
//		}
//		LOG.debugf( "Bound named stored procedure query : %s => %s", def.getRegistrationName(), def.getProcedureName() );
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
