/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.annotations.CacheModeType;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;
import org.hibernate.boot.internal.NamedHqlQueryDefinitionImpl;
import org.hibernate.boot.internal.NamedProcedureCallDefinitionImpl;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinitionBuilder;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.query.sql.internal.ParameterParser;
import org.hibernate.query.sql.spi.ParameterRecognizer;
import org.hibernate.type.BasicType;

import org.jboss.logging.Logger;

import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.StoredProcedureParameter;

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
				.setCacheRegion( hints.getString( HibernateHints.HINT_CACHE_REGION ) )
				.setTimeout( hints.getTimeout() )
				.setFetchSize( hints.getInteger( HibernateHints.HINT_FETCH_SIZE ) )
				.setFlushMode( hints.getFlushMode() )
				.setReadOnly( hints.getBoolean( HibernateHints.HINT_READ_ONLY ) )
				.setLockOptions( hints.determineLockOptions( queryAnn ) )
				.setComment( hints.getString( HibernateHints.HINT_COMMENT ) )
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

		final NamedNativeQueryDefinitionBuilder builder = new NamedNativeQueryDefinitionBuilder( registrationName )
				.setSqlString( queryString )
				.setResultSetMappingName( resultSetMappingName )
				.setResultSetMappingClassName( resultSetMappingClassName )
				.setQuerySpaces( null )
				.setCacheable( hints.getCacheability() )
				.setCacheMode( hints.getCacheMode() )
				.setCacheRegion( hints.getString( HibernateHints.HINT_CACHE_REGION ) )
				.setTimeout( hints.getTimeout() )
				.setFetchSize( hints.getInteger( HibernateHints.HINT_FETCH_SIZE ) )
				.setFlushMode( hints.getFlushMode() )
				.setReadOnly( hints.getBoolean( HibernateHints.HINT_READ_ONLY ) )
				.setComment( hints.getString( HibernateHints.HINT_COMMENT ) )
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

		final NamedNativeQueryDefinitionBuilder builder = new NamedNativeQueryDefinitionBuilder( registrationName )
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
				.setQuerySpaces( CollectionHelper.setOf( queryAnn.querySpaces() ) )
				.setComment( BinderHelper.getAnnotationValueStringOrNull( queryAnn.comment() ) );

		if ( queryAnn.callable() ) {
			final NamedProcedureCallDefinition definition = createStoredProcedure(
					builder, context,
					() -> illegalCallSyntax(
							queryAnn,
							queryAnn.query()
					)
			);
			context.getMetadataCollector().addNamedProcedureCallDefinition( definition );
			DeprecationLogger.DEPRECATION_LOGGER.warn(
					"Marking named native queries as callable is no longer supported; use `@jakarta.persistence.NamedStoredProcedureQuery` instead.  Ignoring."
			);
		}
		else {
			final NamedNativeQueryDefinition queryDefinition = builder.build();

			if ( LOG.isDebugEnabled() ) {
				LOG.debugf(
						"Binding named native query: %s => %s",
						queryDefinition.getRegistrationName(),
						queryDefinition.getSqlQueryString()
				);
			}

			context.getMetadataCollector().addNamedNativeQuery( queryDefinition );
		}

	}

	public static NamedProcedureCallDefinition createStoredProcedure(
			NamedNativeQueryDefinitionBuilder builder,
			MetadataBuildingContext context,
			Supplier<RuntimeException> exceptionProducer) {
		List<StoredProcedureParameter> storedProcedureParameters = new ArrayList<>();
		List<QueryHint> queryHints = new ArrayList<>();
		List<String> parameterNames = new ArrayList<>();
		final String sqlString = builder.getSqlString().trim();
		if ( !sqlString.startsWith( "{" ) || !sqlString.endsWith( "}" ) ) {
			throw exceptionProducer.get();
		}
		final String procedureName = QueryBinder.parseJdbcCall(
				sqlString,
				parameterNames,
				exceptionProducer
		);

		AnnotationDescriptor ann = new AnnotationDescriptor( NamedStoredProcedureQuery.class );
		ann.setValue( "name", builder.getName() );
		ann.setValue( "procedureName", procedureName );

		for ( String parameterName : parameterNames ) {
			AnnotationDescriptor parameterDescriptor = new AnnotationDescriptor( StoredProcedureParameter.class );
			parameterDescriptor.setValue( "name", parameterName );
			parameterDescriptor.setValue( "mode", ParameterMode.IN );
			final String typeName = builder.getParameterTypes().get( parameterName );
			if ( typeName == null ) {
				parameterDescriptor.setValue( "type", Object.class );
			}
			else {
				final BasicType<Object> registeredType = context.getBootstrapContext()
						.getTypeConfiguration()
						.getBasicTypeRegistry()
						.getRegisteredType( typeName );
				parameterDescriptor.setValue( "type", registeredType.getJavaType() );
			}
			storedProcedureParameters.add( AnnotationFactory.create( parameterDescriptor ) );
		}
		ann.setValue(
				"parameters",
				storedProcedureParameters.toArray( new StoredProcedureParameter[storedProcedureParameters.size()] )
		);

		if ( builder.getResultSetMappingName() != null ) {
			ann.setValue( "resultSetMappings", new String[]{ builder.getResultSetMappingName() } );
		}
		else {
			ann.setValue( "resultSetMappings", new String[0]  );
		}

		if ( builder.getResultSetMappingClassName() != null ) {
			ann.setValue(
					"resultClasses",
					new Class[] {
							context.getBootstrapContext()
									.getClassLoaderAccess().classForName( builder.getResultSetMappingClassName() )
					}
			);
		}
		else {
			ann.setValue( "resultClasses", new Class[0]  );
		}

		if ( builder.getQuerySpaces() != null ) {
			AnnotationDescriptor hintDescriptor = new AnnotationDescriptor( QueryHint.class );
			hintDescriptor.setValue( "name", HibernateHints.HINT_NATIVE_SPACES );
			hintDescriptor.setValue( "value", String.join( " ", builder.getQuerySpaces() ) );
			queryHints.add( AnnotationFactory.create( hintDescriptor ) );
		}

		AnnotationDescriptor hintDescriptor2 = new AnnotationDescriptor( QueryHint.class );
		hintDescriptor2.setValue( "name", HibernateHints.HINT_CALLABLE_FUNCTION );
		hintDescriptor2.setValue( "value", "true" );
		queryHints.add( AnnotationFactory.create( hintDescriptor2 ) );

		ann.setValue( "hints", queryHints.toArray( new QueryHint[queryHints.size()] ) );

		return new NamedProcedureCallDefinitionImpl( AnnotationFactory.create( ann ) );
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

		final NamedProcedureCallDefinitionImpl def = new NamedProcedureCallDefinitionImpl( annotation );

		if ( isDefault ) {
			context.getMetadataCollector().addDefaultNamedProcedureCall( def );
		}
		else {
			context.getMetadataCollector().addNamedProcedureCallDefinition( def );
		}
		LOG.debugf( "Bound named stored procedure query : %s => %s", def.getRegistrationName(), def.getProcedureName() );
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

	public static String parseJdbcCall(
			String sqlString,
			List<String> parameterNames,
			Supplier<RuntimeException> exceptionProducer) {
		String procedureName = null;
		int index = skipWhitespace( sqlString, 1 );
		// Parse the out param `?=` part
		if ( sqlString.charAt( index ) == '?' ) {
			index++;
			index = skipWhitespace( sqlString, index );
			if ( sqlString.charAt( index ) != '=' ) {
				throw exceptionProducer.get();
			}
			index++;
			index = skipWhitespace( sqlString, index );
		}
		// Parse the call keyword
		if ( !sqlString.regionMatches( true, index, "call", 0, 4 ) ) {
			throw exceptionProducer.get();
		}
		index += 4;
		index = skipWhitespace( sqlString, index );

		// Parse the procedure name
		final int procedureStart = index;
		for ( ; index < sqlString.length(); index++ ) {
			final char c = sqlString.charAt( index );
			if ( c == '(' || Character.isWhitespace( c ) ) {
				procedureName = sqlString.substring( procedureStart, index );
				break;
			}
		}
		index = skipWhitespace( sqlString, index );
		ParameterParser.parse(
				sqlString.substring( index, sqlString.length() - 1 ),
				new ParameterRecognizer() {
					@Override
					public void ordinalParameter(int sourcePosition) {
						parameterNames.add( "" );
					}

					@Override
					public void namedParameter(String name, int sourcePosition) {
						parameterNames.add( name );
					}

					@Override
					public void jpaPositionalParameter(int label, int sourcePosition) {
						parameterNames.add( "" );
					}

					@Override
					public void other(char character) {
					}
				}
		);
		return procedureName;
	}

	private static int skipWhitespace(String sqlString, int i) {
		while ( i < sqlString.length() ) {
			if ( !Character.isWhitespace( sqlString.charAt( i ) ) ) {
				break;
			}
			i++;
		}
		return i;
	}

	private static AnnotationException illegalCallSyntax(
			org.hibernate.annotations.NamedNativeQuery queryAnn,
			String sqlString) {
		return new AnnotationException(
				String.format(
						"Callable named native query [%s] doesn't use the JDBC call syntax: %s",
						queryAnn.name(),
						sqlString
				)
		);
	}
}
