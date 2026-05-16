/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedNativeStatement;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStatement;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.EntityResult;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.Timeout;
import jakarta.persistence.query.JakartaQuery;
import jakarta.persistence.query.QueryOptions;
import org.hibernate.AnnotationException;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.annotations.HQLSelect;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.boot.model.source.internal.hbm.NativeQueryBuilder;
import org.hibernate.boot.query.internal.NamedHqlMutationDefinitionImpl;
import org.hibernate.boot.query.internal.NamedHqlSelectionDefinitionImpl;
import org.hibernate.boot.query.internal.NamedNativeMutationDefinitionImpl;
import org.hibernate.boot.query.internal.NamedNativeSelectionDefinitionImpl;
import org.hibernate.boot.query.internal.NamedProcedureCallDefinitionImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.boot.query.SqlResultSetMappingDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.query.sql.internal.ParameterParser;
import org.hibernate.query.sql.spi.ParameterRecognizer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.Character.isWhitespace;
import static org.hibernate.boot.BootLogging.BOOT_LOGGER;
import static org.hibernate.boot.query.internal.Helper.extractHints;
import static org.hibernate.internal.util.PrimitiveHelper.boxedType;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_OBJECT_ARRAY;
import static org.hibernate.models.internal.util.StringHelper.isEmpty;

/**
 * Responsible for reading named queries defined in annotations and registering
 * {@link org.hibernate.boot.query.NamedQueryDefinition} objects.
 *
 * @implNote This class is stateless, unlike most of the other "binders".
 *
 * @author Emmanuel Bernard
 */
public abstract class QueryBinder {
	private static final String JAKARTA_DATA_QUERY = "jakarta.data.repository.Query";

	public static void bindQuery(
			NamedQuery namedQuery,
			MetadataBuildingContext context,
			boolean isDefault,
			AnnotationTarget annotationTarget) {
		if ( namedQuery != null ) {
			final String queryName = namedQuery.name();
			final String queryString = namedQuery.query();

			if ( queryName.isBlank() ) {
				throw new AnnotationException(
						"Class or package level '@NamedQuery' annotation must specify a 'name'" );
			}

			if ( BOOT_LOGGER.isTraceEnabled() ) {
				BOOT_LOGGER.bindingNamedQuery( queryName,
						queryString.replace( '\n', ' ' ) );
			}

			final var definition = NamedHqlSelectionDefinitionImpl.from( namedQuery, annotationTarget );
			final var collector = context.getMetadataCollector();
			if ( isDefault ) {
				collector.addDefaultQuery( definition );
			}
			else {
				collector.addNamedQuery( definition );
			}
		}
	}

	public static void bindStatement(
			NamedStatement annotation,
			MetadataBuildingContext context,
			AnnotationTarget location) {
		if ( annotation != null ) {
			final String registrationName = annotation.name();

			if ( registrationName.isBlank() ) {
				throw new AnnotationException(
						"Class or package level '@NamedStatement' annotation must specify a 'name'" );
			}

			if ( BOOT_LOGGER.isTraceEnabled() ) {
				BOOT_LOGGER.bindingNamedMutation( registrationName,
						annotation.statement().replace( '\n', ' ' ) );
			}

			final var definition = NamedHqlMutationDefinitionImpl.from( annotation, location );
			context.getMetadataCollector().addNamedQuery( definition );
		}
	}

	public static void bindNativeStatement(
			NamedNativeStatement annotation,
			MetadataBuildingContext context,
			AnnotationTarget location) {
		if ( annotation != null ) {
			final String registrationName = annotation.name();

			if ( registrationName.isBlank() ) {
				throw new AnnotationException(
						"Class or package level '@NamedStatement' annotation must specify a 'name'" );
			}

			if ( BOOT_LOGGER.isTraceEnabled() ) {
				BOOT_LOGGER.bindingNamedNativeMutation( registrationName,
						annotation.statement().replace( '\n', ' ' ) );
			}

			final var definition = NamedNativeMutationDefinitionImpl.from( annotation, location );
			context.getMetadataCollector().addNamedNativeQuery( definition );
		}
	}

