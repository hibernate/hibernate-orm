/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import jakarta.annotation.Nullable;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedNativeStatement;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStatement;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Timeout;
import jakarta.persistence.query.JakartaQuery;
import jakarta.persistence.query.NativeQuery;
import jakarta.persistence.query.QueryOptions;
import org.hibernate.AnnotationException;
import org.hibernate.CacheMode;
import org.hibernate.LockMode;
import org.hibernate.annotations.HQLSelect;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.boot.query.internal.NamedHqlMutationDefinitionImpl;
import org.hibernate.boot.query.internal.NamedHqlSelectionDefinitionImpl;
import org.hibernate.boot.query.internal.NamedNativeMutationDefinitionImpl;
import org.hibernate.boot.query.internal.NamedNativeSelectionDefinitionImpl;
import org.hibernate.boot.query.internal.NamedProcedureCallDefinitionImpl;
import org.hibernate.boot.query.SqlResultSetMappingDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.query.hql.internal.HqlHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.Character.isWhitespace;
import static org.hibernate.boot.BootLogging.BOOT_LOGGER;
import static org.hibernate.boot.query.internal.Helper.extractHints;
import static org.hibernate.internal.util.GenericsHelper.actualInheritedMemberType;
import static org.hibernate.internal.util.PrimitiveHelper.boxedType;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_OBJECT_ARRAY;

/**
 * Responsible for reading named queries defined in annotations and registering
 * {@link org.hibernate.boot.query.NamedQueryDefinition} objects.
 *
 * @implNote This class is stateless, unlike most of the other "binders".
 *
 * @author Emmanuel Bernard
 */
public abstract class QueryBinder {
	private static final String JAKARTA_DATA_REPOSITORY = "jakarta.data.repository.Repository";
	private static final String JAKARTA_DATA_QUERY = "jakarta.data.repository.Query";
	private static final String JAKARTA_DATA_PAGE = "jakarta.data.page.Page";
	private static final String JAKARTA_DATA_CURSORED_PAGE = "jakarta.data.page.CursoredPage";

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

