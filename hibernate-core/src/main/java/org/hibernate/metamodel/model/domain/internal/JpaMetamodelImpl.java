/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.query.NamedQueryDefinition;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.internal.JpaMetamodelPopulationSetting;
import org.hibernate.metamodel.internal.JpaStaticMetamodelPopulationSetting;
import org.hibernate.metamodel.internal.MetadataContext;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.MappedSuperclassDomainType;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.EntityTypeException;
import org.hibernate.query.sqm.tree.domain.SqmPolymorphicRootDescriptor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.DynamicModelJavaType;
import org.hibernate.type.descriptor.java.spi.EntityJavaType;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Type;

import static java.util.Collections.emptySet;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.metamodel.internal.InjectionHelper.injectEntityGraph;
import static org.hibernate.metamodel.internal.InjectionHelper.injectTypedQueryReference;

/**
 * @author Steve Ebersole
 */
public class JpaMetamodelImpl implements JpaMetamodelImplementor, Serializable {

	private static class ImportInfo {
		private final String importedName;
		private Class<?> loadedClass; // could be null for boot metamodel import; not final to allow for populating later

		private ImportInfo(String importedName, Class<?> loadedClass) {
			this.importedName = importedName;
			this.loadedClass = loadedClass;
		}
	}

	private final TypeConfiguration typeConfiguration;
	private final MappingMetamodel mappingMetamodel;
	private final ServiceRegistry serviceRegistry;
	private final ClassLoaderService classLoaderService;

	private final Map<String, ManagedDomainType<?>> managedTypeByName = new TreeMap<>();
	private final Map<Class<?>, ManagedDomainType<?>> managedTypeByClass = new HashMap<>();
	private JpaMetamodelPopulationSetting jpaMetaModelPopulationSetting;
	private final Map<String, Set<String>> allowedEnumLiteralsToEnumTypeNames = new HashMap<>();
	private final Map<String, EnumJavaType<?>> enumJavaTypes = new HashMap<>();

	private final transient Map<String, RootGraphImplementor<?>> entityGraphMap = new ConcurrentHashMap<>();

	private final Map<Class<?>, SqmPolymorphicRootDescriptor<?>> polymorphicEntityReferenceMap = new ConcurrentHashMap<>();

	private final Map<Class<?>, String> entityProxyInterfaceMap = new HashMap<>();

	private final Map<String, ImportInfo> nameToImportMap = new ConcurrentHashMap<>();
	private final Map<String, Object> knownInvalidnameToImportMap = new ConcurrentHashMap<>();


	public JpaMetamodelImpl(
			TypeConfiguration typeConfiguration,
			MappingMetamodel mappingMetamodel,
			ServiceRegistry serviceRegistry) {
		this.typeConfiguration = typeConfiguration;
		this.mappingMetamodel = mappingMetamodel;
		this.serviceRegistry = serviceRegistry;
		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
	}

	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public @Nullable ManagedDomainType<?> findManagedType(@Nullable String typeName) {
		return typeName == null ? null : managedTypeByName.get( typeName );
	}

	@Override
	public ManagedDomainType<?> managedType(String typeName) {
		final var managedType = findManagedType( typeName );
		if ( managedType == null ) {
			throw new IllegalArgumentException( "Not a managed type: " + typeName );
		}
		return managedType;
	}

	@Override
	@Nullable
	public EntityDomainType<?> findEntityType(@Nullable String entityName) {
		if ( entityName == null ) {
			return null;
		}

		if ( managedTypeByName.get( entityName )
				instanceof EntityDomainType<?> entityDomainType ){
			return entityDomainType;
		}

		// NOTE: `managedTypeByName` is keyed by Hibernate entity name.
		// If there is a direct match based on key, we want that one - see above.
		// However, the JPA contract for `#entity` is to match `@Entity(name)`; if there
		// was no direct match, we need to iterate over all of the and look based on
		// JPA entity-name.

		for ( var entry : managedTypeByName.entrySet() ) {
			if ( entry.getValue() instanceof EntityDomainType<?> possibility ) {
				if ( entityName.equals( possibility.getName() ) ) {
					return possibility;
				}
			}
		}

		return null;
	}