	public static void bindNativeQuery(
			NamedNativeQuery namedNativeQuery,
			MetadataBuildingContext context,
			AnnotationTarget location,
			boolean isDefault) {
		if ( namedNativeQuery != null ) {
			final String registrationName = namedNativeQuery.name();
			final String queryString = namedNativeQuery.query();

			if ( registrationName.isBlank() ) {
				throw new AnnotationException(
						"Class or package level '@NamedNativeQuery' annotation must specify a 'name'" );
			}

			if ( BOOT_LOGGER.isTraceEnabled() ) {
				BOOT_LOGGER.bindingNamedNativeQuery( registrationName,
						queryString.replace( '\n', ' ' ) );
			}

			final var collector = context.getMetadataCollector();
			final String resultSetMappingName;
			if ( hasInlineResultSetMapping( namedNativeQuery ) ) {
				resultSetMappingName = registrationName;
				if ( !namedNativeQuery.resultSetMapping().isBlank() ) {
					throw new AnnotationException(
							"Named native query '%s' specified both 'resultSetMapping' and an inline result set mapping"
									.formatted( registrationName )
					);
				}

				final var mappingDefinition = SqlResultSetMappingDescriptor.from( namedNativeQuery );
				if ( isDefault ) {
					collector.addDefaultResultSetMapping( mappingDefinition );
				}
				else {
					collector.addResultSetMapping( mappingDefinition );
				}
			}
			else {
				resultSetMappingName = namedNativeQuery.resultSetMapping();
			}

			final var definition =
					NamedNativeSelectionDefinitionImpl.from( namedNativeQuery, location, resultSetMappingName );
			if ( isDefault ) {
				collector.addDefaultNamedNativeQuery( definition );
			}
			else {
				collector.addNamedNativeQuery( definition );
			}
		}
	}

	private static boolean hasInlineResultSetMapping(NamedNativeQuery namedNativeQuery) {
		return namedNativeQuery.entities().length > 0
			|| namedNativeQuery.classes().length > 0
			|| namedNativeQuery.columns().length > 0;
	}

	public static void bindStaticQueries(
			ClassDetails classDetails,
			MetadataBuildingContext context) {
		final var modelsContext = context.getBootstrapContext().getModelsContext();
		for ( MethodDetails methodDetails : classDetails.getMethods() ) {
			bindStaticQuery( classDetails, methodDetails, context, modelsContext );
			bindStaticNativeQuery( classDetails, methodDetails, context, modelsContext );
		}
	}

	private static void bindStaticQuery(
			ClassDetails classDetails,
			MethodDetails methodDetails,
			MetadataBuildingContext context,
			ModelsContext modelsContext) {
		final String queryString = staticQueryString( methodDetails, modelsContext );
		if ( queryString != null ) {
			final String registrationName = staticQueryName( classDetails, methodDetails );
			if ( BOOT_LOGGER.isTraceEnabled() ) {
				BOOT_LOGGER.bindingNamedQuery( registrationName, queryString.replace( '\n', ' ' ) );
			}

			final var options = methodDetails.getAnnotationUsage( QueryOptions.class, modelsContext );
			if ( isStatement( queryString ) ) {
				final var definition = new NamedHqlMutationDefinitionImpl<>(
						registrationName,
						staticQueryLocation( classDetails, methodDetails ),
						queryString,
						null,
						queryFlushMode( options ),
						timeout( options ),
						null,
						hints( options )
				);
				context.getMetadataCollector().addNamedQuery( definition );
			}
			else {
				final var definition = new NamedHqlSelectionDefinitionImpl<>(
						registrationName,
						staticQueryLocation( classDetails, methodDetails ),
						queryString,
						staticSelectionResultType( methodDetails ),
						entityGraphName( options ),
						queryFlushMode( options ),
						timeout( options ),
						null,
						null,
						null,
						null,
						null,
						null,
						cacheMode( options ),
						null,
						lockMode( options ),
						lockScope( options ),
						null,
						null,
						Map.of(),
						hints( options )
				);
				context.getMetadataCollector().addNamedQuery( definition );
			}
		}
	}

	private static String staticQueryString(MethodDetails methodDetails, ModelsContext modelsContext) {
		final var jakartaQuery = methodDetails.getAnnotationUsage( JakartaQuery.class, modelsContext );
		if ( jakartaQuery != null ) {
			return jakartaQuery.value();
		}

		for ( Annotation annotation : methodDetails.getDirectAnnotationUsages() ) {
			if ( JAKARTA_DATA_QUERY.equals( annotation.annotationType().getName() ) ) {
				return annotationValue( annotation );
			}
		}
		return null;
	}

	private static String annotationValue(Annotation annotation) {
		try {
			return (String) annotation.annotationType().getMethod( "value" ).invoke( annotation );
		}
		catch (ClassCastException | ReflectiveOperationException e) {
			throw new AnnotationException(
					"Annotation '@" + annotation.annotationType().getName()
							+ "' did not expose a String-valued 'value' member",
					e
			);
		}
	}

