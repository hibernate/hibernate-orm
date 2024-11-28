/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.HEMLogging;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.internal.JpaMetaModelPopulationSetting;
import org.hibernate.metamodel.internal.JpaStaticMetaModelPopulationSetting;
import org.hibernate.metamodel.internal.MetadataContext;
import org.hibernate.metamodel.mapping.EntityMappingType;
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
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Type;

/**
 *
 * @author Steve Ebersole
 */
public class JpaMetamodelImpl implements JpaMetamodelImplementor, Serializable {
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( JpaMetamodel.class );

	private static class ImportInfo<T> {
		final String importedName;
		Class<T> loadedClass; // could be null for boot metamodel import; not final to allow for populating later

		ImportInfo(String importedName, Class<T> loadedClass) {
			this.importedName = importedName;
			this.loadedClass = loadedClass;
		}
	}

	private final TypeConfiguration typeConfiguration;
	private final MappingMetamodel mappingMetamodel;
	private final ServiceRegistry serviceRegistry;

	private final Map<String, ManagedDomainType<?>> managedTypeByName = new TreeMap<>();
	private final Map<Class<?>, ManagedDomainType<?>> managedTypeByClass = new HashMap<>();
	private JpaMetaModelPopulationSetting jpaMetaModelPopulationSetting;
	private final Map<String, Set<String>> allowedEnumLiteralsToEnumTypeNames = new HashMap<>();
	private final Map<String, EnumJavaType<?>> enumJavaTypes = new HashMap<>();

	private final transient Map<String, RootGraphImplementor<?>> entityGraphMap = new ConcurrentHashMap<>();

	private final Map<Class<?>, SqmPolymorphicRootDescriptor<?>> polymorphicEntityReferenceMap = new ConcurrentHashMap<>();

	private final Map<Class<?>, String> entityProxyInterfaceMap = new HashMap<>();

	private final Map<String, ImportInfo<?>> nameToImportMap = new ConcurrentHashMap<>();
	private final Map<String,Object> knownInvalidnameToImportMap = new ConcurrentHashMap<>();