	@Override
	public EntityDomainType<?> entity(String entityName) {
		final EntityDomainType<?> entityType = findEntityType( entityName );
		if ( entityType == null ) {
			// per JPA, this is an exception
			throw new IllegalArgumentException( "Not an entity: " + entityName );
		}
		return entityType;

	}

	@Override
	@Nullable
	public EmbeddableDomainType<?> findEmbeddableType(@Nullable String embeddableName) {
		if ( embeddableName == null ) {
			return null;
		}
		final var managedType = managedTypeByName.get( embeddableName );
		if ( !( managedType instanceof EmbeddableDomainType<?> embeddableDomainType ) ) {
			return null;
		}
		return embeddableDomainType;
	}

	@Override
	public EmbeddableDomainType<?> embeddable(String embeddableName) {
		final var embeddableType = findEmbeddableType( embeddableName );
		if ( embeddableType == null ) {
			throw new IllegalArgumentException( "Not an embeddable: " + embeddableName );
		}
		return embeddableType;
	}

	@Override
	public EntityDomainType<?> getHqlEntityReference(String entityName) {
		Class<?> loadedClass = null;
		final var importInfo = resolveImport( entityName );
		if ( importInfo != null ) {
			loadedClass = importInfo.loadedClass;
			entityName = importInfo.importedName;
		}

		final var entityDescriptor = findEntityType( entityName );
		if ( entityDescriptor != null ) {
			return entityDescriptor;
		}

		if ( loadedClass == null ) {
			loadedClass = resolveRequestedClass( entityName );
			// populate the class cache for boot metamodel imports
			if ( importInfo != null && loadedClass != null ) {
				importInfo.loadedClass = loadedClass;
			}
		}
		if ( loadedClass != null ) {
			return resolveEntityReference( loadedClass );
		}
		return null;
	}

	@Override
	public EntityDomainType<?> resolveHqlEntityReference(String entityName) {
		final var hqlEntityReference = getHqlEntityReference( entityName );
		if ( hqlEntityReference == null ) {
			throw new EntityTypeException( "Could not resolve entity name '" + entityName + "'", entityName );
		}
		return hqlEntityReference;
	}

	private static <X> ManagedDomainType<X> checkDomainType(Class<X> cls, ManagedDomainType<?> domainType) {
		if ( domainType != null && !Objects.equals( domainType.getJavaType(), cls ) ) {
			throw new IllegalStateException( "Managed type " + domainType
						+ " has a different Java type than requested" );
		}
		else {
			@SuppressWarnings("unchecked") // Safe, we checked it
			final var type = (ManagedDomainType<X>) domainType;
			return type;
		}
	}

	@Override
	@Nullable
	public <X> ManagedDomainType<X> findManagedType(Class<X> cls) {
		return checkDomainType( cls, managedTypeByClass.get( cls ) );
	}

	@Override
	public <X> ManagedDomainType<X> managedType(Class<X> cls) {
		final var type = findManagedType( cls );
		if ( type == null ) {
			// per JPA
			throw new IllegalArgumentException( "Not a managed type: " + cls );
		}
		return type;
	}

	@Override
	@Nullable
	public <X> EntityDomainType<X> findEntityType(Class<X> cls) {
		return checkDomainType( cls, managedTypeByClass.get( cls ) )
					instanceof EntityDomainType<X> entityDomainType
				? entityDomainType
				: null;
	}