	private static void bindStaticNativeQuery(
			ClassDetails classDetails,
			MethodDetails methodDetails,
			MetadataBuildingContext context,
			ModelsContext modelsContext) {
		final var query = methodDetails.getAnnotationUsage(
				jakarta.persistence.query.NativeQuery.class,
				modelsContext
		);
		if ( query != null ) {
			final String registrationName = staticQueryName( classDetails, methodDetails );
			if ( BOOT_LOGGER.isTraceEnabled() ) {
				BOOT_LOGGER.bindingNamedNativeQuery( registrationName, query.value().replace( '\n', ' ' ) );
			}

			final var options = methodDetails.getAnnotationUsage( QueryOptions.class, modelsContext );
			if ( isStatement( query.value() ) ) {
				final var definition = new NamedNativeMutationDefinitionImpl<>(
						registrationName,
						staticQueryLocation( classDetails, methodDetails ),
						query.value(),
						new HashSet<>(),
						queryFlushMode( options ),
						timeout( options ),
						null,
						hints( options )
				);
				context.getMetadataCollector().addNamedNativeQuery( definition );
			}
			else {
				final String resultSetMappingName =
						bindStaticNativeQueryResultSetMapping( registrationName, methodDetails, context,
								modelsContext );
				final var definition = new NamedNativeSelectionDefinitionImpl<>(
						registrationName,
						staticQueryLocation( classDetails, methodDetails ),
						query.value(),
						staticSelectionResultType( methodDetails ),
						resultSetMappingName,
						queryFlushMode( options ),
						timeout( options ),
						null,
						null,
						null,
						null,
						null,
						null,
						cacheMode( options ),
						null,
						lockMode( options ),
						lockScope( options ),
						null,
						null,
						Set.of(),
						hints( options )
				);
				context.getMetadataCollector().addNamedNativeQuery( definition );
			}
		}
	}

	private static String bindStaticNativeQueryResultSetMapping(
			String registrationName,
			MethodDetails methodDetails,
			MetadataBuildingContext context,
			ModelsContext modelsContext) {
		final var entityResults = repeatedAnnotations( methodDetails, EntityResult.class, modelsContext );
		final var constructorResults = repeatedAnnotations( methodDetails, ConstructorResult.class, modelsContext );
		final var columnResults = repeatedAnnotations( methodDetails, ColumnResult.class, modelsContext );
		if ( entityResults.length > 0 || constructorResults.length > 0 || columnResults.length > 0 ) {
			context.getMetadataCollector().addResultSetMapping(
					SqlResultSetMappingDescriptor.from( registrationName, entityResults, constructorResults, columnResults )
			);
			return registrationName;
		}
		return null;
	}

	private static <A extends Annotation> A[] repeatedAnnotations(
			MethodDetails methodDetails,
			Class<A> annotationType,
			ModelsContext modelsContext) {
		final A[] annotations = methodDetails.getRepeatedAnnotationUsages( annotationType, modelsContext );
		//noinspection unchecked
		return annotations == null ? (A[]) EMPTY_OBJECT_ARRAY : annotations;
	}

	private static Class<?> staticSelectionResultType(MethodDetails methodDetails) {
		final var returnType = methodDetails.getReturnType();
		return returnType.isImplementor( List.class )
			|| returnType.isImplementor( Stream.class )
				? erasedClass( firstTypeArgument( methodDetails ) )
				: boxedType( returnType.toJavaClass() );
	}

	private static Type firstTypeArgument(MethodDetails methodDetails) {
		final Type genericReturnType = methodDetails.toJavaMember().getGenericReturnType();
		if ( genericReturnType instanceof ParameterizedType parameterizedType
				&& parameterizedType.getActualTypeArguments().length == 1 ) {
			return parameterizedType.getActualTypeArguments()[0];
		}
		return Object.class;
	}

	private static Class<?> erasedClass(Type type) {
		if ( type instanceof Class<?> typeClass ) {
			return typeClass;
		}
		else if ( type instanceof ParameterizedType parameterizedType ) {
			return erasedClass( parameterizedType.getRawType() );
		}
		else if ( type instanceof GenericArrayType genericArrayType ) {
			return Array.newInstance( erasedClass( genericArrayType.getGenericComponentType() ), 0 ).getClass();
		}
		else if ( type instanceof WildcardType wildcardType ) {
			return wildcardType.getUpperBounds().length == 0
					? Object.class
					: erasedClass( wildcardType.getUpperBounds()[0] );
		}
		else {
			return Object.class;
		}
	}