				final var mappingDefinition =
						SqlResultSetMappingDescriptor.from(
								namedNativeQuery,
								location == null ? null : location.getName()
						);
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
		final var processedMethods = new HashSet<MethodSignature>();
		bindDeclaredStaticQueries( classDetails, classDetails, context, modelsContext, processedMethods );
		if ( isJakartaDataRepository( classDetails ) ) {
			bindInheritedRepositoryStaticQueries(
					classDetails,
					classDetails,
					context,
					modelsContext,
					processedMethods,
					new HashSet<>()
			);
		}
	}

	private static void bindDeclaredStaticQueries(
			ClassDetails registrationClassDetails,
			ClassDetails methodClassDetails,
			MetadataBuildingContext context,
			ModelsContext modelsContext,
			Set<MethodSignature> processedMethods) {
		for ( var methodDetails : methodClassDetails.getMethods() ) {
			processedMethods.add( MethodSignature.of( methodDetails ) );
			bindStaticQuery( registrationClassDetails, methodDetails, context, modelsContext );
			bindStaticNativeQuery( registrationClassDetails, methodDetails, context, modelsContext );
		}
	}

	private static boolean isJakartaDataRepository(ClassDetails classDetails) {
		return classDetails.isInterface()
			&& hasDirectAnnotation( classDetails, JAKARTA_DATA_REPOSITORY );
	}

	private static boolean hasDirectAnnotation(AnnotationTarget annotationTarget, String annotationTypeName) {
		for ( var annotation : annotationTarget.getDirectAnnotationUsages() ) {
			if ( annotationTypeName.equals( annotation.annotationType().getName() ) ) {
				return true;
			}
		}
		return false;
	}

	private static void bindInheritedRepositoryStaticQueries(
			ClassDetails repositoryClassDetails,
			ClassDetails classDetails,
			MetadataBuildingContext context,
			ModelsContext modelsContext,
			Set<MethodSignature> processedMethods,
			Set<String> processedTypes) {
		if ( classDetails != null
				&& classDetails != ClassDetails.OBJECT_CLASS_DETAILS
				&& processedTypes.add( classDetails.getName() ) ) {
			for ( var methodDetails : classDetails.getMethods() ) {
				if ( processedMethods.add( MethodSignature.of( methodDetails ) ) ) {
					bindStaticQuery( repositoryClassDetails, methodDetails, context, modelsContext );
					bindStaticNativeQuery( repositoryClassDetails, methodDetails, context, modelsContext );
				}
			}

			for ( var implementedInterface : classDetails.getImplementedInterfaces() ) {
				bindInheritedRepositoryStaticQueries(
						repositoryClassDetails,
						implementedInterface.determineRawClass(),
						context,
						modelsContext,
						processedMethods,
						processedTypes
				);
			}

			bindInheritedRepositoryStaticQueries(
					repositoryClassDetails,
					classDetails.getSuperClass(),
					context,
					modelsContext,
					processedMethods,
					processedTypes
			);
		}
	}

	private record MethodSignature(String name, List<String> argumentTypes) {
		private static MethodSignature of(MethodDetails methodDetails) {
			return new MethodSignature(
					methodDetails.getName(),
					methodDetails.getArgumentTypes().stream()
							.map( ClassDetails::getName )
							.toList()
			);
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
				final var resultType = staticSelectionResultType( classDetails, methodDetails );
				final var definition = new NamedHqlSelectionDefinitionImpl<>(
						registrationName,
						staticQueryLocation( classDetails, methodDetails ),
						staticSelectionQueryString( queryString, resultType, classDetails ),
						resultType,
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

	private static String staticSelectionQueryString(String queryString, Class<?> resultType,
			ClassDetails classDetails) {
		final String entityName = entityName( resultType );
		final String resolvedEntityName = entityName != null ? entityName : repositoryEntityName( classDetails );
		return HqlHelper.addFromClauseIfNecessary( queryString, resolvedEntityName );
	}

	private static final Set<String> DATA_REPOSITORY_SUPERTYPES = Set.of(
			"jakarta.data.repository.DataRepository",
			"jakarta.data.repository.BasicRepository",
			"jakarta.data.repository.CrudRepository"
	);

	private static final Set<String> LIFECYCLE_ANNOTATIONS = Set.of(
			"jakarta.data.repository.Insert",
			"jakarta.data.repository.Delete",
			"jakarta.data.repository.Save"
	);

	private static @Nullable String repositoryEntityName(ClassDetails classDetails) {
		final var result = repositoryEntityNameFromTypeHierarchy( classDetails );
		return result != null ? result : repositoryEntityNameFromLifecycleMethods( classDetails );
	}

	private static @Nullable String repositoryEntityNameFromTypeHierarchy(ClassDetails classDetails) {
		for ( var implementedInterface : classDetails.getImplementedInterfaces() ) {
			final var rawClass = implementedInterface.determineRawClass();
			if ( implementedInterface.getTypeKind() == TypeDetails.Kind.PARAMETERIZED_TYPE ) {
				final var parameterized = implementedInterface.asParameterizedType();
				if ( parameterized.getArguments().size() == 2
						&& DATA_REPOSITORY_SUPERTYPES.contains( rawClass.getName() ) ) {
					return entityName( parameterized.getArguments().get( 0 ).determineRawClass().toJavaClass() );
				}
			}
			// recurse into super interfaces, including companion ($)
			// interfaces which extend the main repository without
			// type arguments
			final var result = repositoryEntityNameFromTypeHierarchy( rawClass );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	private static @Nullable String repositoryEntityNameFromLifecycleMethods(ClassDetails classDetails) {
		for ( var methodDetails : classDetails.getMethods() ) {
			if ( isLifecycleMethod( methodDetails ) ) {
				for ( var argumentType : methodDetails.getArgumentTypes() ) {
					final var name = entityName( argumentType.toJavaClass() );
					if ( name != null ) {
						return name;
					}
					// List<Entity> or other Iterable<Entity> parameter
					if ( argumentType.isImplementor( Iterable.class ) ) {
						final var method = methodDetails.toJavaMember();
						for ( var paramType : method.getGenericParameterTypes() ) {
							if ( paramType instanceof ParameterizedType parameterizedType
									&& parameterizedType.getActualTypeArguments().length == 1 ) {
								final var elementType = erasedClass( parameterizedType.getActualTypeArguments()[0] );
								final var elementName = entityName( elementType );
								if ( elementName != null ) {
									return elementName;
								}
							}
						}
					}
				}
			}
		}
		// check super interfaces (e.g. companion extends main repository)
		for ( var implementedInterface : classDetails.getImplementedInterfaces() ) {
			final var result = repositoryEntityNameFromLifecycleMethods( implementedInterface.determineRawClass() );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	private static boolean isLifecycleMethod(MethodDetails methodDetails) {
		for ( var annotation : methodDetails.getDirectAnnotationUsages() ) {
			if ( LIFECYCLE_ANNOTATIONS.contains( annotation.annotationType().getName() ) ) {
				return true;
			}
		}
		return false;
	}

	private static String entityName(Class<?> resultType) {
		final var entity = resultType.getAnnotation( Entity.class );
		if ( entity == null ) {
			return null;
		}
		else {
			final String explicitName = entity.name();
			return explicitName.isBlank() ? resultType.getSimpleName() : explicitName;
		}
	}

	private static String staticQueryString(MethodDetails methodDetails, ModelsContext modelsContext) {
		final var jakartaQuery = methodDetails.getAnnotationUsage( JakartaQuery.class, modelsContext );
		if ( jakartaQuery != null ) {
			return jakartaQuery.value();
		}
		else {
			for ( var annotation : methodDetails.getDirectAnnotationUsages() ) {
				if ( JAKARTA_DATA_QUERY.equals( annotation.annotationType().getName() ) ) {
					return annotationValue( annotation );
				}
			}
			return null;
		}
	}

	private static String annotationValue(Annotation annotation) {
		try {
			return (String)
					annotation.annotationType()
							.getMethod( "value" )
							.invoke( annotation );
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
		final var query = methodDetails.getAnnotationUsage( NativeQuery.class, modelsContext );
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
				final var definition = new NamedNativeSelectionDefinitionImpl<>(
						registrationName,
						staticQueryLocation( classDetails, methodDetails ),
						query.value(),
						staticSelectionResultType( classDetails, methodDetails ),
						bindStaticNativeQueryResultSetMapping( registrationName, methodDetails, context, modelsContext ),
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
					SqlResultSetMappingDescriptor.from(
							registrationName,
							entityResults,
							constructorResults,
							columnResults,
							staticQueryLocation( methodDetails.getDeclaringType(), methodDetails )
					)
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

	private static Class<?> staticSelectionResultType(ClassDetails classDetails, MethodDetails methodDetails) {
		final var returnType = methodDetails.getReturnType();
		final var returnClass = returnType.toJavaClass();
		if ( returnClass.isArray() ) {
			return staticArraySelectionResultType( classDetails, methodDetails, returnClass );
		}
		else if ( isStaticQueryContainerType( returnType ) ) {
			return erasedClass( firstTypeArgument( classDetails, methodDetails ) );
		}
		else {
			return boxedType( erasedClass( genericReturnType( classDetails, methodDetails ) ) );
		}
	}

	private static Class<?> staticArraySelectionResultType(
			ClassDetails classDetails,
			MethodDetails methodDetails,
			Class<?> returnClass) {
		final var componentType = returnClass.getComponentType();
		if ( componentType != Object.class ) {
			return boxedType( componentType );
		}
		else {
			return genericReturnType( classDetails, methodDetails )
						instanceof GenericArrayType genericArrayType
					? boxedType( erasedClass( genericArrayType.getGenericComponentType() ) )
					: returnClass;
		}
	}

	private static boolean isStaticQueryContainerType(ClassDetails returnType) {
		return returnType.isImplementor( List.class )
			|| returnType.isImplementor( Stream.class )
			|| returnType.isImplementor( Optional.class )
			|| returnType.isImplementor( jakarta.persistence.Query.class )
			|| returnType.isImplementor( jakarta.persistence.TypedQuery.class )
			|| returnType.isImplementor( org.hibernate.query.Query.class )
			|| returnType.isImplementor( org.hibernate.query.SelectionQuery.class )
			|| returnType.isImplementor( org.hibernate.query.KeyedResultList.class )
			|| isJakartaDataQueryContainerType( returnType.toJavaClass().getName() );
	}

	private static boolean isJakartaDataQueryContainerType(String typeName) {
		return JAKARTA_DATA_PAGE.equals( typeName )
			|| JAKARTA_DATA_CURSORED_PAGE.equals( typeName );
	}

	private static Type firstTypeArgument(ClassDetails classDetails, MethodDetails methodDetails) {
		if ( genericReturnType( classDetails, methodDetails )
				instanceof ParameterizedType parameterizedType ) {
			var actualTypeArguments = parameterizedType.getActualTypeArguments();
			if ( actualTypeArguments.length == 1 ) {
				return actualTypeArguments[0];
			}
		}
		return Object.class;
	}

	private static Type genericReturnType(ClassDetails classDetails, MethodDetails methodDetails) {
		final var method = methodDetails.toJavaMember();
		final var containerClass = classDetails.toJavaClass();
		return method.getDeclaringClass() == containerClass
				? method.getGenericReturnType()
				: actualInheritedMemberType( containerClass, method );
	}

	private static Class<?> erasedClass(Type type) {
		if ( type instanceof Class<?> typeClass ) {
			return typeClass;
		}
		else if ( type instanceof ParameterizedType parameterizedType ) {
			return erasedClass( parameterizedType.getRawType() );
		}
		else if ( type instanceof GenericArrayType genericArrayType ) {
			return erasedClass( genericArrayType.getGenericComponentType() ).arrayType();
		}
		else if ( type instanceof WildcardType wildcardType ) {
			final var upperBounds = wildcardType.getUpperBounds();
			return upperBounds.length == 0 ? Object.class : erasedClass( upperBounds[0] );
		}
		else if ( type instanceof TypeVariable<?> typeVariable ) {
			final var bounds = typeVariable.getBounds();
			return bounds.length == 0 ? Object.class : erasedClass( bounds[0] );
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
		final var name =
				new StringBuilder( javadocTypeName( classDetails ) )
						.append( '#' )
						.append( methodDetails.getName() )
						.append( '(' );
		final var argumentTypes = methodDetails.getArgumentTypes();
		for ( int i = 0; i < argumentTypes.size(); i++ ) {
			if ( i > 0 ) {
				name.append( ',' );
			}
			name.append( javadocTypeName( argumentTypes.get( i ) ) );
		}
		return name.append( ')' ).toString();
	}

	private static String javadocTypeName(ClassDetails classDetails) {
		final var classDetailsClassName = classDetails.getClassName();
		final String className =
				classDetailsClassName == null
						? classDetails.getName()
						: classDetailsClassName;
		// JVM array descriptor for varargs, e.g. "[Ljakarta.data.Sort;"
		// The annotation processor uses the component type without "[]"
		if ( className.startsWith( "[L" ) && className.endsWith( ";" ) ) {
			// cannot be a companion, just return:
			return className.substring( 2, className.length() - 1 );
		}
		return javadocName( className );
	}

	private static String javadocName(String className) {
		return className.endsWith( "$" )
				? className
				: className.replace( '$', '.' );
	}

	private static String staticQueryLocation(ClassDetails classDetails, MethodDetails methodDetails) {
		return classDetails.getName() + "#" + methodDetails.getName();
	}

	private static QueryFlushMode queryFlushMode(QueryOptions options) {
		if ( options == null || options.flush() == QueryFlushMode.DEFAULT ) {
			return QueryFlushMode.DEFAULT;
		}
		else {
			return options.flush();
		}
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
		if ( namedNativeQuery != null ) {
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
		if ( namedQuery != null ) {
			final String registrationName = namedQuery.name();
			if ( registrationName.isBlank() ) {
				throw new AnnotationException(
						"Class or package level '@NamedQuery' annotation must specify a 'name'" );
			}

			if ( BOOT_LOGGER.isTraceEnabled() ) {
				BOOT_LOGGER.bindingNamedQuery( registrationName,
						namedQuery.query().replace( '\n', ' ' ) );
			}

			final var definition = NamedHqlSelectionDefinitionImpl.from( namedQuery, location );
			context.getMetadataCollector().addNamedQuery( definition );
		}
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
		bindSqlResultSetMapping( resultSetMappingAnn, context, null, isDefault );
	}

	public static void bindSqlResultSetMapping(
			SqlResultSetMapping resultSetMappingAnn,
			MetadataBuildingContext context,
			AnnotationTarget location,
			boolean isDefault) {
		//no need to handle inSecondPass
		context.getMetadataCollector()
				.addSecondPass( new ResultSetMappingSecondPass( resultSetMappingAnn, context, location, isDefault ) );
	}

}