	@Override
	public <X> EntityDomainType<X> entity(Class<X> cls) {
		final var entityType = findEntityType( cls );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "Not an entity: " + cls.getName() );
		}
		return entityType;
	}

	@Override
	public @Nullable <X> EmbeddableDomainType<X> findEmbeddableType(Class<X> cls) {
		return checkDomainType( cls, managedTypeByClass.get( cls ) )
					instanceof EmbeddableDomainType<X> embeddableDomainType
				? embeddableDomainType
				: null;
	}

	@Override
	public <X> EmbeddableDomainType<X> embeddable(Class<X> cls) {
		final var embeddableType = findEmbeddableType( cls );
		if ( embeddableType == null ) {
			throw new IllegalArgumentException( "Not an embeddable: " + cls.getName() );
		}
		return embeddableType;
	}

	private Collection<ManagedDomainType<?>> getAllManagedTypes() {
		// should never happen
		return switch ( jpaMetaModelPopulationSetting ) {
			case IGNORE_UNSUPPORTED -> managedTypeByClass.values();
			case ENABLED -> managedTypeByName.values();
			case DISABLED -> emptySet();
		};
	}

	@Override
	public Set<ManagedType<?>> getManagedTypes() {
		return new HashSet<>( getAllManagedTypes() );
	}

	@Override
	public Set<EntityType<?>> getEntities() {
		return getAllManagedTypes().stream()
				.filter( EntityType.class::isInstance )
				.map( t -> (EntityType<?>) t )
				.collect( Collectors.toSet() );
	}

	@Override
	public Set<EmbeddableType<?>> getEmbeddables() {
		return getAllManagedTypes().stream()
				.filter( EmbeddableType.class::isInstance )
				.map( t -> (EmbeddableType<?>) t )
				.collect( Collectors.toSet() );
	}

	@Override
	public @Nullable Set<String> getEnumTypesForValue(String enumValue) {
		return allowedEnumLiteralsToEnumTypeNames.get( enumValue );
	}

	@Override
	public EnumJavaType<?> getEnumType(String className) {
		final var enumJavaType = enumJavaTypes.get( className );
		if ( enumJavaType != null ) {
			return enumJavaType;
		}
		else {
			try {
				final Class<?> clazz = classLoaderService.classForName( className );
				if ( clazz == null || !clazz.isEnum() ) {
					return null;
				}
				//noinspection rawtypes,unchecked
				return new EnumJavaType( clazz );
			}
			catch (ClassLoadingException e) {
				throw new RuntimeException( e );
			}
		}
	}

	@Override
	public <E extends Enum<E>> E enumValue(EnumJavaType<E> enumType, String enumValueName) {
		return Enum.valueOf( enumType.getJavaTypeClass(), enumValueName );
	}

	@Override
	public JavaType<?> getJavaConstantType(String className, String fieldName) {
		try {
			final var referencedField = getJavaField( className, fieldName );
			if ( referencedField != null ) {
				return getTypeConfiguration().getJavaTypeRegistry()
						.resolveDescriptor( referencedField.getType() );
			}
		}
		catch (NoSuchFieldException e) {
			// ignore
		}
		return null;
	}

	@Override
	public <T> T getJavaConstant(String className, String fieldName) {
		try {
			final var referencedField = getJavaField( className, fieldName );
			//noinspection unchecked
			return (T) referencedField.get( null );
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException( e );
		}
	}

	private Field getJavaField(String className, String fieldName) throws NoSuchFieldException {
		final Class<?> namedClass = classLoaderService.classForName( className );
		if ( namedClass != null ) {
			return namedClass.getDeclaredField( fieldName );
		}
		return null;
	}

	@Override
	public void addNamedEntityGraph(String graphName, RootGraphImplementor<?> rootGraph) {
		final var old = entityGraphMap.put( graphName, rootGraph.makeImmutableCopy( graphName ) );
		if ( old != null ) {
			CORE_LOGGER.tracef( "EntityGraph named '%s' was replaced", graphName );
		}
	}

	@Override
	public RootGraphImplementor<?> findEntityGraphByName(String name) {
		return entityGraphMap.get( name );
	}

	@Override
	public <T> List<EntityGraph<? super T>> findEntityGraphsByJavaType(Class<T> entityClass) {
		final var entityType = entity( entityClass );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "Given class is not an entity: " + entityClass.getName() );
		}
		else {
			final List<EntityGraph<? super T>> results = new ArrayList<>();
			for ( var entityGraph : entityGraphMap.values() ) {
				if ( entityGraph.appliesTo( entityType ) ) {
					@SuppressWarnings("unchecked") // safe, we just checked
					var result = (RootGraphImplementor<? super T>) entityGraph;
					results.add( result );
				}
			}
			return results;
		}
	}

	@Override
	public <T> Map<String, EntityGraph<? extends T>> getNamedEntityGraphs(Class<T> entityClass) {
		final var entityType = entity( entityClass );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "Given class is not an entity: " + entityClass.getName() );
		}
		else {
			final Map<String, EntityGraph<? extends T>> results = new HashMap<>();
			for ( var entityGraph : entityGraphMap.values() ) {
				if ( entityGraph.appliesTo( entityType ) ) {
					@SuppressWarnings("unchecked") // safe, we just checked
					var graph = (EntityGraph<? extends T>) entityGraph;
					results.put( entityGraph.getName(), graph );
				}
			}
			return results;
		}
	}

	@Override
	public String qualifyImportableName(String queryName) {
		final var importInfo = resolveImport( queryName );
		return importInfo == null ? null : importInfo.importedName;
	}

	private ImportInfo resolveImport(final String name) {
		final var importInfo = nameToImportMap.get( name );
		//optimal path first
		if ( importInfo != null ) {
			return importInfo;
		}
		else {
			//then check the negative cache to avoid bothering the classloader unnecessarily
			if ( knownInvalidnameToImportMap.containsKey( name ) ) {
				return null;
			}
			else {
				// see if the name is a fully qualified class name
				final var loadedClass = resolveRequestedClass( name );
				if ( loadedClass == null ) {
					// it is NOT a fully qualified class name - add a marker entry, so we do not keep trying later
					// note that ConcurrentHashMap does not support null value so a marker entry is needed
					// [HHH-14948] But only add it if the cache size isn't getting too large, as in some use cases
					// the queries are dynamically generated and this cache could lead to memory leaks when left unbounded.
					if ( knownInvalidnameToImportMap.size() < 1_000 ) {
						//TODO this collection might benefit from a LRU eviction algorithm,
						//we currently have no evidence for this need but this could be explored further.
						//To consider that we don't have a hard dependency on a cache implementation providing LRU semantics.
						//Alternatively - even better - would be to precompute all possible valid options and
						//store them in nameToImportMap on bootstrap: if that can be filled with all (comprehensive)
						//valid values, then there is no need for ever bothering the classloader.
						knownInvalidnameToImportMap.put( name, name );
					}
					return null;
				}
				else {
					// it is a fully qualified class name - add it to the cache
					// so to not needing to load from the classloader again
					final var info = new ImportInfo( name, loadedClass );
					nameToImportMap.put( name, info );
					return info;
				}
			}
		}
	}

	private void applyNamedEntityGraphs(Collection<NamedEntityGraphDefinition> namedEntityGraphs) {
		for ( var definition : namedEntityGraphs ) {
			CORE_LOGGER.tracef( "Applying named entity graph [name=%s, source=%s]",
					definition.name(), definition.source() );

			final var graph = definition.graphCreator().createEntityGraph(
					entityClass -> {
						if ( managedTypeByClass.get( entityClass ) instanceof EntityDomainType<?> match ) {
							return match;
						}
						throw new IllegalArgumentException( "Cannot resolve entity class : " + entityClass.getName() );
					},
					jpaEntityName -> {
						for ( var entry : managedTypeByName.entrySet() ) {
							if ( entry.getValue() instanceof EntityDomainType<?> possibility
									&& jpaEntityName.equals( possibility.getName() ) ) {
								return possibility;
							}
						}
						throw new IllegalArgumentException( "Cannot resolve entity name : " + jpaEntityName );
					}
			);
			entityGraphMap.put( definition.name(), graph );
		}
	}


	private Class<?> resolveRequestedClass(String entityName) {
		try {
			return classLoaderService.classForName( entityName );
		}
		catch (ClassLoadingException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> EntityDomainType<T> resolveEntityReference(Class<T> javaType) {
		// try the incoming Java type as a "strict" entity reference
		{
			final var managedType = managedTypeByClass.get( javaType );
			if ( managedType instanceof EntityDomainType<?> ) {
				return (EntityDomainType<T>) managedType;
			}
		}

		// Next, try it as a proxy interface reference
		{
			final String proxyEntityName = entityProxyInterfaceMap.get( javaType );
			if ( proxyEntityName != null ) {
				return (EntityDomainType<T>) entity( proxyEntityName );
			}
		}

		// otherwise, try to handle it as a polymorphic reference
		{
			final var polymorphicDomainType =
					(EntityDomainType<T>)
							polymorphicEntityReferenceMap.get( javaType );
			if ( polymorphicDomainType != null ) {
				return polymorphicDomainType;
			}

			// create a set of descriptors that should be used to build the polymorphic EntityDomainType
			final Set<EntityDomainType<? extends T>> matchingDescriptors = new HashSet<>();
			for ( var managedType : managedTypeByName.values() ) {
				if ( managedType.getPersistenceType() == Type.PersistenceType.ENTITY
						// see if we should add EntityDomainType as one of the matching descriptors.
						&& javaType.isAssignableFrom( managedType.getJavaType() ) ) {
					// The queried type is assignable from the type of the current entity type.
					// We should add it to the collecting set of matching descriptors. It should
					// be added aside from a few cases...

					// If the managed type has a supertype and the java type is assignable from the super type,
					// do not add the managed type as the supertype itself will get added and the initializers
					// for entity mappings already handle loading subtypes - adding it would be redundant and
					// lead to incorrect results
					final var superType = managedType.getSuperType();
					if ( superType == null
							|| superType.getPersistenceType() != Type.PersistenceType.ENTITY
							|| !javaType.isAssignableFrom( superType.getJavaType() ) ) {
						matchingDescriptors.add( (EntityDomainType<? extends T>) managedType );
					}

				}
			}

			// if we found any matching, create the virtual root EntityDomainType reference
			if ( !matchingDescriptors.isEmpty() ) {
				final var polymorphicRootDescriptor = new SqmPolymorphicRootDescriptor<>(
						typeConfiguration.getJavaTypeRegistry().resolveDescriptor( javaType ),
						matchingDescriptors,
						this
				);
				polymorphicEntityReferenceMap.putIfAbsent( javaType, polymorphicRootDescriptor );
				return polymorphicRootDescriptor;
			}
		}

		throw new EntityTypeException(
				"Could not resolve entity class '" + javaType.getName() + "'",
				javaType.getName()
		);
	}

	@Override
	public MappingMetamodel getMappingMetamodel() {
		return mappingMetamodel;
	}

	public void processJpa(
			MetadataImplementor bootMetamodel,
			MappingMetamodel mappingMetamodel,
			Map<Class<?>, String> entityProxyInterfaceMap,
			JpaStaticMetamodelPopulationSetting jpaStaticMetaModelPopulationSetting,
			JpaMetamodelPopulationSetting jpaMetaModelPopulationSetting,
			Collection<NamedEntityGraphDefinition> namedEntityGraphDefinitions,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		bootMetamodel.getImports()
				.forEach( (key, value) -> nameToImportMap.put( key,
						new ImportInfo( value, null ) ) );
		this.entityProxyInterfaceMap.putAll( entityProxyInterfaceMap );

		final var context = new MetadataContext(
				this,
				mappingMetamodel,
				bootMetamodel,
				jpaStaticMetaModelPopulationSetting,
				jpaMetaModelPopulationSetting,
				runtimeModelCreationContext,
				runtimeModelCreationContext.getBootstrapContext().getClassLoaderService()
		);


		for ( var entityBinding : bootMetamodel.getEntityBindings() ) {
			locateOrBuildEntityType( entityBinding, context, typeConfiguration );
		}
		handleUnusedMappedSuperclasses( context, typeConfiguration );

		context.wrapUp();

		this.jpaMetaModelPopulationSetting = jpaMetaModelPopulationSetting;

		// Identifiable types (Entities and MappedSuperclasses)
		managedTypeByName.putAll( context.getIdentifiableTypesByName() );
		managedTypeByClass.putAll( context.getEntityTypeMap() );
		managedTypeByClass.putAll( context.getMappedSuperclassTypeMap() );

		// Embeddable types
		int mapEmbeddables = 0;
		for ( var embeddable : context.getEmbeddableTypeSet() ) {
			// Do not register the embeddable types for id classes
			if ( embeddable.getExpressibleJavaType() instanceof EntityJavaType<?> ) {
				continue;
			}
			final var embeddableClass = embeddable.getJavaType();
			if ( embeddableClass != Map.class ) {
				managedTypeByClass.put( embeddable.getJavaType(), embeddable );
				managedTypeByName.put( embeddable.getTypeName(), embeddable );
			}
			else {
				managedTypeByName.put( "dynamic-embeddable-" + mapEmbeddables++, embeddable );
			}
		}

		typeConfiguration.getJavaTypeRegistry().forEachDescriptor( descriptor -> {
			if ( descriptor instanceof EnumJavaType<? extends Enum<?>> enumJavaType ) {
				final var enumJavaClass = enumJavaType.getJavaTypeClass();
				for ( var enumConstant : enumJavaClass.getEnumConstants() ) {
					addAllowedEnumLiteralsToEnumTypesMap(
							allowedEnumLiteralsToEnumTypeNames,
							enumConstant.name(),
							enumJavaClass.getSimpleName(),
							enumJavaClass.getCanonicalName(),
							enumJavaClass.getName()
					);
					enumJavaTypes.put( enumJavaClass.getName(), enumJavaType );
					enumJavaTypes.put( enumJavaClass.getCanonicalName(), enumJavaType );
				}
			}
		} );

		applyNamedEntityGraphs( namedEntityGraphDefinitions );

		populateStaticMetamodel( bootMetamodel, context );
	}

	private void populateStaticMetamodel(MetadataImplementor bootMetamodel, MetadataContext context) {
		bootMetamodel.visitNamedHqlQueryDefinitions( definition
				-> injectTypedQueryReference( definition, namedQueryMetamodelClass( definition, context ) ) );
		bootMetamodel.visitNamedNativeQueryDefinitions( definition
				-> injectTypedQueryReference( definition, namedQueryMetamodelClass( definition, context ) ) );
		bootMetamodel.getNamedEntityGraphs().values().stream().filter( (definition) -> definition.entityName() != null )
				.forEach( definition -> injectEntityGraph( definition, graphMetamodelClass( definition, context ), this ) );
	}

	private Class<?> namedQueryMetamodelClass(NamedQueryDefinition<?> definition, MetadataContext context) {
		final String location = definition.getLocation();
		return location == null ? null : context.metamodelClass( managedTypeByName.get( location ) );
	}

	private Class<?> graphMetamodelClass(NamedEntityGraphDefinition definition, MetadataContext context) {
		return context.metamodelClass( managedTypeByName.get( definition.entityName() ) );
	}

	public static void addAllowedEnumLiteralsToEnumTypesMap(
			Map<String, Set<String>> allowedEnumLiteralsToEnumTypeNames,
			String enumConstantName,
			String enumSimpleName,
			String enumAlternativeName,
			String enumClassName
	) {
		allowedEnumLiteralsToEnumTypeNames
				.computeIfAbsent( enumConstantName, s -> new HashSet<>() )
				.add( enumClassName );

		final String simpleQualifiedName = enumSimpleName + "." + enumConstantName;
		allowedEnumLiteralsToEnumTypeNames
				.computeIfAbsent( simpleQualifiedName, s -> new HashSet<>() )
				.add( enumClassName );

		final String qualifiedAlternativeName = enumAlternativeName + "." + enumConstantName;
		allowedEnumLiteralsToEnumTypeNames
				.computeIfAbsent( qualifiedAlternativeName, s -> new HashSet<>() )
				.add( enumClassName );

		final String qualifiedName = enumClassName + "." + enumConstantName;
		allowedEnumLiteralsToEnumTypeNames
				.computeIfAbsent( qualifiedName, s -> new HashSet<>() )
				.add( enumClassName );
	}

	private EntityDomainType<?> locateOrBuildEntityType(
			PersistentClass persistentClass,
			MetadataContext context,
			final TypeConfiguration typeConfiguration) {
		final var entityType = context.locateEntityType( persistentClass );
		return entityType == null
				? buildEntityType( persistentClass, context, typeConfiguration )
				: entityType;
	}

	private EntityTypeImpl<?> buildEntityType(
			PersistentClass persistentClass,
			MetadataContext context,
			TypeConfiguration typeConfiguration) {
		context.pushEntityWorkedOn( persistentClass );
		final var entityType =
				entityType( persistentClass, persistentClass.getMappedClass(), context, typeConfiguration );
		context.registerEntityType( persistentClass, entityType );
		context.popEntityWorkedOn( persistentClass );
		return entityType;
	}

	private <J> EntityTypeImpl<J> entityType(
			PersistentClass persistentClass,
			Class<J> mappedClass,
			MetadataContext context,
			TypeConfiguration typeConfiguration) {
		@SuppressWarnings("unchecked")
		final var supertype =
				(IdentifiableDomainType<? super J>)
						supertypeForPersistentClass( persistentClass, context, typeConfiguration );
		return new EntityTypeImpl<>( entityJavaType( mappedClass, context ),
				supertype, persistentClass, this );
	}

	private static <J> JavaType<J> entityJavaType(Class<J> mappedClass, MetadataContext context) {
		if ( mappedClass == null || Map.class.isAssignableFrom( mappedClass ) ) {
			// dynamic map
			//noinspection unchecked
			return (JavaType<J>) new DynamicModelJavaType();
		}
		else {
			return context.getTypeConfiguration().getJavaTypeRegistry()
					.resolveEntityTypeDescriptor( mappedClass );
		}
	}

	private void handleUnusedMappedSuperclasses(MetadataContext context, TypeConfiguration typeConfiguration) {
		final var unusedMappedSuperclasses = context.getUnusedMappedSuperclasses();
		if ( !unusedMappedSuperclasses.isEmpty() ) {
			for ( var mappedSuperclass : unusedMappedSuperclasses ) {
				CORE_LOGGER.unusedMappedSuperclass( mappedSuperclass.getMappedClass().getName() );
				locateOrBuildMappedSuperclassType( mappedSuperclass, context, typeConfiguration );
			}
		}
	}

	private MappedSuperclassDomainType<?> locateOrBuildMappedSuperclassType(
			MappedSuperclass mappedSuperclass,
			MetadataContext context,
			TypeConfiguration typeConfiguration) {
		final var mappedSuperclassType =
				context.locateMappedSuperclassType( mappedSuperclass );
		return mappedSuperclassType == null
				? buildMappedSuperclassType( mappedSuperclass, mappedSuperclass.getMappedClass(), context, typeConfiguration )
				: mappedSuperclassType;
	}

	private <T> MappedSuperclassTypeImpl<T> buildMappedSuperclassType(
			MappedSuperclass mappedSuperclass,
			Class<T> mappedClass,
			MetadataContext context,
			TypeConfiguration typeConfiguration) {
		@SuppressWarnings("unchecked")
		final var superType =
				(IdentifiableDomainType<? super T>)
						supertypeForMappedSuperclass( mappedSuperclass, context, typeConfiguration );
		final var javaType =
				context.getTypeConfiguration().getJavaTypeRegistry()
						.resolveManagedTypeDescriptor( mappedClass );
		final var mappedSuperclassType =
				new MappedSuperclassTypeImpl<>( javaType, mappedSuperclass, superType, this );
		context.registerMappedSuperclassType( mappedSuperclass, mappedSuperclassType );
		return mappedSuperclassType;
	}

	private IdentifiableDomainType<?> supertypeForPersistentClass(
			PersistentClass persistentClass,
			MetadataContext context,
			TypeConfiguration typeConfiguration) {
		final var superMappedSuperclass = persistentClass.getSuperMappedSuperclass();
		final var supertype =
				superMappedSuperclass == null
						? null
						: locateOrBuildMappedSuperclassType( superMappedSuperclass, context, typeConfiguration );
		if ( supertype == null ) {
			// no mapped superclass, check for a super entity
			final var superPersistentClass = persistentClass.getSuperclass();
			return superPersistentClass == null
					? null
					: locateOrBuildEntityType( superPersistentClass, context, typeConfiguration );
		}
		else {
			return supertype;
		}
	}

	private IdentifiableDomainType<?> supertypeForMappedSuperclass(
			MappedSuperclass mappedSuperclass,
			MetadataContext context,
			TypeConfiguration typeConfiguration) {
		final var superMappedSuperclass = mappedSuperclass.getSuperMappedSuperclass();
		final var superType =
				superMappedSuperclass == null
						? null
						: locateOrBuildMappedSuperclassType( superMappedSuperclass, context, typeConfiguration );
		if ( superType == null ) {
			//no mapped superclass, check for a super entity
			final var superPersistentClass = mappedSuperclass.getSuperPersistentClass();
			return superPersistentClass == null
					? null
					: locateOrBuildEntityType( superPersistentClass, context, typeConfiguration );
		}
		else {
			return superType;
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Serialization

	@Serial
	private Object writeReplace() throws ObjectStreamException {
		return new SerialForm( typeConfiguration.getSessionFactory() );
	}

	private static class SerialForm implements Serializable {
		private final SessionFactoryImplementor sessionFactory;

		public SerialForm(SessionFactoryImplementor sessionFactory) {
			this.sessionFactory = sessionFactory;
		}

		@Serial
		private Object readResolve() {
			return sessionFactory.getJpaMetamodel();
		}

	}
}