	private static boolean isStatement(String queryString) {
		final String keyword = firstKeyword( queryString );
		return "insert".equalsIgnoreCase( keyword )
			|| "update".equalsIgnoreCase( keyword )
			|| "delete".equalsIgnoreCase( keyword );
	}

	private static String firstKeyword(String queryString) {
		final String trimmed = queryString.trim();
		final int length = trimmed.length();
		for ( int i = 0; i < length; i++ ) {
			if ( isWhitespace( trimmed.charAt( i ) ) ) {
				return trimmed.substring( 0, i );
			}
		}
		return trimmed;
	}

	private static String staticQueryName(ClassDetails classDetails, MethodDetails methodDetails) {
		return unqualifiedClassName( classDetails ) + "." + methodDetails.getName();
	}

	private static String unqualifiedClassName(ClassDetails classDetails) {
		final String className = classDetails.getClassName() == null ? classDetails.getName() : classDetails.getClassName();
		final int packageSeparator = className.lastIndexOf( '.' );
		final String unqualifiedName = packageSeparator < 0 ? className : className.substring( packageSeparator + 1 );
		final int nestedClassSeparator = unqualifiedName.lastIndexOf( '$' );
		return nestedClassSeparator < 0 ? unqualifiedName : unqualifiedName.substring( nestedClassSeparator + 1 );
	}

	private static String staticQueryLocation(ClassDetails classDetails, MethodDetails methodDetails) {
		return classDetails.getName() + "#" + methodDetails.getName();
	}

	private static FlushMode queryFlushMode(QueryOptions options) {
		if ( options == null || options.flush() == QueryFlushMode.DEFAULT ) {
			return null;
		}
		return options.flush() == QueryFlushMode.FLUSH ? FlushMode.ALWAYS : FlushMode.MANUAL;
	}

	private static Timeout timeout(QueryOptions options) {
		return options == null || options.timeout() < 0 ? null : Timeout.milliseconds( options.timeout() );
	}

	private static Map<String,Object> hints(QueryOptions options) {
		return options == null ? Map.of() : extractHints( options.hints() );
	}

	private static String entityGraphName(QueryOptions options) {
		return options == null || options.entityGraph().isBlank() ? null : options.entityGraph();
	}

	private static CacheMode cacheMode(QueryOptions options) {
		return options == null ? null : CacheMode.fromJpaModes( options.cacheRetrieveMode(), options.cacheStoreMode() );
	}

	private static LockMode lockMode(QueryOptions options) {
		return options == null ? null : LockMode.fromJpaLockMode( options.lockMode() );
	}

	private static PessimisticLockScope lockScope(QueryOptions options) {
		return options == null ? null : options.lockScope();
	}
//
//	private static <T> NamedNativeQueryDefinition<T> createNamedQueryDefinition(
//			String registrationName, String queryString,
//			Class<T> resultClass, String resultSetMappingName,
//			QueryHintDefinition hints,
//			AnnotationTarget location) {
//		return new NativeQueryBuilder<T>(registrationName, location)
//				.setSqlString(queryString)
//				.setResultClass(resultClass)
//				.setResultSetMappingName(resultSetMappingName)
//				.setQuerySpaces(null)
//				.setCacheable(hints.getCacheability())
//				.setCacheMode(hints.getCacheMode())
//				.setCacheRegion(hints.getString(HibernateHints.HINT_CACHE_REGION))
//				.setTimeout(hints.getTimeoutRef())
//				.setFetchSize(hints.getInteger(HibernateHints.HINT_FETCH_SIZE))
//				.setFlushMode(hints.getFlushMode())
//				.setReadOnly(hints.getBooleanWrapper(HibernateHints.HINT_READ_ONLY))
//				.setComment(hints.getString(HibernateHints.HINT_COMMENT))
//				.addHints(hints.getHintsMap())
//				.build();
//	}

	public static void bindNativeQuery(
			String name,
			SQLSelect sqlSelect,
			ClassDetails annotatedClass,
			MetadataBuildingContext context) {
		final var definition = NamedNativeSelectionDefinitionImpl.from( name, sqlSelect, annotatedClass, context );
		context.getMetadataCollector().addNamedNativeQuery( definition );
	}

