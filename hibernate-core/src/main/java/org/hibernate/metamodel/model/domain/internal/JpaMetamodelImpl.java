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
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.internal.JpaMetamodelPopulationSetting;
import org.hibernate.metamodel.internal.JpaStaticMetamodelPopulationSetting;
import org.hibernate.metamodel.internal.MetadataContext;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
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
import static org.hibernate.metamodel.internal.InjectionHelper.injectEntityGraph;
import static org.hibernate.metamodel.internal.InjectionHelper.injectTypedQueryReference;

/**
 * @author Steve Ebersole
 */
public class JpaMetamodelImpl implements JpaMetamodelImplementor, Serializable {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( JpaMetamodel.class );

	private static class ImportInfo<T> {
		private final String importedName;
		private Class<T> loadedClass; // could be null for boot metamodel import; not final to allow for populating later

		private ImportInfo(String importedName, Class<T> loadedClass) {
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

	private final Map<String, ImportInfo<?>> nameToImportMap = new ConcurrentHashMap<>();
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
	public @Nullable <X> ManagedDomainType<X> findManagedType(@Nullable String typeName) {
		//noinspection unchecked
		return typeName == null ? null : (ManagedDomainType<X>) managedTypeByName.get( typeName );
	}

	@Override
	public <X> ManagedDomainType<X> managedType(String typeName) {
		final ManagedDomainType<X> managedType = findManagedType( typeName );
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

		final ManagedDomainType<?> managedType = managedTypeByName.get( entityName );
		if ( managedType instanceof EntityDomainType<?> entityDomainType ){
			return entityDomainType;
		}

		// NOTE: `managedTypeByName` is keyed by Hibernate entity name.
		// If there is a direct match based on key, we want that one - see above.
		// However, the JPA contract for `#entity` is to match `@Entity(name)`; if there
		// was no direct match, we need to iterate over all of the and look based on
		// JPA entity-name.

		for ( Map.Entry<String, ManagedDomainType<?>> entry : managedTypeByName.entrySet() ) {
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
		final ManagedDomainType<?> managedType = managedTypeByName.get( embeddableName );
		if ( !( managedType instanceof EmbeddableDomainType<?> embeddableDomainType ) ) {
			return null;
		}
		return embeddableDomainType;
	}

	@Override
	public EmbeddableDomainType<?> embeddable(String embeddableName) {
		final EmbeddableDomainType<?> embeddableType = findEmbeddableType( embeddableName );
		if ( embeddableType == null ) {
			throw new IllegalArgumentException( "Not an embeddable: " + embeddableName );
		}
		return embeddableType;
	}

	@Override
	public <X> EntityDomainType<X> getHqlEntityReference(String entityName) {
		Class<X> loadedClass = null;
		final ImportInfo<X> importInfo = resolveImport( entityName );
		if ( importInfo != null ) {
			loadedClass = importInfo.loadedClass;
			entityName = importInfo.importedName;
		}

		final EntityDomainType<?> entityDescriptor = findEntityType( entityName );
		if ( entityDescriptor != null ) {
			//noinspection unchecked
			return (EntityDomainType<X>) entityDescriptor;
		}

		if ( loadedClass == null ) {
			loadedClass = resolveRequestedClass( entityName );
			// populate class cache for boot metamodel imports
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
	public <X> EntityDomainType<X> resolveHqlEntityReference(String entityName) {
		final EntityDomainType<X> hqlEntityReference = getHqlEntityReference( entityName );
		if ( hqlEntityReference == null ) {
			throw new EntityTypeException( "Could not resolve entity name '" + entityName + "'", entityName );
		}
		return hqlEntityReference;
	}

	@Override
	@Nullable
	public <X> ManagedDomainType<X> findManagedType(Class<X> cls) {
		//noinspection unchecked
		return (ManagedDomainType<X>) managedTypeByClass.get( cls );
	}

	@Override
	public <X> ManagedDomainType<X> managedType(Class<X> cls) {
		final ManagedDomainType<X> type = findManagedType( cls );
		if ( type == null ) {
			// per JPA
			throw new IllegalArgumentException( "Not a managed type: " + cls );
		}
		return type;
	}

	@Override
	@Nullable
	public <X> EntityDomainType<X> findEntityType(Class<X> cls) {
		final ManagedType<?> type = managedTypeByClass.get( cls );
		if ( !( type instanceof EntityDomainType<?> ) ) {
			return null;
		}
		//noinspection unchecked
		return (EntityDomainType<X>) type;
	}

	@Override
	public <X> EntityDomainType<X> entity(Class<X> cls) {
		final EntityDomainType<X> entityType = findEntityType( cls );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "Not an entity: " + cls.getName() );
		}
		return entityType;
	}

	@Override
	public @Nullable <X> EmbeddableDomainType<X> findEmbeddableType(Class<X> cls) {
		final ManagedType<?> type = managedTypeByClass.get( cls );
		if ( !( type instanceof EmbeddableDomainType<?> ) ) {
			return null;
		}
		//noinspection unchecked
		return (EmbeddableDomainType<X>) type;
	}

	@Override
	public <X> EmbeddableDomainType<X> embeddable(Class<X> cls) {
		final EmbeddableDomainType<X> embeddableType = findEmbeddableType( cls );
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
		final EnumJavaType<?> enumJavaType = enumJavaTypes.get( className );
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
			final Field referencedField = getJavaField( className, fieldName );
			if ( referencedField != null ) {
				return getTypeConfiguration().getJavaTypeRegistry()
						.getDescriptor( referencedField.getType() );
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
			final Field referencedField = getJavaField( className, fieldName );
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
		final EntityGraph<?> old = entityGraphMap.put( graphName, rootGraph.makeImmutableCopy( graphName ) );
		if ( old != null ) {
			log.debugf( "EntityGraph named '%s' was replaced", graphName );
		}
	}

	@Override
	public RootGraphImplementor<?> findEntityGraphByName(String name) {
		return entityGraphMap.get( name );
	}

	@Override
	public <T> List<EntityGraph<? super T>> findEntityGraphsByJavaType(Class<T> entityClass) {
		final EntityDomainType<T> entityType = entity( entityClass );
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
		final EntityDomainType<T> entityType = entity( entityClass );
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
		final ImportInfo<?> importInfo = resolveImport( queryName );
		return importInfo == null ? null : importInfo.importedName;
	}

	private <T> ImportInfo<T> resolveImport(final String name) {
		final ImportInfo<?> importInfo = nameToImportMap.get( name );
		//optimal path first
		if ( importInfo != null ) {
			//noinspection unchecked
			return (ImportInfo<T>) importInfo;
		}
		else {
			//then check the negative cache, to avoid bothering the classloader unnecessarily
			if ( knownInvalidnameToImportMap.containsKey( name ) ) {
				return null;
			}
			else {
				// see if the name is a fully-qualified class name
				final Class<T> loadedClass = resolveRequestedClass( name );
				if ( loadedClass == null ) {
					// it is NOT a fully-qualified class name - add a marker entry so we do not keep trying later
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
					// it is a fully-qualified class name - add it to the cache
					// so to not needing to load from the classloader again
					final ImportInfo<T> info = new ImportInfo<>( name, loadedClass );
					nameToImportMap.put( name, info );
					return info;
				}
			}
		}
	}

	private void applyNamedEntityGraphs(Collection<NamedEntityGraphDefinition> namedEntityGraphs) {
		for ( NamedEntityGraphDefinition definition : namedEntityGraphs ) {
			log.debugf(
					"Applying named entity graph [name=%s, source=%s]",
					definition.name(),
					definition.source()
			);

			final RootGraphImplementor<?> graph = definition.graphCreator().createEntityGraph(
					(entityClass) -> {
						final ManagedDomainType<?> managedDomainType = managedTypeByClass.get( entityClass );
						if ( managedDomainType instanceof EntityDomainType<?> match ) {
							return match;
						}
						throw new IllegalArgumentException( "Cannot resolve entity class : " + entityClass.getName() );
					},
					(jpaEntityName) -> {
						for ( Map.Entry<String, ManagedDomainType<?>> entry : managedTypeByName.entrySet() ) {
							if ( entry.getValue() instanceof EntityDomainType<?> possibility ) {
								if ( jpaEntityName.equals( possibility.getName() ) ) {
									return possibility;
								}
							}
						}
						throw new IllegalArgumentException( "Cannot resolve entity name : " + jpaEntityName );
					}
			);
			entityGraphMap.put( definition.name(), graph );
		}
	}


	private <X> Class<X> resolveRequestedClass(String entityName) {
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
			final ManagedDomainType<?> managedType = managedTypeByClass.get( javaType );
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
			final EntityDomainType<T> polymorphicDomainType =
					(EntityDomainType<T>) polymorphicEntityReferenceMap.get( javaType );
			if ( polymorphicDomainType != null ) {
				return polymorphicDomainType;
			}

			// create a set of descriptors that should be used to build the polymorphic EntityDomainType
			final Set<EntityDomainType<? extends T>> matchingDescriptors = new HashSet<>();
			for ( ManagedDomainType<?> managedType : managedTypeByName.values() ) {
				if ( managedType.getPersistenceType() != Type.PersistenceType.ENTITY ) {
					continue;
				}
				// see if we should add `entityDomainType` as one of the matching-descriptors.
				if ( javaType.isAssignableFrom( managedType.getJavaType() ) ) {
					// the queried type is assignable from the type of the current entity-type
					// we should add it to the collecting set of matching descriptors.  it should
					// be added aside from a few cases...

					// if the managed-type has a super type and the java type is assignable from the super type,
					// do not add the managed-type as the super itself will get added and the initializers for
					// entity mappings already handle loading subtypes - adding it would be redundant and lead to
					// incorrect results
					final ManagedDomainType<?> superType = managedType.getSuperType();
					if ( superType != null
							&& superType.getPersistenceType() == Type.PersistenceType.ENTITY
							&& javaType.isAssignableFrom( superType.getJavaType() ) ) {
						continue;
					}

					// otherwise, add it
					matchingDescriptors.add( (EntityDomainType<? extends T>) managedType );
				}
			}

			// if we found any matching, create the virtual root EntityDomainType reference
			if ( !matchingDescriptors.isEmpty() ) {
				final SqmPolymorphicRootDescriptor<T> descriptor = new SqmPolymorphicRootDescriptor<>(
						typeConfiguration.getJavaTypeRegistry().resolveDescriptor( javaType ),
						matchingDescriptors,
						this
				);
				polymorphicEntityReferenceMap.putIfAbsent( javaType, descriptor );
				return descriptor;
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
				.forEach( (key, value) -> this.nameToImportMap.put( key, new ImportInfo<>( value, null ) ) );
		this.entityProxyInterfaceMap.putAll( entityProxyInterfaceMap );

		final MetadataContext context = new MetadataContext(
				this,
				mappingMetamodel,
				bootMetamodel,
				jpaStaticMetaModelPopulationSetting,
				jpaMetaModelPopulationSetting,
				runtimeModelCreationContext,
				runtimeModelCreationContext.getBootstrapContext().getClassLoaderService()
		);


		for ( PersistentClass entityBinding : bootMetamodel.getEntityBindings() ) {
			locateOrBuildEntityType( entityBinding, context, typeConfiguration );
		}
		handleUnusedMappedSuperclasses( context, typeConfiguration );

		context.wrapUp();

		this.jpaMetaModelPopulationSetting = jpaMetaModelPopulationSetting;

		// Identifiable types (Entities and MappedSuperclasses)
		this.managedTypeByName.putAll( context.getIdentifiableTypesByName() );
		this.managedTypeByClass.putAll( context.getEntityTypeMap() );
		this.managedTypeByClass.putAll( context.getMappedSuperclassTypeMap() );

		// Embeddable types
		int mapEmbeddables = 0;
		for ( EmbeddableDomainType<?> embeddable : context.getEmbeddableTypeSet() ) {
			// Do not register the embeddable types for id classes
			if ( embeddable.getExpressibleJavaType() instanceof EntityJavaType<?> ) {
				continue;
			}
			final Class<?> embeddableClass = embeddable.getJavaType();
			if ( embeddableClass != Map.class ) {
				this.managedTypeByClass.put( embeddable.getJavaType(), embeddable );
				this.managedTypeByName.put( embeddable.getTypeName(), embeddable );
			}
			else {
				this.managedTypeByName.put( "dynamic-embeddable-" + mapEmbeddables++, embeddable );
			}
		}

		typeConfiguration.getJavaTypeRegistry().forEachDescriptor( descriptor -> {
			if ( descriptor instanceof EnumJavaType<? extends Enum<?>> enumJavaType ) {
				final Class<? extends Enum<?>> enumJavaClass = enumJavaType.getJavaTypeClass();
				for ( Enum<?> enumConstant : enumJavaClass.getEnumConstants() ) {
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

	private <T> EntityDomainType<T> locateOrBuildEntityType(
			PersistentClass persistentClass,
			MetadataContext context,
			final TypeConfiguration typeConfiguration) {
		@SuppressWarnings("unchecked")
		final EntityDomainType<T> entityType =
				(EntityDomainType<T>)
						context.locateEntityType( persistentClass );
		return entityType == null
				? buildEntityType( persistentClass, context, typeConfiguration )
				: entityType;
	}

	private <T> EntityTypeImpl<T> buildEntityType(
			PersistentClass persistentClass,
			MetadataContext context,
			TypeConfiguration typeConfiguration) {
		context.pushEntityWorkedOn( persistentClass );
		final EntityTypeImpl<T> entityType =
				new EntityTypeImpl<>(
						javaType( persistentClass, context ),
						supertypeForPersistentClass( persistentClass, context, typeConfiguration ),
						persistentClass,
						this
				);
		context.registerEntityType( persistentClass, entityType );
		context.popEntityWorkedOn( persistentClass );
		return entityType;
	}

	@SuppressWarnings("unchecked")
	private static <T> JavaType<T> javaType(PersistentClass persistentClass, MetadataContext context) {
		final Class<T> javaTypeClass = (Class<T>) persistentClass.getMappedClass();
		if ( javaTypeClass == null || Map.class.isAssignableFrom( javaTypeClass ) ) {
			// dynamic map
			return (JavaType<T>) new DynamicModelJavaType();
		}
		else {
			return context.getTypeConfiguration().getJavaTypeRegistry()
					.resolveEntityTypeDescriptor( javaTypeClass );
		}
	}

	private void handleUnusedMappedSuperclasses(MetadataContext context, TypeConfiguration typeConfiguration) {
		final Set<MappedSuperclass> unusedMappedSuperclasses = context.getUnusedMappedSuperclasses();
		if ( !unusedMappedSuperclasses.isEmpty() ) {
			for ( MappedSuperclass mappedSuperclass : unusedMappedSuperclasses ) {
				log.unusedMappedSuperclass( mappedSuperclass.getMappedClass().getName() );
				locateOrBuildMappedSuperclassType( mappedSuperclass, context, typeConfiguration );
			}
		}
	}

	private <T> MappedSuperclassDomainType<T> locateOrBuildMappedSuperclassType(
			MappedSuperclass mappedSuperclass,
			MetadataContext context,
			TypeConfiguration typeConfiguration) {
		@SuppressWarnings("unchecked")
		MappedSuperclassDomainType<T> mappedSuperclassType =
				(MappedSuperclassDomainType<T>) context.locateMappedSuperclassType( mappedSuperclass );
		if (mappedSuperclassType == null) {
			mappedSuperclassType = buildMappedSuperclassType( mappedSuperclass, context, typeConfiguration );
		}
		// HHH-19076: Ensure that each mapped superclass knows ALL its implementations
		context.registerMappedSuperclassForPersistenceClass( mappedSuperclassType );
		return mappedSuperclassType;
	}

	private <T> MappedSuperclassTypeImpl<T> buildMappedSuperclassType(
			MappedSuperclass mappedSuperclass,
			MetadataContext context,
			TypeConfiguration typeConfiguration) {
		final IdentifiableDomainType<? super T> superType =
				supertypeForMappedSuperclass( mappedSuperclass, context, typeConfiguration );
		final JavaType<T> javaType =
				context.getTypeConfiguration().getJavaTypeRegistry()
						.resolveManagedTypeDescriptor( mappedSuperclass.getMappedClass() );
		final MappedSuperclassTypeImpl<T> mappedSuperclassType =
				new MappedSuperclassTypeImpl<>( javaType, mappedSuperclass, superType, this );
		context.registerMappedSuperclassType( mappedSuperclass, mappedSuperclassType );
		return mappedSuperclassType;
	}

	private <T> IdentifiableDomainType<? super T> supertypeForPersistentClass(
			PersistentClass persistentClass,
			MetadataContext context,
			TypeConfiguration typeConfiguration) {
		final MappedSuperclass superMappedSuperclass = persistentClass.getSuperMappedSuperclass();
		final IdentifiableDomainType<? super T> supertype =
				superMappedSuperclass == null
						? null
						: locateOrBuildMappedSuperclassType( superMappedSuperclass, context, typeConfiguration );

		//no mappedSuperclass, check for a super entity
		if ( supertype == null ) {
			final PersistentClass superPersistentClass = persistentClass.getSuperclass();
			return superPersistentClass == null
					? null
					: locateOrBuildEntityType( superPersistentClass, context, typeConfiguration );
		}
		else {
			return supertype;
		}
	}

	private <T> IdentifiableDomainType<? super T> supertypeForMappedSuperclass(
			MappedSuperclass mappedSuperclass,
			MetadataContext context,
			TypeConfiguration typeConfiguration) {
		final MappedSuperclass superMappedSuperclass = mappedSuperclass.getSuperMappedSuperclass();
		final IdentifiableDomainType<T> superType =
				superMappedSuperclass == null
						? null
						: locateOrBuildMappedSuperclassType( superMappedSuperclass, context, typeConfiguration );
		//no mappedSuperclass, check for a super entity
		if ( superType == null ) {
			final PersistentClass superPersistentClass = mappedSuperclass.getSuperPersistentClass();
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
