/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.AnnotationException;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.annotations.HQLSelect;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.boot.internal.NamedHqlQueryDefinitionImpl;
import org.hibernate.boot.internal.NamedProcedureCallDefinitionImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.NamedStoredProcedureQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.QueryHintJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.StoredProcedureParameterJpaAnnotation;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.boot.query.SqlResultSetMappingDescriptor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.models.internal.util.StringHelper;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.query.sql.internal.ParameterParser;
import org.hibernate.query.sql.spi.ParameterRecognizer;
import org.hibernate.type.BasicType;

import org.jboss.logging.Logger;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SqlResultSetMapping;
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
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, QueryBinder.class.getName() );

	public static void bindQuery(
			NamedQuery namedQuery,
			MetadataBuildingContext context,
			boolean isDefault) {
		if ( namedQuery == null ) {
			return;
		}

		final String queryName = namedQuery.name();
		final String queryString = namedQuery.query();

		if ( queryName.isEmpty() ) {
			throw new AnnotationException( "Class or package level '@NamedQuery' annotation must specify a 'name'" );
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding named query: %s => %s", queryName, queryString );
		}

		final QueryHintDefinition hints = new QueryHintDefinition( queryName, namedQuery.hints() );

		final NamedHqlQueryDefinition<?> queryMapping = new NamedHqlQueryDefinitionImpl.Builder<>( queryName )
				.setHqlString( queryString )
				.setResultClass( (Class<Object>) namedQuery.resultClass() )
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

	private static Class<Object> loadClass(ClassDetails classDetails, MetadataBuildingContext context) {
		return ClassDetails.VOID_CLASS_DETAILS == classDetails
				? null
				: context.getBootstrapContext()
						.getServiceRegistry()
						.requireService( ClassLoaderService.class )
						.classForName( classDetails.getName() );
	}

	public static void bindNativeQuery(
			NamedNativeQuery namedNativeQuery,
			MetadataBuildingContext context,
			boolean isDefault) {
		if ( namedNativeQuery == null ) {
			return;
		}

		final String registrationName = namedNativeQuery.name();
		final String queryString = namedNativeQuery.query();

		if ( registrationName.isEmpty() ) {
			throw new AnnotationException( "Class or package level '@NamedNativeQuery' annotation must specify a 'name'" );
		}

		final QueryHintDefinition hints = new QueryHintDefinition( registrationName, namedNativeQuery.hints() );

		final String resultSetMappingName = namedNativeQuery.resultSetMapping();
		final Class<?> resultClassDetails = namedNativeQuery.resultClass();
		final Class<Object> resultClass = void.class == resultClassDetails
				? null
				: (Class<Object>) resultClassDetails;

		final NamedNativeQueryDefinition.Builder<?> builder = new NamedNativeQueryDefinition.Builder<>( registrationName )
				.setSqlString( queryString )
				.setResultClass( resultClass )
				.setResultSetMappingName( resultSetMappingName )
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

		final NamedNativeQueryDefinition<?> queryDefinition = builder.build();

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
			SQLSelect sqlSelect,
			ClassDetails annotatedClass,
			MetadataBuildingContext context) {
		final NamedNativeQueryDefinition.Builder<?> builder = new NamedNativeQueryDefinition.Builder<>( name )
				.setFlushMode( FlushMode.MANUAL )
				.setSqlString( sqlSelect.sql() )
				.setQuerySpaces( setOf( sqlSelect.querySpaces() ) );

		if ( annotatedClass != null ) {
			builder.setResultClass(
					context.getBootstrapContext().getServiceRegistry().requireService( ClassLoaderService.class )
							.classForName( annotatedClass.getClassName() )
			);
		}

		final SqlResultSetMapping resultSetMapping = sqlSelect.resultSetMapping();
		if ( !ArrayHelper.isEmpty( resultSetMapping.columns() )
				|| !ArrayHelper.isEmpty( resultSetMapping.entities() )
				|| !ArrayHelper.isEmpty( resultSetMapping.classes() ) ) {
			context.getMetadataCollector().addResultSetMapping( SqlResultSetMappingDescriptor.from( resultSetMapping, name ) );
			builder.setResultSetMappingName( name );
		}

		context.getMetadataCollector().addNamedNativeQuery( builder.build() );
	}

	public static void bindNativeQuery(
			org.hibernate.annotations.NamedNativeQuery namedNativeQuery,
			MetadataBuildingContext context) {
		if ( namedNativeQuery == null ) {
			return;
		}

		final String registrationName = namedNativeQuery.name();

		//ResultSetMappingDefinition mappingDefinition = mappings.getJdbcValuesMappingProducer( queryAnn.resultSetMapping() );
		if ( registrationName.isEmpty() ) {
			throw new AnnotationException( "Class or package level '@NamedNativeQuery' annotation must specify a 'name'" );
		}

		final String resultSetMappingName = namedNativeQuery.resultSetMapping();
		final Class<?> resultClassDetails = namedNativeQuery.resultClass();
		final Class<Object> resultClass = resultClassDetails == void.class
				? null
				: (Class<Object>) resultClassDetails;

		final Integer timeout = namedNativeQuery.timeout();
		final Integer fetchSize = namedNativeQuery.fetchSize();

		final String[] querySpacesList = namedNativeQuery.querySpaces();
		final HashSet<String> querySpaces = new HashSet<>( determineProperSizing( querySpacesList.length ) );
		Collections.addAll( querySpaces, querySpacesList );

		final NamedNativeQueryDefinition.Builder<?> builder = new NamedNativeQueryDefinition.Builder<>( registrationName )
				.setSqlString( namedNativeQuery.query() )
				.setResultSetMappingName( resultSetMappingName )
				.setResultClass( resultClass )
				.setCacheable( namedNativeQuery.cacheable() )
				.setCacheRegion( nullIfEmpty( namedNativeQuery.cacheRegion() ) )
				.setCacheMode( getCacheMode( namedNativeQuery.cacheRetrieveMode(), namedNativeQuery.cacheStoreMode() ) )
				.setTimeout( timeout < 0 ? null : timeout )
				.setFetchSize( fetchSize < 0 ? null : fetchSize )
				.setFlushMode( getFlushMode( namedNativeQuery.flushMode() ) )
				.setReadOnly( namedNativeQuery.readOnly() )
				.setQuerySpaces( querySpaces )
				.setComment( nullIfEmpty( namedNativeQuery.comment() ) );

		if ( TRUE == namedNativeQuery.callable() ) {
			final NamedProcedureCallDefinition definition =
					createStoredProcedure( builder, context, () -> illegalCallSyntax( namedNativeQuery ) );
			context.getMetadataCollector().addNamedProcedureCallDefinition( definition );
			DeprecationLogger.DEPRECATION_LOGGER.warn(
					"Marking named native queries as callable is no longer supported; use '@jakarta.persistence.NamedStoredProcedureQuery' instead. Ignoring."
			);
		}
		else {
			final NamedNativeQueryDefinition<?> queryDefinition = builder.build();

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

	/**
	 * Handles legacy cases where a named native query was used to specify a procedure call
	 *
	 * @deprecated User should use {@linkplain NamedStoredProcedureQuery} instead
	 */
	@Deprecated
	public static NamedProcedureCallDefinition createStoredProcedure(
			NamedNativeQueryDefinition.Builder<?> builder,
			MetadataBuildingContext context,
			Supplier<RuntimeException> exceptionProducer) {
		final String sqlString = builder.getSqlString().trim();
		if ( !sqlString.startsWith( "{" ) || !sqlString.endsWith( "}" ) ) {
			throw exceptionProducer.get();
		}
		final JdbcCall jdbcCall = parseJdbcCall( sqlString, exceptionProducer );

		final SourceModelBuildingContext sourceModelBuildingContext = context.getMetadataCollector()
				.getSourceModelBuildingContext();
		final NamedStoredProcedureQueryJpaAnnotation nameStoredProcedureQueryAnn = JpaAnnotations.NAMED_STORED_PROCEDURE_QUERY.createUsage( sourceModelBuildingContext );
		nameStoredProcedureQueryAnn.name( builder.getName() );
		nameStoredProcedureQueryAnn.procedureName( jdbcCall.callableName );

		final StoredProcedureParameter[] parameters = new StoredProcedureParameter[jdbcCall.parameters.size()];
		nameStoredProcedureQueryAnn.parameters( parameters );

		for ( int i = 0; i < jdbcCall.parameters.size(); i++ ) {
			final StoredProcedureParameterJpaAnnotation param = JpaAnnotations.STORED_PROCEDURE_PARAMETER.createUsage( sourceModelBuildingContext );
			parameters[i] = param;

			final String paramName = jdbcCall.parameters.get( i );
			param.name( paramName );
			param.mode( ParameterMode.IN );

			final String typeName = builder.getParameterTypes().get( paramName );
			final ClassDetails classDetails;
			if ( StringHelper.isEmpty( typeName ) ) {
				classDetails = ClassDetails.VOID_CLASS_DETAILS;
			}
			else {
				final BasicType<Object> registeredType = context.getBootstrapContext()
						.getTypeConfiguration()
						.getBasicTypeRegistry()
						.getRegisteredType( typeName );
				classDetails = context.getMetadataCollector().getClassDetailsRegistry().getClassDetails( registeredType.getJavaType().getName() );
			}
			param.type( classDetails.toJavaClass() );
		}

		if ( builder.getResultSetMappingName() != null ) {
			nameStoredProcedureQueryAnn.resultSetMappings( new String[] { builder.getResultSetMappingName() } );
		}

		final Class<?> resultClass = builder.getResultClass();
		if ( resultClass != null ) {
			nameStoredProcedureQueryAnn.resultClasses( new Class[]{ builder.getResultClass() } );
		}

		final List<QueryHintJpaAnnotation> hints = new ArrayList<>();

		if ( builder.getQuerySpaces() != null ) {
			final QueryHintJpaAnnotation hint = JpaAnnotations.QUERY_HINT.createUsage( sourceModelBuildingContext );
			hint.name( HibernateHints.HINT_NATIVE_SPACES );
			hint.value( String.join( " ", builder.getQuerySpaces() ) );
			hints.add( hint );
		}

		if ( jdbcCall.resultParameter ) {
			// Mark native queries that have a result parameter as callable functions
			final QueryHintJpaAnnotation hint = JpaAnnotations.QUERY_HINT.createUsage( sourceModelBuildingContext );
			hint.name( HibernateHints.HINT_CALLABLE_FUNCTION );
			hint.value( "true" );
			hints.add( hint );
		}

		nameStoredProcedureQueryAnn.hints( hints.toArray(QueryHint[]::new) );

		return new NamedProcedureCallDefinitionImpl( nameStoredProcedureQueryAnn );
	}

	public static void bindQuery(
			String name,
			HQLSelect hqlSelect,
			MetadataBuildingContext context) {
		final NamedHqlQueryDefinition<?> hqlQueryDefinition = new NamedHqlQueryDefinition.Builder<>( name )
				.setFlushMode( FlushMode.MANUAL )
				.setHqlString( hqlSelect.query() )
				.build();

		context.getMetadataCollector().addNamedQuery( hqlQueryDefinition );
	}

	public static void bindQuery(
			org.hibernate.annotations.NamedQuery namedQuery,
			MetadataBuildingContext context) {
		if ( namedQuery == null ) {
			return;
		}

		final String registrationName = namedQuery.name();

		//ResultSetMappingDefinition mappingDefinition = mappings.getJdbcValuesMappingProducer( namedQuery.resultSetMapping() );
		if ( registrationName.isEmpty() ) {
			throw new AnnotationException( "Class or package level '@NamedQuery' annotation must specify a 'name'" );
		}

		final int timeout = namedQuery.timeout();
		final int fetchSize = namedQuery.fetchSize();

		final NamedHqlQueryDefinition.Builder<?> builder = new NamedHqlQueryDefinition.Builder<>( registrationName )
				.setHqlString( namedQuery.query() )
				.setResultClass( (Class<Object>) namedQuery.resultClass() )
				.setCacheable( namedQuery.cacheable() )
				.setCacheRegion( nullIfEmpty( namedQuery.cacheRegion() ) )
				.setCacheMode( getCacheMode( namedQuery.cacheRetrieveMode(), namedQuery.cacheStoreMode() ) )
				.setTimeout( timeout < 0 ? null : timeout )
				.setFetchSize( fetchSize < 0 ? null : fetchSize )
				.setFlushMode( getFlushMode( namedQuery.flushMode() ) )
				.setReadOnly( namedQuery.readOnly() )
				.setComment( nullIfEmpty( namedQuery.comment() ) );

		final NamedHqlQueryDefinitionImpl<?> hqlQueryDefinition = builder.build();

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding named query: %s => %s", hqlQueryDefinition.getRegistrationName(), hqlQueryDefinition.getHqlString() );
		}

		context.getMetadataCollector().addNamedQuery( hqlQueryDefinition );
	}

	private static CacheMode getCacheMode(CacheRetrieveMode cacheRetrieveMode, CacheStoreMode cacheStoreMode) {
		final CacheMode cacheMode = CacheMode.fromJpaModes( cacheRetrieveMode, cacheStoreMode );
		return cacheMode == null ? CacheMode.NORMAL : cacheMode;
	}

	private static FlushMode getFlushMode(FlushModeType flushModeType) {
		return switch ( flushModeType ) {
			case ALWAYS -> FlushMode.ALWAYS;
			case AUTO -> FlushMode.AUTO;
			case COMMIT -> FlushMode.COMMIT;
			case MANUAL -> FlushMode.MANUAL;
			case PERSISTENCE_CONTEXT -> null;
		};
	}

	public static void bindNamedStoredProcedureQuery(
			NamedStoredProcedureQuery namedStoredProcedureQuery,
			MetadataBuildingContext context,
			boolean isDefault) {
		if ( namedStoredProcedureQuery != null ) {
			if ( namedStoredProcedureQuery.name().isEmpty() ) {
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

	public static void bindSqlResultSetMapping(
			SqlResultSetMapping resultSetMappingAnn,
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

	private static int skipWhitespace(String sqlString, int i) {
		while ( i < sqlString.length() ) {
			if ( !Character.isWhitespace( sqlString.charAt( i ) ) ) {
				break;
			}
			i++;
		}
		return i;
	}

	private static AnnotationException illegalCallSyntax(org.hibernate.annotations.NamedNativeQuery queryAnn) {
		return new AnnotationException( "Callable 'NamedNativeQuery' named '" + queryAnn.name()
				+ "' does not use the JDBC call syntax" );
	}
}