	public static void bindNativeQuery(
			org.hibernate.annotations.NamedNativeQuery namedNativeQuery,
			MetadataBuildingContext context,
			AnnotationTarget location) {
		if ( namedNativeQuery == null ) {
			return;
		}

		final String registrationName = namedNativeQuery.name();

		if ( registrationName.isBlank() ) {
			throw new AnnotationException(
					"Class or package level '@NamedNativeQuery' annotation must specify a 'name'" );
		}

		if ( BOOT_LOGGER.isTraceEnabled() ) {
			BOOT_LOGGER.bindingNamedNativeQuery( registrationName,
					namedNativeQuery.query().replace( '\n', ' ' ) );
		}

		final var definition = NamedNativeSelectionDefinitionImpl.from( namedNativeQuery, location );
		context.getMetadataCollector().addNamedNativeQuery( definition );
	}

//	private static CacheMode getCacheMode(CacheRetrieveMode cacheRetrieveMode, CacheStoreMode cacheStoreMode) {
//		final CacheMode cacheMode = CacheMode.fromJpaModes( cacheRetrieveMode, cacheStoreMode );
//		return cacheMode == null ? CacheMode.NORMAL : cacheMode;
//	}
//
//	private static <T> NativeQueryBuilder<T> createQueryDefinition(
//			org.hibernate.annotations.NamedNativeQuery namedNativeQuery,
//			String registrationName, String resultSetMappingName,
//			Class<T> resultClass,
//			int timeout,
//			int fetchSize,
//			HashSet<String> querySpaces,
//			AnnotationTarget location) {
//		return new NativeQueryBuilder<T>(registrationName, location)
//				.setSqlString(namedNativeQuery.query())
//				.setResultSetMappingName(resultSetMappingName)
//				.setResultClass(resultClass)
//				.setCacheable(namedNativeQuery.cacheable())
//				.setCacheRegion(nullIfEmpty(namedNativeQuery.cacheRegion()))
//				.setCacheMode(getCacheMode(namedNativeQuery.cacheRetrieveMode(), namedNativeQuery.cacheStoreMode()))
//				.setTimeout(timeout < 0 ? null : Timeout.seconds( timeout ) )
//				.setFetchSize(fetchSize < 0 ? null : fetchSize)
//				.setFlushMode(namedNativeQuery.flush())
//				.setReadOnly(namedNativeQuery.readOnly())
//				.setQuerySpaces(querySpaces)
//				.setComment(nullIfEmpty(namedNativeQuery.comment()));
//	}

	/**
	 * Handles legacy cases where a named native query was used to specify a procedure call
	 *
	 * @deprecated User should use {@linkplain NamedStoredProcedureQuery} instead
	 */
	@Deprecated
	public static NamedProcedureCallDefinition createStoredProcedure(
			NativeQueryBuilder<?> builder,
			MetadataBuildingContext context,
			Supplier<RuntimeException> exceptionProducer) {
		final String sqlString = builder.getSqlString().trim();
		if ( !sqlString.startsWith( "{" ) || !sqlString.endsWith( "}" ) ) {
			throw exceptionProducer.get();
		}
		final JdbcCall jdbcCall = parseJdbcCall( sqlString, exceptionProducer );

		final var modelsContext = context.getBootstrapContext().getModelsContext();
		final var nameStoredProcedureQuery = JpaAnnotations.NAMED_STORED_PROCEDURE_QUERY.createUsage( modelsContext );
		nameStoredProcedureQuery.name( builder.getName() );
		nameStoredProcedureQuery.procedureName( jdbcCall.callableName );
		nameStoredProcedureQuery.parameters( parametersAsAnnotations( builder, context, jdbcCall ) );

		final String resultSetMappingName = builder.getResultSetMappingName();
		if ( resultSetMappingName != null ) {
			nameStoredProcedureQuery.resultSetMappings( new String[] {resultSetMappingName} );
		}

		final var resultClass = builder.getResultClass();
		if ( resultClass != null ) {
			nameStoredProcedureQuery.resultClasses( new Class[]{ builder.getResultClass() } );
		}

		final var hints = hintsAsAnnotations( builder, modelsContext, jdbcCall );
		nameStoredProcedureQuery.hints( hints.toArray(QueryHint[]::new) );

		return new NamedProcedureCallDefinitionImpl( nameStoredProcedureQuery );
	}