	public JpaMetamodelImpl(
			TypeConfiguration typeConfiguration,
			MappingMetamodel mappingMetamodel,
			ServiceRegistry serviceRegistry) {
		this.typeConfiguration = typeConfiguration;
		this.mappingMetamodel = mappingMetamodel;
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public JpaCompliance getJpaCompliance() {
		return typeConfiguration.getJpaCompliance();
	}

	@Override
	public <X> ManagedDomainType<X> managedType(String typeName) {
		//noinspection unchecked
		return typeName == null ? null : (ManagedDomainType<X>) managedTypeByName.get( typeName );
	}

	@Override
	public <X> EntityDomainType<X> entity(String entityName) {
		if ( entityName == null ) {
			return null;
		}
		final ManagedDomainType<?> managedType = managedTypeByName.get( entityName );
		if ( !( managedType instanceof EntityDomainType<?> ) ) {
			return null;
		}
		//noinspection unchecked
		return (EntityDomainType<X>) managedType;
	}

	@Override
	public <X> EmbeddableDomainType<X> embeddable(String embeddableName) {
		if ( embeddableName == null ) {
			return null;
		}
		final ManagedDomainType<?> managedType = managedTypeByName.get( embeddableName );
		if ( !( managedType instanceof EmbeddableDomainType<?> ) ) {
			return null;
		}
		//noinspection unchecked
		return (EmbeddableDomainType<X>) managedType;
	}

	@Override
	public <X> EntityDomainType<X> getHqlEntityReference(String entityName) {
		Class<X> loadedClass = null;
		final ImportInfo<X> importInfo = resolveImport( entityName );
		if ( importInfo != null ) {
			loadedClass = importInfo.loadedClass;
			entityName = importInfo.importedName;
		}

		final EntityDomainType<X> entityDescriptor = entity( entityName );
		if ( entityDescriptor != null ) {
			return entityDescriptor;
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
	public <X> ManagedDomainType<X> findManagedType(Class<X> cls) {
		//noinspection unchecked
		return (ManagedDomainType<X>) managedTypeByClass.get( cls );
	}

	@Override
	public <X> EntityDomainType<X> findEntityType(Class<X> cls) {
		final ManagedType<?> type = managedTypeByClass.get( cls );
		if ( !( type instanceof EntityDomainType<?> ) ) {
			return null;
		}
		//noinspection unchecked
		return (EntityDomainType<X>) type;
	}

	@Override
	public <X> ManagedDomainType<X> managedType(Class<X> cls) {
		final ManagedType<?> type = managedTypeByClass.get( cls );
		if ( type == null ) {
			// per JPA
			throw new IllegalArgumentException( "Not a managed type: " + cls );
		}

		//noinspection unchecked
		return (ManagedDomainType<X>) type;
	}

	@Override
	public <X> EntityDomainType<X> entity(Class<X> cls) {
		final ManagedType<?> type = managedTypeByClass.get( cls );
		if ( !( type instanceof EntityDomainType<?> ) ) {
			throw new IllegalArgumentException( "Not an entity: " + cls.getName() );
		}
		//noinspection unchecked
		return (EntityDomainType<X>) type;
	}

	@Override
	public <X> EmbeddableDomainType<X> embeddable(Class<X> cls) {
		final ManagedType<?> type = managedTypeByClass.get( cls );
		if ( !( type instanceof EmbeddableDomainType<?> ) ) {
			throw new IllegalArgumentException( "Not an embeddable: " + cls.getName() );
		}
		//noinspection unchecked
		return (EmbeddableDomainType<X>) type;
	}

	private Collection<ManagedDomainType<?>> getAllManagedTypes() {
		switch ( jpaMetaModelPopulationSetting ) {
			case IGNORE_UNSUPPORTED:
				return managedTypeByClass.values();
			case ENABLED:
				return managedTypeByName.values();
			case DISABLED:
				return Collections.emptySet();
			default:
				// should never happen
				throw new AssertionError();
		}
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
		return allowedEnumLiteralsToEnumTypeNames.get( enumValue);
	}

	@Override
	public EnumJavaType<?> getEnumType(String className) {
		final EnumJavaType<?> enumJavaType = enumJavaTypes.get( className );
		if ( enumJavaType != null ) {
			return enumJavaType;
		}
		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		try {
			final Class<Object> clazz = classLoaderService.classForName( className );
			if ( clazz == null || !clazz.isEnum() ) {
				return null;
			}
			return new EnumJavaType( clazz );
		}
		catch (ClassLoadingException e) {
			throw new RuntimeException( e );
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
				return getTypeConfiguration()
						.getJavaTypeRegistry()
						.getDescriptor( referencedField.getType() );
			}
		}
		catch (NoSuchFieldException e) {
		}
		return null;
	}

	@Override
	public <T> T getJavaConstant(String className, String fieldName) {
		try {
			final Field referencedField = getJavaField( className, fieldName );
			return (T) referencedField.get( null );
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException( e );
		}
	}

	private Field getJavaField(String className, String fieldName) throws NoSuchFieldException {
		final Class<?> namedClass =
				getServiceRegistry()
						.requireService( ClassLoaderService.class )
						.classForName( className );
		if ( namedClass != null ) {
			return namedClass.getDeclaredField( fieldName );
		}
		return null;
	}

	@Override
	public <T> void addNamedEntityGraph(String graphName, RootGraphImplementor<T> entityGraph) {
		final EntityGraph<?> old = entityGraphMap.put(
				graphName,
				entityGraph.makeImmutableCopy( graphName )
		);

		if ( old != null ) {
			log.debugf( "EntityGraph being replaced on EntityManagerFactory for name %s", graphName );
		}
	}

	@Override
	public <T> RootGraphImplementor<T> findEntityGraphByName(String name) {
		//noinspection unchecked
		return (RootGraphImplementor<T>) entityGraphMap.get( name );
	}

	@Override
	public <T> List<RootGraphImplementor<? super T>> findEntityGraphsByJavaType(Class<T> entityClass) {
		final EntityDomainType<T> entityType = entity( entityClass );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "Given class is not an entity: " + entityClass.getName() );
		}
		else {
			final List<RootGraphImplementor<? super T>> results = new ArrayList<>();
			for ( RootGraphImplementor<?> entityGraph : entityGraphMap.values() ) {
				if ( entityGraph.appliesTo( entityType ) ) {
					@SuppressWarnings("unchecked")
					final RootGraphImplementor<? super T> result = (RootGraphImplementor<? super T>) entityGraph;
					results.add( result );
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

	@SuppressWarnings("unchecked")
	private void applyNamedEntityGraphs(Collection<NamedEntityGraphDefinition> namedEntityGraphs) {
		for ( NamedEntityGraphDefinition definition : namedEntityGraphs ) {
			log.debugf(
					"Applying named entity graph [name=%s, entity-name=%s, jpa-entity-name=%s]",
					definition.getRegisteredName(),
					definition.getEntityName(),
					definition.getJpaEntityName()
			);
			final EntityDomainType<Object> entityType = entity( definition.getEntityName() );
			if ( entityType == null ) {
				throw new IllegalArgumentException(
						"Attempted to register named entity graph [" + definition.getRegisteredName()
								+ "] for unknown entity [" + definition.getEntityName() + "]"

				);
			}

			final RootGraphImpl<Object> entityGraph = new RootGraphImpl<>( definition.getRegisteredName(), entityType );

			final NamedEntityGraph namedEntityGraph = definition.getAnnotation();

			if ( namedEntityGraph.includeAllAttributes() ) {
				for ( Attribute<? super Object, ?> attribute : entityType.getAttributes() ) {
					entityGraph.addAttributeNodes( attribute );
				}
			}

			if ( namedEntityGraph.attributeNodes() != null ) {
				applyNamedAttributeNodes( namedEntityGraph.attributeNodes(), namedEntityGraph, entityGraph );
			}

			entityGraphMap.put( definition.getRegisteredName(), entityGraph );
		}
	}

	private void applyNamedAttributeNodes(
			NamedAttributeNode[] namedAttributeNodes,
			NamedEntityGraph namedEntityGraph,
			GraphImplementor<?> graphNode) {
		for ( NamedAttributeNode namedAttributeNode : namedAttributeNodes ) {
			final String value = namedAttributeNode.value();
			AttributeNodeImplementor<?> attributeNode = graphNode.addAttributeNode( value );
			if ( StringHelper.isNotEmpty( namedAttributeNode.subgraph() ) ) {
				final SubGraphImplementor<?> subgraph = attributeNode.makeSubGraph();
				applyNamedSubgraphs(
						namedEntityGraph,
						namedAttributeNode.subgraph(),
						subgraph
				);
			}
			if ( StringHelper.isNotEmpty( namedAttributeNode.keySubgraph() ) ) {
				final SubGraphImplementor<?> subgraph = attributeNode.makeKeySubGraph();

				applyNamedSubgraphs(
						namedEntityGraph,
						namedAttributeNode.keySubgraph(),
						subgraph
				);
			}
		}
	}

	private void applyNamedSubgraphs(
			NamedEntityGraph namedEntityGraph,
			String subgraphName,
			SubGraphImplementor<?> subgraph) {
		for ( NamedSubgraph namedSubgraph : namedEntityGraph.subgraphs() ) {
			if ( subgraphName.equals( namedSubgraph.name() ) ) {
				applyNamedAttributeNodes(
						namedSubgraph.attributeNodes(),
						namedEntityGraph,
						subgraph
				);
			}
		}
	}

	private <X> Class<X> resolveRequestedClass(String entityName) {
		try {
			return getServiceRegistry().requireService( ClassLoaderService.class ).classForName( entityName );
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
				return entity( proxyEntityName );
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

					// it should not be added if its direct super (if one) is defined without
					// explicit-polymorphism.  The super itself will get added and the initializers
					// for entity mappings already handle loading subtypes - adding it would be redundant
					final ManagedDomainType<?> superType = managedType.getSuperType();
					if ( superType != null
							&& superType.getPersistenceType() == Type.PersistenceType.ENTITY
							&& javaType.isAssignableFrom( superType.getJavaType() ) ) {
						final EntityMappingType superMapping = getMappingMetamodel()
								.getEntityDescriptor( ( (EntityDomainType<?>) superType ).getHibernateEntityName() );
						if ( !superMapping.isExplicitPolymorphism() ) {
							continue;
						}
					}

					// it should not be added if it is mapped with explicit polymorphism itself
					final EntityMappingType entityPersister = getMappingMetamodel()
							.getEntityDescriptor( managedType.getTypeName() );
					if ( entityPersister.isExplicitPolymorphism() ) {
						continue;
					}

					// aside from these special cases, add it
					matchingDescriptors.add( (EntityDomainType<? extends T>) managedType );
				}
			}

			// if we found any matching, create the virtual root EntityDomainType reference
			if ( !matchingDescriptors.isEmpty() ) {
				final SqmPolymorphicRootDescriptor<T> descriptor = new SqmPolymorphicRootDescriptor<>(
						typeConfiguration.getJavaTypeRegistry().resolveDescriptor( javaType ),
						matchingDescriptors
				);
				polymorphicEntityReferenceMap.putIfAbsent( javaType, descriptor );
				return descriptor;
			}
		}

		throw new EntityTypeException( "Could not resolve entity class '" + javaType.getName() + "'", javaType.getName() );
	}

	@Override
	public MappingMetamodel getMappingMetamodel() {
		return mappingMetamodel;
	}

	public void processJpa(
			MetadataImplementor bootMetamodel,
			MappingMetamodel mappingMetamodel,
			Map<Class<?>, String> entityProxyInterfaceMap,
			JpaStaticMetaModelPopulationSetting jpaStaticMetaModelPopulationSetting,
			JpaMetaModelPopulationSetting jpaMetaModelPopulationSetting,
			Collection<NamedEntityGraphDefinition> namedEntityGraphDefinitions,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		bootMetamodel.getImports().forEach( ( k, v ) ->  this.nameToImportMap.put( k, new ImportInfo<>( v, null ) ) );
		this.entityProxyInterfaceMap.putAll( entityProxyInterfaceMap );

		final MetadataContext context = new MetadataContext(
				this,
				mappingMetamodel,
				bootMetamodel,
				jpaStaticMetaModelPopulationSetting,
				jpaMetaModelPopulationSetting,
				runtimeModelCreationContext
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

		typeConfiguration.getJavaTypeRegistry().forEachDescriptor( (descriptor) -> {
			if ( descriptor instanceof EnumJavaType ) {
				final EnumJavaType<? extends Enum<?>> enumJavaType = (EnumJavaType<? extends Enum<?>>) descriptor;
				final Class<? extends Enum<?>> enumJavaClass = enumJavaType.getJavaTypeClass();
				final Enum<?>[] enumConstants = enumJavaClass.getEnumConstants();
				for ( Enum<?> enumConstant : enumConstants ) {
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
			TypeConfiguration typeConfiguration) {
		EntityDomainType<?> entityType = context.locateEntityType( persistentClass );
		if ( entityType == null ) {
			entityType = buildEntityType( persistentClass, context, typeConfiguration );
		}
		return entityType;
	}

	@SuppressWarnings("unchecked")
	private EntityTypeImpl<?> buildEntityType(
			PersistentClass persistentClass,
			MetadataContext context,
			TypeConfiguration typeConfiguration) {
		context.pushEntityWorkedOn( persistentClass );

		final MappedSuperclass superMappedSuperclass = persistentClass.getSuperMappedSuperclass();

		IdentifiableDomainType<?> superType = superMappedSuperclass == null
				? null
				: locateOrBuildMappedSuperclassType( superMappedSuperclass, context, typeConfiguration );

		//no mappedSuperclass, check for a super entity
		if ( superType == null ) {
			final PersistentClass superPersistentClass = persistentClass.getSuperclass();
			superType = superPersistentClass == null
					? null
					: locateOrBuildEntityType( superPersistentClass, context, typeConfiguration );
		}

		final Class<?> javaTypeClass = persistentClass.getMappedClass();
		final JavaType<?> javaType;
		if ( javaTypeClass == null || javaTypeClass == Map.class ) {
			// dynamic map
			javaType = new DynamicModelJavaType();
		}
		else {
			javaType = context.getTypeConfiguration()
					.getJavaTypeRegistry()
					.resolveEntityTypeDescriptor( javaTypeClass );
		}

		final EntityTypeImpl<?> entityType = new EntityTypeImpl(
				javaType,
				superType,
				persistentClass,
				this
		);

		context.registerEntityType( persistentClass, entityType );
		context.popEntityWorkedOn( persistentClass );

		return entityType;
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

	private MappedSuperclassDomainType<?> locateOrBuildMappedSuperclassType(
			MappedSuperclass mappedSuperclass,
			MetadataContext context,
			TypeConfiguration typeConfiguration) {
		MappedSuperclassDomainType<?> mappedSuperclassType = context.locateMappedSuperclassType( mappedSuperclass );
		if ( mappedSuperclassType == null ) {
			mappedSuperclassType = buildMappedSuperclassType( mappedSuperclass, context, typeConfiguration );
		}
		return mappedSuperclassType;
	}

	@SuppressWarnings("unchecked")
	private MappedSuperclassTypeImpl<?> buildMappedSuperclassType(
			MappedSuperclass mappedSuperclass,
			MetadataContext context,
			TypeConfiguration typeConfiguration) {
		final MappedSuperclass superMappedSuperclass = mappedSuperclass.getSuperMappedSuperclass();
		IdentifiableDomainType<?> superType = superMappedSuperclass == null
				? null
				: locateOrBuildMappedSuperclassType( superMappedSuperclass, context, typeConfiguration );
		//no mappedSuperclass, check for a super entity
		if ( superType == null ) {
			final PersistentClass superPersistentClass = mappedSuperclass.getSuperPersistentClass();
			superType = superPersistentClass == null
					? null
					: locateOrBuildEntityType( superPersistentClass, context, typeConfiguration );
		}
		final JavaType<?> javaType = context.getTypeConfiguration()
				.getJavaTypeRegistry()
				.resolveManagedTypeDescriptor( mappedSuperclass.getMappedClass() );
		final MappedSuperclassTypeImpl<?> mappedSuperclassType = new MappedSuperclassTypeImpl(
				javaType,
				mappedSuperclass,
				superType,
				this
		);

		context.registerMappedSuperclassType( mappedSuperclass, mappedSuperclassType );
		return mappedSuperclassType;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Serialization

	private Object writeReplace() throws ObjectStreamException {
		return new SerialForm( typeConfiguration.getSessionFactory() );
	}

	private static class SerialForm implements Serializable {
		private final SessionFactoryImplementor sessionFactory;

		public SerialForm(SessionFactoryImplementor sessionFactory) {
			this.sessionFactory = sessionFactory;
		}

		private Object readResolve() {
			return sessionFactory.getJpaMetamodel();
		}

	}
}
