/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Remove;
import org.hibernate.annotations.CacheModeType;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.annotations.HQLSelect;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.internal.NamedHqlQueryDefinitionImpl;
import org.hibernate.boot.internal.NamedProcedureCallDefinitionImpl;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinitionBuilder;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.boot.query.SqlResultSetMappingDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
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

import static java.lang.Boolean.TRUE;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.determineProperSizing;
import static org.hibernate.internal.util.collections.CollectionHelper.setOf;

/**
 * Responsible for reading named queries defined in annotations and registering
 * {@link org.hibernate.boot.query.NamedQueryDefinition} objects.
 *
 * @implNote This class is stateless, unlike most of the other "binders".
 *
 * @author Emmanuel Bernard
 */
public abstract class QueryBinder {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, QueryBinder.class.getName());

	public static void bindQuery(
			AnnotationUsage<NamedQuery> namedQuery,
			MetadataBuildingContext context,
			boolean isDefault) {
		if ( namedQuery == null ) {
			return;
		}

		final String queryName = namedQuery.getString( "name" );
		final String queryString = namedQuery.getString( "query" );

		if ( queryName.isEmpty() ) {
			throw new AnnotationException( "Class or package level '@NamedQuery' annotation must specify a 'name'" );
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding named query: %s => %s", queryName, queryString );
		}

		final QueryHintDefinition hints = new QueryHintDefinition( queryName, namedQuery.getList( "hints" ) );

		final NamedHqlQueryDefinition queryMapping = new NamedHqlQueryDefinitionImpl.Builder( queryName )
				.setHqlString( queryString )
				.setCacheable( hints.getCacheability() )
				.setCacheMode( hints.getCacheMode() )
				.setCacheRegion( hints.getString( HibernateHints.HINT_CACHE_REGION ) )
				.setTimeout( hints.getTimeout() )
				.setFetchSize( hints.getInteger( HibernateHints.HINT_FETCH_SIZE ) )
				.setFlushMode( hints.getFlushMode() )
				.setReadOnly( hints.getBooleanWrapper( HibernateHints.HINT_READ_ONLY ) )
				.setLockOptions( hints.determineLockOptions( namedQuery ) )
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
			AnnotationUsage<NamedNativeQuery> namedNativeQuery,
			MetadataBuildingContext context,
			boolean isDefault) {
		if ( namedNativeQuery == null ) {
			return;
		}

		final String registrationName = namedNativeQuery.getString( "name" );
		final String queryString = namedNativeQuery.getString( "query" );

		if ( registrationName.isEmpty() ) {
			throw new AnnotationException( "Class or package level '@NamedNativeQuery' annotation must specify a 'name'" );
		}

		final QueryHintDefinition hints = new QueryHintDefinition( registrationName, namedNativeQuery.getList( "hints" ) );

		final String resultSetMappingName = namedNativeQuery.getString( "resultSetMapping" );
		final ClassDetails resultClassDetails = namedNativeQuery.getClassDetails( "resultClass" );
		final String resultSetMappingClassName = ClassDetails.VOID_CLASS_DETAILS == resultClassDetails
				? null
				: resultClassDetails.getName();

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
				.setReadOnly( hints.getBooleanWrapper( HibernateHints.HINT_READ_ONLY ) )
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
			String name,
			AnnotationUsage<SQLSelect> sqlSelect,
			ClassDetails annotatedClass,
			MetadataBuildingContext context) {
		final NamedNativeQueryDefinitionBuilder builder = new NamedNativeQueryDefinitionBuilder( name )
				.setFlushMode( FlushMode.MANUAL )
				.setSqlString( sqlSelect.getString( "sql" ) )
				.setQuerySpaces( setOf( sqlSelect.getList( "querySpaces" ) ) );

		if ( annotatedClass != null ) {
			builder.setResultSetMappingClassName( annotatedClass.getName() );
		}

		final AnnotationUsage<SqlResultSetMapping> resultSetMapping = sqlSelect.getNestedUsage( "resultSetMapping" );
		if ( !resultSetMapping.getList( "columns" ).isEmpty()
				|| !resultSetMapping.getList( "entities" ).isEmpty()
				|| !resultSetMapping.getList( "classes" ).isEmpty() ) {
			context.getMetadataCollector().addResultSetMapping( SqlResultSetMappingDescriptor.from( resultSetMapping, name ) );
			builder.setResultSetMappingName( name );
		}

		context.getMetadataCollector().addNamedNativeQuery( builder.build() );
	}

	public static void bindNativeQuery(
			AnnotationUsage<org.hibernate.annotations.NamedNativeQuery> namedNativeQuery,
			MetadataBuildingContext context) {
		if ( namedNativeQuery == null ) {
			return;
		}

		final String registrationName = namedNativeQuery.getString( "name" );

		//ResultSetMappingDefinition mappingDefinition = mappings.getJdbcValuesMappingProducer( queryAnn.resultSetMapping() );
		if ( registrationName.isEmpty() ) {
			throw new AnnotationException( "Class or package level '@NamedNativeQuery' annotation must specify a 'name'" );
		}

		final String resultSetMappingName = namedNativeQuery.getString( "resultSetMapping" );
		final ClassDetails resultClassDetails = namedNativeQuery.getClassDetails( "resultClass" );
		final String resultSetMappingClassName = ClassDetails.VOID_CLASS_DETAILS == resultClassDetails
				? null
				: resultClassDetails.getName();

		final Integer timeout = namedNativeQuery.getInteger( "timeout" );
		final Integer fetchSize = namedNativeQuery.getInteger( "fetchSize" );

		final List<String> querySpacesList = namedNativeQuery.getList( "querySpaces" );
		final HashSet<String> querySpaces = new HashSet<>( determineProperSizing( querySpacesList.size() ) );
		querySpaces.addAll( querySpacesList );

		final NamedNativeQueryDefinitionBuilder builder = new NamedNativeQueryDefinitionBuilder( registrationName )
				.setSqlString( namedNativeQuery.getString( "query" ) )
				.setResultSetMappingName( resultSetMappingName )
				.setResultSetMappingClassName( resultSetMappingClassName )
				.setCacheable( namedNativeQuery.getBoolean( "cacheable" ) )
				.setCacheRegion( nullIfEmpty( namedNativeQuery.getString( "cacheRegion" ) ) )
				.setCacheMode( getCacheMode( namedNativeQuery )  )
				.setTimeout( timeout < 0 ? null : timeout )
				.setFetchSize( fetchSize < 0 ? null : fetchSize )
				.setFlushMode( getFlushMode( namedNativeQuery.getEnum( "flushMode" ) ) )
				.setReadOnly( namedNativeQuery.getBoolean( "readOnly" ) )
				.setQuerySpaces( querySpaces )
				.setComment( nullIfEmpty( namedNativeQuery.getString( "comment" ) ) );

		if ( TRUE == namedNativeQuery.getBoolean( "callable" ) ) {
			final NamedProcedureCallDefinition definition =
					createStoredProcedure( builder, context, () -> illegalCallSyntax( namedNativeQuery ) );
			context.getMetadataCollector().addNamedProcedureCallDefinition( definition );
			DeprecationLogger.DEPRECATION_LOGGER.warn(
					"Marking named native queries as callable is no longer supported; use '@jakarta.persistence.NamedStoredProcedureQuery' instead. Ignoring."
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
		final String sqlString = builder.getSqlString().trim();
		if ( !sqlString.startsWith( "{" ) || !sqlString.endsWith( "}" ) ) {
			throw exceptionProducer.get();
		}
		final JdbcCall jdbcCall = parseJdbcCall( sqlString, exceptionProducer );

		AnnotationDescriptor descriptor = new AnnotationDescriptor( NamedStoredProcedureQuery.class );
		descriptor.setValue( "name", builder.getName() );
		descriptor.setValue( "procedureName", jdbcCall.callableName );

		for ( String parameterName : jdbcCall.parameters ) {
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
		descriptor.setValue(
				"parameters",
				storedProcedureParameters.toArray( new StoredProcedureParameter[storedProcedureParameters.size()] )
		);

		if ( builder.getResultSetMappingName() != null ) {
			descriptor.setValue( "resultSetMappings", new String[]{ builder.getResultSetMappingName() } );
		}
		else {
			descriptor.setValue( "resultSetMappings", new String[0]  );
		}

		if ( builder.getResultSetMappingClassName() != null ) {
			descriptor.setValue(
					"resultClasses",
					new Class[] {
							context.getBootstrapContext()
									.getClassLoaderAccess().classForName( builder.getResultSetMappingClassName() )
					}
			);
		}
		else {
			descriptor.setValue( "resultClasses", new Class[0]  );
		}

		if ( builder.getQuerySpaces() != null ) {
			AnnotationDescriptor hintDescriptor = new AnnotationDescriptor( QueryHint.class );
			hintDescriptor.setValue( "name", HibernateHints.HINT_NATIVE_SPACES );
			hintDescriptor.setValue( "value", String.join( " ", builder.getQuerySpaces() ) );
			queryHints.add( AnnotationFactory.create( hintDescriptor ) );
		}

		if ( jdbcCall.resultParameter ) {
			// Mark native queries that have a result parameter as callable functions
			AnnotationDescriptor hintDescriptor2 = new AnnotationDescriptor( QueryHint.class );
			hintDescriptor2.setValue( "name", HibernateHints.HINT_CALLABLE_FUNCTION );
			hintDescriptor2.setValue( "value", "true" );
			queryHints.add( AnnotationFactory.create( hintDescriptor2 ) );
		}

		descriptor.setValue( "hints", queryHints.toArray( new QueryHint[queryHints.size()] ) );

		return new NamedProcedureCallDefinitionImpl( AnnotationFactory.create( descriptor ) );
	}

	public static void bindQueries(AnnotationUsage<NamedQueries> namedQueries, MetadataBuildingContext context, boolean isDefault) {
		if ( namedQueries == null ) {
			return;
		}

		final List<AnnotationUsage<NamedQuery>> nestedValues = namedQueries.getList( "value" );
		for ( AnnotationUsage<NamedQuery> nestedValue : nestedValues ) {
			bindQuery( nestedValue, context, isDefault );
		}
	}

	public static void bindNativeQueries(
			AnnotationUsage<NamedNativeQueries> namedNativeQueries,
			MetadataBuildingContext context,
			boolean isDefault) {
		if ( namedNativeQueries == null ) {
			return;
		}

		final List<AnnotationUsage<NamedNativeQuery>> nestedValues = namedNativeQueries.getList( "value" );
		for ( AnnotationUsage<NamedNativeQuery> nestedValue : nestedValues ) {
			bindNativeQuery( nestedValue, context, isDefault );
		}
	}

	public static void bindNativeQueries(
			AnnotationUsage<org.hibernate.annotations.NamedNativeQueries> namedNativeQueries,
			MetadataBuildingContext context) {
		if ( namedNativeQueries == null ) {
			return;
		}

		final List<AnnotationUsage<org.hibernate.annotations.NamedNativeQuery>> nestedValues = namedNativeQueries.getList( "value" );
		for ( AnnotationUsage<org.hibernate.annotations.NamedNativeQuery> nestedValue : nestedValues ) {
			bindNativeQuery( nestedValue, context );
		}
	}

	public static void bindQuery(
			String name,
			AnnotationUsage<HQLSelect> hqlSelect,
			MetadataBuildingContext context) {
		final NamedHqlQueryDefinition hqlQueryDefinition = new NamedHqlQueryDefinition.Builder( name )
				.setFlushMode( FlushMode.MANUAL )
				.setHqlString( hqlSelect.getString( "query" ) )
				.build();

		context.getMetadataCollector().addNamedQuery( hqlQueryDefinition );
	}

	public static void bindQuery(
			AnnotationUsage<org.hibernate.annotations.NamedQuery> namedQuery,
			MetadataBuildingContext context) {
		if ( namedQuery == null ) {
			return;
		}

		final String registrationName = namedQuery.getString( "name" );

		//ResultSetMappingDefinition mappingDefinition = mappings.getJdbcValuesMappingProducer( namedQuery.resultSetMapping() );
		if ( registrationName.isEmpty() ) {
			throw new AnnotationException( "Class or package level '@NamedQuery' annotation must specify a 'name'" );
		}

		final Integer timeout = namedQuery.getInteger( "timeout" );
		final Integer fetchSize = namedQuery.getInteger( "fetchSize" );

		final NamedHqlQueryDefinition.Builder builder = new NamedHqlQueryDefinition.Builder( registrationName )
				.setHqlString( namedQuery.getString( "query" ) )
				.setCacheable( namedQuery.getBoolean( "cacheable" ) )
				.setCacheRegion( nullIfEmpty( namedQuery.getString( "cacheRegion" ) ) )
				.setCacheMode( getCacheMode( namedQuery ) )
				.setTimeout( timeout < 0 ? null : timeout )
				.setFetchSize( fetchSize < 0 ? null : fetchSize )
				.setFlushMode( getFlushMode( namedQuery.getEnum( "flushMode" ) ) )
				.setReadOnly( namedQuery.getBoolean( "readOnly" ) )
				.setComment( nullIfEmpty( namedQuery.getString( "comment" ) ) );

		final NamedHqlQueryDefinitionImpl hqlQueryDefinition = builder.build();

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding named query: %s => %s", hqlQueryDefinition.getRegistrationName(), hqlQueryDefinition.getHqlString() );
		}

		context.getMetadataCollector().addNamedQuery( hqlQueryDefinition );
	}

	private static CacheMode getCacheMode(AnnotationUsage<?> namedQuery) {
		final CacheMode cacheMode = CacheMode.fromJpaModes(
				namedQuery.getEnum( "cacheRetrieveMode" ),
				namedQuery.getEnum( "cacheStoreMode" )
		);
		return cacheMode == null || cacheMode == CacheMode.NORMAL
				? interpretCacheMode( namedQuery.getEnum( "cacheMode" ) )
				: cacheMode;
	}

	private static FlushMode getFlushMode(FlushModeType flushModeType) {
		switch ( flushModeType ) {
			case ALWAYS:
				return FlushMode.ALWAYS;
			case AUTO:
				return FlushMode.AUTO;
			case COMMIT:
				return FlushMode.COMMIT;
			case MANUAL:
				return FlushMode.MANUAL;
			case PERSISTENCE_CONTEXT:
				return null;
			default:
				throw new AssertionFailure( "Unknown FlushModeType: " + flushModeType );
		}
	}

	private static CacheMode interpretCacheMode(CacheModeType cacheModeType) {
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
			AnnotationUsage<org.hibernate.annotations.NamedQueries> namedQueries,
			MetadataBuildingContext context) {
		if ( namedQueries == null ) {
			return;
		}

		final List<AnnotationUsage<org.hibernate.annotations.NamedQuery>> nestedValues = namedQueries.getList( "value" );
		for ( AnnotationUsage<org.hibernate.annotations.NamedQuery> nestedValue : nestedValues ) {
			bindQuery( nestedValue, context );
		}
	}

	public static void bindNamedStoredProcedureQuery(
			AnnotationUsage<NamedStoredProcedureQuery> namedStoredProcedureQuery,
			MetadataBuildingContext context,
			boolean isDefault) {
		if ( namedStoredProcedureQuery != null ) {
			if ( namedStoredProcedureQuery.getString( "name" ).isEmpty() ) {
				throw new AnnotationException( "Class or package level '@NamedStoredProcedureQuery' annotation must specify a 'name'" );
			}

			final NamedProcedureCallDefinitionImpl definition = new NamedProcedureCallDefinitionImpl( namedStoredProcedureQuery );
			if ( isDefault ) {
				context.getMetadataCollector().addDefaultNamedProcedureCall( definition );
			}
			else {
				context.getMetadataCollector().addNamedProcedureCallDefinition( definition );
			}
			LOG.debugf( "Bound named stored procedure query : %s => %s", definition.getRegistrationName(), definition.getProcedureName() );
		}
	}

	public static void bindSqlResultSetMappings(
			AnnotationUsage<SqlResultSetMappings> resultSetMappings,
			MetadataBuildingContext context,
			boolean isDefault) {
		if ( resultSetMappings == null ) {
			return;
		}

		final List<AnnotationUsage<SqlResultSetMapping>> mappings = resultSetMappings.getList( "value" );
		for ( AnnotationUsage<SqlResultSetMapping> mapping : mappings ) {
			bindSqlResultSetMapping( mapping, context, isDefault );
		}
	}

	public static void bindSqlResultSetMapping(
			AnnotationUsage<SqlResultSetMapping> resultSetMappingAnn,
			MetadataBuildingContext context,
			boolean isDefault) {
		//no need to handle inSecondPass
		context.getMetadataCollector().addSecondPass( new ResultSetMappingSecondPass( resultSetMappingAnn, context, isDefault ) );
	}

	private static class JdbcCall {
		private final String callableName;
		private final boolean resultParameter;
		private final ArrayList<String> parameters;

		public JdbcCall(String callableName, boolean resultParameter, ArrayList<String> parameters) {
			this.callableName = callableName;
			this.resultParameter = resultParameter;
			this.parameters = parameters;
		}
	}

	private static JdbcCall parseJdbcCall(String sqlString, Supplier<RuntimeException> exceptionProducer) {
		String callableName = null;
		boolean resultParameter = false;
		int index = skipWhitespace( sqlString, 1 );
		// Parse the out param `?=` part
		if ( sqlString.charAt( index ) == '?' ) {
			resultParameter = true;
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
				callableName = sqlString.substring( procedureStart, index );
				break;
			}
		}
		index = skipWhitespace( sqlString, index );
		final ArrayList<String> parameters = new ArrayList<>();
		ParameterParser.parse(
				sqlString.substring( index, sqlString.length() - 1 ),
				new ParameterRecognizer() {
					@Override
					public void ordinalParameter(int sourcePosition) {
						parameters.add( "" );
					}

					@Override
					public void namedParameter(String name, int sourcePosition) {
						parameters.add( name );
					}

					@Override
					public void jpaPositionalParameter(int label, int sourcePosition) {
						parameters.add( "" );
					}

					@Override
					public void other(char character) {
					}
				}
		);
		return new JdbcCall( callableName, resultParameter, parameters );
	}

	@Remove
	@Deprecated(since = "6.2")
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

	private static AnnotationException illegalCallSyntax(AnnotationUsage<org.hibernate.annotations.NamedNativeQuery> queryAnn) {
		return new AnnotationException( "Callable 'NamedNativeQuery' named '" + queryAnn.getString( "name" )
				+ "' does not use the JDBC call syntax" );
	}
}