	private static StoredProcedureParameter[] parametersAsAnnotations(
			NativeQueryBuilder<?> builder, MetadataBuildingContext context, JdbcCall jdbcCall) {
		final var modelsContext = context.getBootstrapContext().getModelsContext();
		final var parameters = new StoredProcedureParameter[jdbcCall.parameters.size()];
		for ( int i = 0; i < jdbcCall.parameters.size(); i++ ) {
			final var param = JpaAnnotations.STORED_PROCEDURE_PARAMETER.createUsage( modelsContext );
			parameters[i] = param;

			final String paramName = jdbcCall.parameters.get( i );
			param.name( paramName );
			param.mode( ParameterMode.IN );

			final String typeName = builder.getParameterTypes().get( paramName );
			final var classDetails =
					isEmpty( typeName )
							? ClassDetails.VOID_CLASS_DETAILS
							: classDetails( context, typeName );
			param.type( classDetails.toJavaClass() );
		}
		return parameters;
	}

	private static List<QueryHint> hintsAsAnnotations(
			NativeQueryBuilder<?> builder, ModelsContext modelsContext, JdbcCall jdbcCall) {
		final List<QueryHint> hints = new ArrayList<>();
		if ( builder.getQuerySpaces() != null ) {
			final var hint = JpaAnnotations.QUERY_HINT.createUsage( modelsContext );
			hint.name( HibernateHints.HINT_NATIVE_SPACES );
			hint.value( String.join( " ", builder.getQuerySpaces() ) );
			hints.add( hint );
		}
		if ( jdbcCall.resultParameter ) {
			// Mark native queries that have a result parameter as callable functions
			final var hint = JpaAnnotations.QUERY_HINT.createUsage( modelsContext );
			hint.name( HibernateHints.HINT_CALLABLE_FUNCTION );
			hint.value( "true" );
			hints.add( hint );
		}
		return hints;
	}

	private static ClassDetails classDetails(MetadataBuildingContext context, String typeName) {
		final String registeredTypeName =
				context.getBootstrapContext().getTypeConfiguration().getBasicTypeRegistry()
						.getRegisteredType( typeName ).getJavaType().getName();
		return context.getMetadataCollector().getClassDetailsRegistry()
				.getClassDetails( registeredTypeName );
	}

	public static void bindQuery(
			String name,
			HQLSelect hqlSelect,
			MetadataBuildingContext context) {
		final var definition = NamedHqlSelectionDefinitionImpl.from( name, hqlSelect, null );
		context.getMetadataCollector().addNamedQuery( definition );
	}

	public static void bindQuery(
			org.hibernate.annotations.NamedQuery namedQuery,
			MetadataBuildingContext context,
			AnnotationTarget location) {
		if ( namedQuery == null ) {
			return;
		}

		final String registrationName = namedQuery.name();
		if ( registrationName.isBlank() ) {
			throw new AnnotationException( "Class or package level '@NamedQuery' annotation must specify a 'name'" );
		}

		if ( BOOT_LOGGER.isTraceEnabled() ) {
			BOOT_LOGGER.bindingNamedQuery( registrationName,
					namedQuery.query().replace( '\n', ' ' ) );
		}

		final var definition = NamedHqlSelectionDefinitionImpl.from( namedQuery, location );
		context.getMetadataCollector().addNamedQuery( definition );
	}

	public static void bindNamedStoredProcedureQuery(
			NamedStoredProcedureQuery namedStoredProcedureQuery,
			MetadataBuildingContext context,
			boolean isDefault) {
		if ( namedStoredProcedureQuery != null ) {
			if ( namedStoredProcedureQuery.name().isBlank() ) {
				throw new AnnotationException( "Class or package level '@NamedStoredProcedureQuery' annotation must specify a 'name'" );
			}

			final var definition = new NamedProcedureCallDefinitionImpl( namedStoredProcedureQuery );
			final var collector = context.getMetadataCollector();
			if ( isDefault ) {
				collector.addDefaultNamedProcedureCall( definition );
			}
			else {
				collector.addNamedProcedureCallDefinition( definition );
			}
			BOOT_LOGGER.boundStoredProcedureQuery(
					definition.getRegistrationName(),
					definition.getProcedureName() );
		}
	}

	public static void bindSqlResultSetMapping(
			SqlResultSetMapping resultSetMappingAnn,
			MetadataBuildingContext context,
			boolean isDefault) {
		//no need to handle inSecondPass
		context.getMetadataCollector()
				.addSecondPass( new ResultSetMappingSecondPass( resultSetMappingAnn, context, isDefault ) );
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
			if ( c == '(' || isWhitespace( c ) ) {
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
			if ( !isWhitespace( sqlString.charAt( i ) ) ) {
				break;
			}
			i++;
		}
		return i;
	}
}
