/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.HEMLogging;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.internal.JpaStaticMetaModelPopulationSetting;
import org.hibernate.metamodel.internal.MetadataContext;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.MappedSuperclassDomainType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.query.sqm.tree.domain.SqmPolymorphicRootDescriptor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.DynamicModelJtd;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class JpaMetamodelImpl implements JpaMetamodel, Serializable {
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( JpaMetamodel.class );
	private static final ImportInfo<?> INVALID_IMPORT = new ImportInfo<>( null, null );

	private static class ImportInfo<T> {
		final String importedName;
		Class<T> loadedClass; // could be null for boot metamodel import; not final to allow for populating later

		ImportInfo(String importedName, Class<T> loadedClass) {
			this.importedName = importedName;
			this.loadedClass = loadedClass;
		}
	}

	private final TypeConfiguration typeConfiguration;
	private final JpaCompliance jpaCompliance;

	private final Map<String, EntityDomainType<?>> jpaEntityTypeMap = new TreeMap<>(); // Need ordering for deterministic implementers list in SqmPolymorphicRootDescriptor
	private final Map<Class<?>, MappedSuperclassDomainType<?>> jpaMappedSuperclassTypeMap = new HashMap<>();
	private final Map<Class, EmbeddableDomainType<?>> jpaEmbeddableDescriptorMap = new HashMap<>();
	private final Map<String, Map<Class<?>, Enum<?>>> allowedEnumLiteralTexts = new HashMap<>();

	private final transient Map<String, RootGraphImplementor> entityGraphMap = new ConcurrentHashMap<>();

	private final Map<Class, SqmPolymorphicRootDescriptor<?>> polymorphicEntityReferenceMap = new ConcurrentHashMap<>();

	private final Map<Class, String> entityProxyInterfaceMap = new HashMap<>();

	private final Map<String, ImportInfo<?>> nameToImportMap = new ConcurrentHashMap<>();


	public JpaMetamodelImpl(TypeConfiguration typeConfiguration, JpaCompliance jpaCompliance) {
		this.typeConfiguration = typeConfiguration;
		this.jpaCompliance = jpaCompliance;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public JpaCompliance getJpaCompliance() {
		return jpaCompliance;
	}

	@Override
	public <X> EntityDomainType<X> entity(String entityName) {
		//noinspection unchecked
		return (EntityDomainType) jpaEntityTypeMap.get( entityName );
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
			throw new IllegalArgumentException( "Could not resolve entity reference: " + entityName );
		}
		return hqlEntityReference;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitManagedTypes(Consumer<ManagedDomainType<?>> action) {
		visitEntityTypes( (Consumer) action );
		visitEmbeddables( (Consumer) action );
		jpaMappedSuperclassTypeMap.values().forEach( action );
	}

	@Override
	public <X> ManagedDomainType<X> findManagedType(Class<X> cls) {
		ManagedType<?> type = jpaEntityTypeMap.get( cls.getName() );
		if ( type == null ) {
			type = jpaMappedSuperclassTypeMap.get( cls );
		}
		if ( type == null ) {
			type = jpaEmbeddableDescriptorMap.get( cls );
		}
		if ( type == null ) {
			return null;
		}

		//noinspection unchecked
		return (ManagedDomainType<X>) type;
	}

	@Override
	public void visitEntityTypes(Consumer<EntityDomainType<?>> action) {
		jpaEntityTypeMap.values().forEach( action );
	}

	@Override
	public <X> EntityDomainType<X> findEntityType(Class<X> cls) {
		final EntityType<?> entityType = jpaEntityTypeMap.get( cls.getName() );
		if ( entityType == null ) {
			return null;
		}
		//noinspection unchecked
		return (EntityDomainType<X>) entityType;
	}

	@Override
	public void visitRootEntityTypes(Consumer<EntityDomainType<?>> action) {
		jpaEntityTypeMap.values().forEach(
				entityDomainType -> {
					if ( entityDomainType.getSuperType() == null ) {
						action.accept( entityDomainType );
					}
				}
		);
	}

	@Override
	public void visitEmbeddables(Consumer<EmbeddableDomainType<?>> action) {
		jpaEmbeddableDescriptorMap.values().forEach( action );
	}

	@Override
	public <X> ManagedDomainType<X> managedType(Class<X> cls) {
		ManagedType<?> type = jpaEntityTypeMap.get( cls.getName() );
		if ( type == null ) {
			type = jpaMappedSuperclassTypeMap.get( cls );
		}
		if ( type == null ) {
			type = jpaEmbeddableDescriptorMap.get( cls );
		}
		if ( type == null ) {
			// per JPA
			throw new IllegalArgumentException( "Not a managed type: " + cls );
		}

		//noinspection unchecked
		return (ManagedDomainType<X>) type;
	}

	@Override
	public <X> EntityDomainType<X> entity(Class<X> cls) {
		final EntityType<?> entityType = jpaEntityTypeMap.get( cls.getName() );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "Not an entity: " + cls.getName() );
		}
		//noinspection unchecked
		return (EntityDomainType<X>) entityType;
	}

	@Override
	public <X> EmbeddableDomainType<X> embeddable(Class<X> cls) {
		final EmbeddableDomainType<?> embeddableType = jpaEmbeddableDescriptorMap.get( cls );
		if ( embeddableType == null ) {
			throw new IllegalArgumentException( "Not an embeddable: " + cls );
		}
		//noinspection unchecked
		return (EmbeddableDomainType<X>) embeddableType;
	}

	@Override
	public Set<ManagedType<?>> getManagedTypes() {
		final int setSize = CollectionHelper.determineProperSizing(
				jpaEntityTypeMap.size() + jpaMappedSuperclassTypeMap.size() + jpaEmbeddableDescriptorMap.size()
		);
		final Set<ManagedType<?>> managedTypes = CollectionHelper.setOfSize( setSize );
		managedTypes.addAll( jpaEntityTypeMap.values() );
		managedTypes.addAll( jpaMappedSuperclassTypeMap.values() );
		managedTypes.addAll( jpaEmbeddableDescriptorMap.values() );
		return managedTypes;
	}

	@Override
	public Set<EntityType<?>> getEntities() {
		return new HashSet<>( jpaEntityTypeMap.values() );
	}

	@Override
	public Set<EmbeddableType<?>> getEmbeddables() {
		return new HashSet<>( jpaEmbeddableDescriptorMap.values() );
	}

	@Override
	public Map<String, Map<Class<?>, Enum<?>>> getAllowedEnumLiteralTexts() {
		return allowedEnumLiteralTexts;
	}

	@Override
	public <T> void addNamedEntityGraph(String graphName, RootGraphImplementor<T> entityGraph) {
		final EntityGraph old = entityGraphMap.put(
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
		return entityGraphMap.get( name );
	}

	@Override
	public <T> List<RootGraphImplementor<? super T>> findEntityGraphsByJavaType(Class<T> entityClass) {
		final EntityDomainType<T> entityType = entity( entityClass );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "Given class is not an entity : " + entityClass.getName() );
		}

		final List<RootGraphImplementor<? super T>> results = new ArrayList<>();

		for ( EntityGraph entityGraph : entityGraphMap.values() ) {
			if ( !( entityGraph instanceof RootGraphImplementor ) ) {
				continue;
			}

			final RootGraphImplementor egi = (RootGraphImplementor) entityGraph;
			//noinspection unchecked
			if ( egi.appliesTo( entityType ) ) {
				//noinspection unchecked
				results.add( egi );
			}
		}

		return results;
	}

	@Override
	public String qualifyImportableName(String queryName) {
		final ImportInfo<?> importInfo = resolveImport( queryName );
		return importInfo == null ? null : importInfo.importedName;
	}

	private <T> ImportInfo<T> resolveImport(String name) {
		//noinspection unchecked
		final ImportInfo<T> result = (ImportInfo<T>) nameToImportMap.computeIfAbsent( name, unknownName -> {
			// see if the name is a fully-qualified class name
			final Class<T> loadedClass = resolveRequestedClass( unknownName );
			if ( loadedClass == null ) {
				// it is NOT a fully-qualified class name - add a marker entry
				//		so we do not keep trying later
				// note that ConcurrentHashMap does not support null value so a marker entry is needed
				return INVALID_IMPORT;
			}
			else {
				// it is a fully-qualified class name - add it to the cache
				//		so we do not keep trying later
				return new ImportInfo<>( unknownName, loadedClass );
			}
		} );
		if ( result == INVALID_IMPORT ) {
			return null;
		}
		else {
			return result;
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
			final EntityDomainType entityType = entity( definition.getEntityName() );
			if ( entityType == null ) {
				throw new IllegalArgumentException(
						"Attempted to register named entity graph [" + definition.getRegisteredName()
								+ "] for unknown entity [" + definition.getEntityName() + "]"

				);
			}

			final RootGraphImpl entityGraph = new RootGraphImpl(
					definition.getRegisteredName(),
					entityType,
					this
			);

			final NamedEntityGraph namedEntityGraph = definition.getAnnotation();

			if ( namedEntityGraph.includeAllAttributes() ) {
				for ( Object attributeObject : entityType.getAttributes() ) {
					entityGraph.addAttributeNodes( (Attribute) attributeObject );
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
			GraphImplementor graphNode) {
		for ( NamedAttributeNode namedAttributeNode : namedAttributeNodes ) {
			final String value = namedAttributeNode.value();
			AttributeNodeImplementor attributeNode = graphNode.addAttributeNode( value );
			if ( StringHelper.isNotEmpty( namedAttributeNode.subgraph() ) ) {
				final SubGraphImplementor subgraph = attributeNode.makeSubGraph();
				applyNamedSubgraphs(
						namedEntityGraph,
						namedAttributeNode.subgraph(),
						subgraph
				);
			}
			if ( StringHelper.isNotEmpty( namedAttributeNode.keySubgraph() ) ) {
				final SubGraphImplementor subgraph = attributeNode.makeKeySubGraph();

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
			SubGraphImplementor subgraph) {
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
			return getServiceRegistry().getService( ClassLoaderService.class ).classForName( entityName );
		}
		catch (ClassLoadingException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> EntityDomainType<T> resolveEntityReference(Class<T> javaType) {
		// try the incoming Java type as a "strict" entity reference
		{
			final EntityDomainType<?> descriptor = jpaEntityTypeMap.get( javaType.getName() );
			if ( descriptor != null ) {
				return (EntityDomainType<T>) descriptor;
			}
		}

		// Next, try it as a proxy interface reference
		{
			final String proxyEntityName = entityProxyInterfaceMap.get( javaType );
			if ( proxyEntityName != null ) {
				return (EntityDomainType<T>) jpaEntityTypeMap.get( proxyEntityName );
			}
		}

		// otherwise, try to handle it as a polymorphic reference
		{
			EntityDomainType<T> polymorphicDomainType = (EntityDomainType<T>) polymorphicEntityReferenceMap.get( javaType );
			if ( polymorphicDomainType != null ) {
				return polymorphicDomainType;
			}

			final Set<EntityDomainType<?>> matchingDescriptors = new HashSet<>();
			visitEntityTypes(
					entityDomainType -> {
						if ( javaType.isAssignableFrom( entityDomainType.getJavaType() ) ) {
							final ManagedDomainType<?> superType = entityDomainType.getSuperType();
							// If the entity super type is also assignable, skip adding this entity type
							if ( superType instanceof EntityDomainType<?>
									&& javaType.isAssignableFrom( superType.getJavaType() ) ) {
								final Queryable entityPersister = (Queryable) typeConfiguration.getSessionFactory()
										.getMetamodel()
										.getEntityDescriptor( ( (EntityDomainType<?>) superType ).getHibernateEntityName() );
								// But only skip adding this type if the parent doesn't require explicit polymorphism
								if ( !entityPersister.isExplicitPolymorphism() ) {
									return;
								}
							}
							final Queryable entityPersister = (Queryable) typeConfiguration.getSessionFactory()
									.getMetamodel()
									.getEntityDescriptor( entityDomainType.getHibernateEntityName() );
							if ( !entityPersister.isExplicitPolymorphism() ) {
								matchingDescriptors.add( entityDomainType );
							}
						}
					}
			);
			if ( !matchingDescriptors.isEmpty() ) {
				final SqmPolymorphicRootDescriptor<T> descriptor = new SqmPolymorphicRootDescriptor<>(
						typeConfiguration.getJavaTypeDescriptorRegistry().resolveDescriptor( javaType ),
						matchingDescriptors
				);
				polymorphicEntityReferenceMap.putIfAbsent( javaType, descriptor );
				return descriptor;
			}
		}

		throw new IllegalArgumentException( "Could not resolve entity reference : " + javaType.getName() );
	}

	public void processJpa(
			MetadataImplementor bootMetamodel,
			MappingMetamodel mappingMetamodel,
			Map<Class, String> entityProxyInterfaceMap,
			JpaStaticMetaModelPopulationSetting jpaStaticMetaModelPopulationSetting,
			Collection<NamedEntityGraphDefinition> namedEntityGraphDefinitions,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		bootMetamodel.getImports().forEach( ( k, v ) ->  this.nameToImportMap.put( k, new ImportInfo<>( v, null ) ) );
		this.entityProxyInterfaceMap.putAll( entityProxyInterfaceMap );

		final MetadataContext context = new MetadataContext(
				this,
				mappingMetamodel,
				bootMetamodel,
				jpaStaticMetaModelPopulationSetting,
				runtimeModelCreationContext
		);


		for ( PersistentClass entityBinding : bootMetamodel.getEntityBindings() ) {
			locateOrBuildEntityType( entityBinding, context, typeConfiguration );
		}
		handleUnusedMappedSuperclasses( context, typeConfiguration );

		context.wrapUp();

		this.jpaEntityTypeMap.putAll( context.getEntityTypesByEntityName() );
		this.jpaMappedSuperclassTypeMap.putAll( context.getMappedSuperclassTypeMap() );

		for ( EmbeddableDomainType<?> embeddable : context.getEmbeddableTypeSet() ) {
			this.jpaEmbeddableDescriptorMap.put( embeddable.getJavaType(), embeddable );
		}

		domainTypeStream( context ).forEach( (managedDomainType) -> managedDomainType.visitAttributes( persistentAttribute -> {
			if ( persistentAttribute.getJavaType() != null && persistentAttribute.getJavaType().isEnum() ) {
				@SuppressWarnings("unchecked")
				Class<Enum<?>> enumClass = (Class<Enum<?>>) persistentAttribute.getJavaType();
				Enum<?>[] enumConstants = enumClass.getEnumConstants();
				for ( Enum<?> enumConstant : enumConstants ) {
					String qualifiedEnumLiteral = enumConstant.getDeclaringClass()
							.getSimpleName() + "." + enumConstant.name();

					this.allowedEnumLiteralTexts.computeIfAbsent(
							enumConstant.name(),
							k -> new HashMap<>()
					).put( enumClass, enumConstant );
					this.allowedEnumLiteralTexts.computeIfAbsent(
							qualifiedEnumLiteral,
							k -> new HashMap<>()
					).put( enumClass, enumConstant );
				}
			}
		} ) );

		applyNamedEntityGraphs( namedEntityGraphDefinitions );
	}

	private static Stream<ManagedDomainType<?>> domainTypeStream(MetadataContext context) {
		return Stream.concat(
				context.getEntityTypesByEntityName().values().stream(),
				Stream.concat(
						context.getMappedSuperclassTypeMap().values().stream(),
						context.getEmbeddableTypeSet().stream()
				)
		);
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

		final Class<?> javaType = persistentClass.getMappedClass();
		final JavaType<?> javaTypeDescriptor;
		if ( javaType == null || Map.class.isAssignableFrom( javaType ) ) {
			// dynamic map
			javaTypeDescriptor = new DynamicModelJtd();
		}
		else {
			javaTypeDescriptor = context.getTypeConfiguration()
					.getJavaTypeDescriptorRegistry()
					.resolveEntityTypeDescriptor( javaType );
		}

		final EntityTypeImpl<?> entityType = new EntityTypeImpl(
				javaTypeDescriptor,
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
		final JavaType<?> javaTypeDescriptor = context.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.resolveManagedTypeDescriptor( mappedSuperclass.getMappedClass() );
		final MappedSuperclassTypeImpl<?> mappedSuperclassType = new MappedSuperclassTypeImpl(
				javaTypeDescriptor,
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
