/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import javax.persistence.EntityGraph;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.cfg.internal.DomainDataRegionConfigImpl;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.HEMLogging;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.internal.JpaMetaModelPopulationSetting;
import org.hibernate.metamodel.internal.JpaStaticMetaModelPopulationSetting;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.spi.DomainMetamodel;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Hibernate implementation of the JPA {@link javax.persistence.metamodel.Metamodel} contract.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 * @author Andrea Boriero
 */
public class DomainMetamodelImpl implements DomainMetamodel, MetamodelImplementor, Serializable {
	// todo : Integrate EntityManagerLogger into CoreMessageLogger
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( DomainMetamodelImpl.class );

	private static final Object ENTITY_NAME_RESOLVER_MAP_VALUE = new Object();
	private static final String INVALID_IMPORT = "";
	private static final String[] EMPTY_IMPLEMENTORS = new String[0];

	private final SessionFactoryImplementor sessionFactory;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JpaMetamodel

	private final JpaMetamodelImpl jpaMetamodel;

	//	private final Map<Class<?>, EntityDomainType<?>> jpaEntityTypeMap = new ConcurrentHashMap<>();
//	private final Map<String, EntityDomainType<?>> jpaEntityTypesByEntityName = new ConcurrentHashMap<>();
//	private final Map<Class<?>, MappedSuperclassType<?>> jpaMappedSuperclassTypeMap = new ConcurrentHashMap<>();
//	private final Set<EmbeddableDomainType<?>> jpaEmbeddableTypes = new CopyOnWriteArraySet<>();
	private final Map<Class, String> entityProxyInterfaceMap = new ConcurrentHashMap<>();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// RuntimeModel

	private final Map<String, EntityPersister> entityPersisterMap = new ConcurrentHashMap<>();
	private final Map<String, CollectionPersister> collectionPersisterMap = new ConcurrentHashMap<>();
	private final Map<String, Set<String>> collectionRolesByEntityParticipant = new ConcurrentHashMap<>();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainMetamodel

	private final ConcurrentMap<EntityNameResolver, Object> entityNameResolvers = new ConcurrentHashMap<>();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NOTE : Relational/mapping information is not part of the JPA metamodel
	// (type system).  However, this relational/mapping info *is* part of the
	// Hibernate metamodel.  This is a mismatch.  Normally this is not a
	// problem - ignoring Hibernate's representation mode (entity mode),
	// an entity (or mapped superclass) *Class* always refers to the same
	// EntityType (JPA) and EntityPersister (Hibernate)..  The problem is
	// in regards to embeddables.  For an embeddable, as with the rest of its
	// metamodel, Hibernate combines the embeddable's relational/mapping
	// while JPA does not.  This is consistent with each's model paradigm.
	// However, it causes a mismatch in that while JPA expects a single
	// "type descriptor" for a given embeddable class, Hibernate incorporates
	// the relational/mapping info so we have a "type descriptor" for each
	// usage of that embeddable.  Think embeddable versus embedded.
	//
	// To account for this, we track both paradigms here...

	/**
	 * There can be multiple instances of an Embeddable type, each one being relative to its parent entity.
	 */

	/**
	 * That's not strictly correct in the JPA standard since for a given Java type we could have
	 * multiple instances of an embeddable type. Some embeddable might override attributes, but we
	 * can only return a single EmbeddableTypeImpl for a given Java object class.
	 * <p>
	 * A better approach would be if the parent class and attribute name would be included as well
	 * when trying to locate the embeddable type.
	 */
//	private final Map<Class<?>, EmbeddableDomainType<?>> jpaEmbeddableTypeMap = new ConcurrentHashMap<>();
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private final TypeConfiguration typeConfiguration;

	private final Map<String, String[]> implementorsCache = new ConcurrentHashMap<>();

	public DomainMetamodelImpl(SessionFactoryImplementor sessionFactory, TypeConfiguration typeConfiguration) {
		this.sessionFactory = sessionFactory;
		this.typeConfiguration = typeConfiguration;
		this.jpaMetamodel = new JpaMetamodelImpl( typeConfiguration );
	}

	/**
	 * Prepare the metamodel using the information from the collection of Hibernate
	 * {@link PersistentClass} models
	 *
	 * @param mappingMetadata The mapping information
	 * @param jpaMetaModelPopulationSetting Should the JPA Metamodel be built as well?
	 */
	public void initialize(
			MetadataImplementor mappingMetadata,
			JpaMetaModelPopulationSetting jpaMetaModelPopulationSetting) {

		primeSecondLevelCacheRegions( mappingMetadata );

		final PersisterCreationContext persisterCreationContext = new PersisterCreationContext() {
			@Override
			public SessionFactoryImplementor getSessionFactory() {
				return sessionFactory;
			}

			@Override
			public MetadataImplementor getMetadata() {
				return mappingMetadata;
			}
		};

		final PersisterFactory persisterFactory = sessionFactory.getServiceRegistry()
				.getService( PersisterFactory.class );

		for ( final PersistentClass model : mappingMetadata.getEntityBindings() ) {
			final NavigableRole rootEntityRole = new NavigableRole( model.getRootClass().getEntityName() );
			final EntityDataAccess accessStrategy = sessionFactory.getCache().getEntityRegionAccess( rootEntityRole );
			final NaturalIdDataAccess naturalIdAccessStrategy = sessionFactory.getCache()
					.getNaturalIdCacheRegionAccessStrategy( rootEntityRole );

			final EntityPersister cp = persisterFactory.createEntityPersister(
					model,
					accessStrategy,
					naturalIdAccessStrategy,
					persisterCreationContext
			);
			entityPersisterMap.put( model.getEntityName(), cp );

			if ( cp.getConcreteProxyClass() != null
					&& cp.getConcreteProxyClass().isInterface()
					&& !Map.class.isAssignableFrom( cp.getConcreteProxyClass() )
					&& cp.getMappedClass() != cp.getConcreteProxyClass() ) {
				// IMPL NOTE : we exclude Map based proxy interfaces here because that should
				//		indicate MAP entity mode.0

				if ( cp.getMappedClass().equals( cp.getConcreteProxyClass() ) ) {
					// this part handles an odd case in the Hibernate test suite where we map an interface
					// as the class and the proxy.  I cannot think of a real life use case for that
					// specific test, but..
					log.debugf(
							"Entity [%s] mapped same interface [%s] as class and proxy",
							cp.getEntityName(),
							cp.getMappedClass()
					);
				}
				else {
					final String old = entityProxyInterfaceMap.put( cp.getConcreteProxyClass(), cp.getEntityName() );
					if ( old != null ) {
						throw new HibernateException(
								String.format(
										Locale.ENGLISH,
										"Multiple entities [%s, %s] named the same interface [%s] as their proxy which is not supported",
										old,
										cp.getEntityName(),
										cp.getConcreteProxyClass().getName()
								)
						);
					}
				}
			}
		}

		for ( final Collection model : mappingMetadata.getCollectionBindings() ) {
			final NavigableRole navigableRole = new NavigableRole( model.getRole() );

			final CollectionDataAccess accessStrategy = sessionFactory.getCache().getCollectionRegionAccess(
					navigableRole );

			final CollectionPersister persister = persisterFactory.createCollectionPersister(
					model,
					accessStrategy,
					persisterCreationContext
			);
			collectionPersisterMap.put( model.getRole(), persister );
			Type indexType = persister.getIndexType();
			if ( indexType != null && indexType.isAssociationType() && !indexType.isAnyType() ) {
				String entityName = ( (AssociationType) indexType ).getAssociatedEntityName( sessionFactory );
				Set<String> roles = collectionRolesByEntityParticipant.get( entityName );
				if ( roles == null ) {
					roles = new HashSet<>();
					collectionRolesByEntityParticipant.put( entityName, roles );
				}
				roles.add( persister.getRole() );
			}
			Type elementType = persister.getElementType();
			if ( elementType.isAssociationType() && !elementType.isAnyType() ) {
				String entityName = ( (AssociationType) elementType ).getAssociatedEntityName( sessionFactory );
				Set<String> roles = collectionRolesByEntityParticipant.get( entityName );
				if ( roles == null ) {
					roles = new HashSet<>();
					collectionRolesByEntityParticipant.put( entityName, roles );
				}
				roles.add( persister.getRole() );
			}
		}

		// after *all* persisters and named queries are registered
		entityPersisterMap.values().forEach( EntityPersister::generateEntityDefinition );

		for ( EntityPersister persister : entityPersisterMap.values() ) {
			persister.postInstantiate();
			registerEntityNameResolvers( persister, entityNameResolvers );
		}
		collectionPersisterMap.values().forEach( CollectionPersister::postInstantiate );

		jpaMetamodel.initialize(
				this,
				mappingMetadata,
				sessionFactory.getQueryEngine().getCriteriaBuilder(),
				jpaMetaModelPopulationSetting,
				JpaStaticMetaModelPopulationSetting.determineJpaMetaModelPopulationSetting( sessionFactory.getProperties() )
		);
	}

	private void primeSecondLevelCacheRegions(MetadataImplementor mappingMetadata) {
		final Map<String, DomainDataRegionConfigImpl.Builder> regionConfigBuilders = new ConcurrentHashMap<>();

		// todo : ultimately this code can be made more efficient when we have a better intrinsic understanding of the hierarchy as a whole

		for ( PersistentClass bootEntityDescriptor : mappingMetadata.getEntityBindings() ) {
			final AccessType accessType = AccessType.fromExternalName( bootEntityDescriptor.getCacheConcurrencyStrategy() );

			if ( accessType != null ) {
				if ( bootEntityDescriptor.isCached() ) {
					regionConfigBuilders.computeIfAbsent(
							bootEntityDescriptor.getRootClass().getCacheRegionName(),
							DomainDataRegionConfigImpl.Builder::new
					)
							.addEntityConfig( bootEntityDescriptor, accessType );
				}

				if ( RootClass.class.isInstance( bootEntityDescriptor )
						&& bootEntityDescriptor.hasNaturalId()
						&& bootEntityDescriptor.getNaturalIdCacheRegionName() != null ) {
					regionConfigBuilders.computeIfAbsent(
							bootEntityDescriptor.getNaturalIdCacheRegionName(),
							DomainDataRegionConfigImpl.Builder::new
					)
							.addNaturalIdConfig( (RootClass) bootEntityDescriptor, accessType );
				}
			}
		}

		for ( Collection collection : mappingMetadata.getCollectionBindings() ) {
			final AccessType accessType = AccessType.fromExternalName( collection.getCacheConcurrencyStrategy() );
			if ( accessType != null ) {
				regionConfigBuilders.computeIfAbsent(
						collection.getCacheRegionName(),
						DomainDataRegionConfigImpl.Builder::new
				)
						.addCollectionConfig( collection, accessType );
			}
		}

		final Set<DomainDataRegionConfig> regionConfigs;
		if ( regionConfigBuilders.isEmpty() ) {
			regionConfigs = Collections.emptySet();
		}
		else {
			regionConfigs = new HashSet<>();
			for ( DomainDataRegionConfigImpl.Builder builder : regionConfigBuilders.values() ) {
				regionConfigs.add( builder.build() );
			}
		}

		getSessionFactory().getCache().prime( regionConfigs );
	}


	@Override
	public java.util.Collection<EntityNameResolver> getEntityNameResolvers() {
		return entityNameResolvers.keySet();
	}

	private static void registerEntityNameResolvers(
			EntityPersister persister,
			Map<EntityNameResolver, Object> entityNameResolvers) {
		if ( persister.getEntityMetamodel() == null || persister.getEntityMetamodel().getTuplizer() == null ) {
			return;
		}
		registerEntityNameResolvers( persister.getEntityMetamodel().getTuplizer(), entityNameResolvers );
	}

	private static void registerEntityNameResolvers(
			EntityTuplizer tuplizer,
			Map<EntityNameResolver, Object> entityNameResolvers) {
		EntityNameResolver[] resolvers = tuplizer.getEntityNameResolvers();
		if ( resolvers == null ) {
			return;
		}

		for ( EntityNameResolver resolver : resolvers ) {
			entityNameResolvers.put( resolver, ENTITY_NAME_RESOLVER_MAP_VALUE );
		}
	}

//	/**
//	 * Instantiate the metamodel.
//	 *
//	 * @param entityNameResolvers
//	 * @param entities The entity mappings.
//	 * @param embeddables The embeddable (component) mappings.
//	 * @param mappedSuperclassTypeMap The {@link javax.persistence.MappedSuperclass} mappings
//	 */
//	private MetamodelImpl(
//			SessionFactoryImplementor sessionFactory,
//			Map<String, String> imports,
//			Map<String, EntityPersister> entityPersisterMap,
//			Map<Class, String> entityProxyInterfaceMap,
//			ConcurrentHashMap<EntityNameResolver, Object> entityNameResolvers,
//			Map<String, CollectionPersister> collectionPersisterMap,
//			Map<String, Set<String>> collectionRolesByEntityParticipant,
//			Map<Class<?>, EntityTypeImpl<?>> entities,
//			Map<Class<?>, EmbeddableTypeImpl<?>> embeddables,
//			Map<Class<?>, MappedSuperclassType<?>> mappedSuperclassTypeMap,
//			Map<String, EntityTypeImpl<?>> entityTypesByEntityName) {
//		this.sessionFactory = sessionFactory;
//		this.imports = imports;
//		this.entityPersisterMap = entityPersisterMap;
//		this.entityProxyInterfaceMap = entityProxyInterfaceMap;
//		this.entityNameResolvers = entityNameResolvers;
//		this.collectionPersisterMap = collectionPersisterMap;
//		this.collectionRolesByEntityParticipant = collectionRolesByEntityParticipant;
//		this.entities = entities;
//		this.embeddables = embeddables;
//		this.mappedSuperclassTypeMap = mappedSuperclassTypeMap;
//		this.entityTypesByEntityName = entityTypesByEntityName;
//	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public JpaMetamodel getJpaMetamodel() {
		return this.jpaMetamodel;
	}

	@Override
	public EntityPersister determineEntityPersister(Object entity) {
		return findEntityDescriptor(entity.getClass());
	}

	@Override
	public void visitEntityDescriptors(Consumer<EntityPersister> action){
		entityPersisterMap.values().forEach( action );
	}

	@Override
	public EntityPersister getEntityDescriptor(String entityName){
		final EntityPersister entityPersister = entityPersisterMap.get( entityName );
		if ( entityPersister == null ) {
			throw new IllegalArgumentException( "Unable to locate persister: " + entityName );
		}
		return entityPersister;
	}

	@Override
	public EntityPersister findEntityDescriptor(String entityName){
		return entityPersisterMap.get( entityName );
	}

	@Override
	public EntityPersister findEntityDescriptor(Class entityJavaType){
		return findEntityDescriptor( entityJavaType.getName() );
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public EntityPersister getEntityDescriptor(Class entityJavaType){
		EntityPersister entityPersister = entityPersisterMap.get( entityJavaType.getName() );
		if ( entityPersister == null ) {
			String mappedEntityName = entityProxyInterfaceMap.get( entityJavaType );
			if ( mappedEntityName != null ) {
				entityPersister = entityPersisterMap.get( mappedEntityName );
			}
		}

		if ( entityPersister == null ) {
			throw new IllegalArgumentException( "Unable to locate persister: " + entityJavaType.getName() );
		}

		return entityPersister;
	}

	@Override
	public EntityPersister locateEntityDescriptor(Class byClass){
		EntityPersister entityPersister = entityPersisterMap.get( byClass.getName() );
		if ( entityPersister == null ) {
			String mappedEntityName = entityProxyInterfaceMap.get( byClass );
			if ( mappedEntityName != null ) {
				entityPersister = entityPersisterMap.get( mappedEntityName );
			}
		}

		if ( entityPersister == null ) {
			throw new UnknownEntityTypeException( "Unable to locate persister: " + byClass.getName() );
		}

		return entityPersister;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> EntityDomainType<X> entity(Class<X> cls) {
		return getJpaMetamodel().entity( cls );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> ManagedDomainType<X> managedType(Class<X> cls) {
		return getJpaMetamodel().managedType( cls );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> EmbeddableDomainType<X> embeddable(Class<X> cls) {
		return getJpaMetamodel().embeddable( cls );
	}

	@Override
	public Set<ManagedType<?>> getManagedTypes() {
		return getJpaMetamodel().getManagedTypes();
	}

	@Override
	public Set<EntityType<?>> getEntities() {
		return getJpaMetamodel().getEntities();
	}

	@Override
	public Set<EmbeddableType<?>> getEmbeddables() {
		return getJpaMetamodel().getEmbeddables();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> EntityDomainType<X> entity(String entityName) {
		return getJpaMetamodel().entity( entityName );
	}

	@Override
	public String[] getImplementors(String className) throws MappingException {
		// computeIfAbsent() can be a contention point and we expect all the values to be in the map at some point so
		// let's do an optimistic check first
		String[] implementors = implementorsCache.get( className );
		if ( implementors != null ) {
			return Arrays.copyOf( implementors, implementors.length );
		}

		try {
			final Class<?> clazz = getSessionFactory().getServiceRegistry()
					.getService( ClassLoaderService.class )
					.classForName( className );
			implementors = doGetImplementors( clazz );
			if ( implementors.length > 0 ) {
				implementorsCache.putIfAbsent( className, implementors );
				return Arrays.copyOf( implementors, implementors.length );
			}
			else {
				return EMPTY_IMPLEMENTORS;
			}
		}
		catch (ClassLoadingException e) {
			return new String[] { className }; // we don't cache anything for dynamic classes
		}
	}

	@Override
	public Map<String, EntityPersister> entityPersisters() {
		return entityPersisterMap;
	}

	@Override
	public CollectionPersister collectionPersister(String role) {
		final CollectionPersister persister = collectionPersisterMap.get( role );
		if ( persister == null ) {
			throw new MappingException( "Could not locate CollectionPersister for role : " + role );
		}
		return persister;
	}

	@Override
	public Map<String, CollectionPersister> collectionPersisters() {
		return collectionPersisterMap;
	}

	@Override
	public EntityPersister entityPersister(Class entityClass) {
		return entityPersister( entityClass.getName() );
	}

	@Override
	public EntityPersister entityPersister(String entityName) throws MappingException {
		EntityPersister result = entityPersisterMap.get( entityName );
		if ( result == null ) {
			throw new MappingException( "Unknown entity: " + entityName );
		}
		return result;
	}

	@Override
	public EntityPersister locateEntityPersister(String byName) {
		final EntityPersister entityPersister = entityPersisterMap.get( byName );
		if ( entityPersister == null ) {
			throw new UnknownEntityTypeException( "Unable to locate persister: " + byName );
		}
		return entityPersister;
	}

	@Override
	public void visitCollectionDescriptors(Consumer<CollectionPersister> action){
		collectionPersisterMap.values().forEach( action );
	}

	@Override
	public CollectionPersister getCollectionDescriptor(String role){
		CollectionPersister collectionPersister = collectionPersisterMap.get( role );
		if(collectionPersister == null){
			throw new IllegalArgumentException( "Unable to locate persister: " + role );
		}
		return collectionPersister;
	}

	@Override
	public CollectionPersister findCollectionDescriptor(String role){
		return collectionPersisterMap.get( role );
	}

	@Override
	public Set<String> getCollectionRolesByEntityParticipant(String entityName) {
		return collectionRolesByEntityParticipant.get( entityName );
	}

	@Override
	public String[] getAllEntityNames() {
		return ArrayHelper.toStringArray( entityPersisterMap.keySet() );
	}

	@Override
	public String[] getAllCollectionRoles() {
		return ArrayHelper.toStringArray( collectionPersisterMap.keySet() );
	}

	@Override
	public <T> void addNamedEntityGraph(String graphName, RootGraphImplementor<T> entityGraph) {
		jpaMetamodel.addNamedEntityGraph( graphName, entityGraph );
	}

	@Override
	public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
		addNamedEntityGraph( graphName, (RootGraphImplementor<T>) entityGraph );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> RootGraphImplementor<T> findEntityGraphByName(String name) {
		return getJpaMetamodel().findEntityGraphByName( name );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<RootGraphImplementor<? super T>> findEntityGraphsByJavaType(Class<T> entityClass) {
		return getJpaMetamodel().findEntityGraphsByJavaType( entityClass );
	}

	@Override
	public RootGraph<?> findNamedGraph(String name){
		return findEntityGraphByName( name );
	}

	@Override
	public void close() {
		// anything to do ?
	}

	private String[] doGetImplementors(Class<?> clazz) throws MappingException {
		ArrayList<String> results = new ArrayList<>();
		for ( EntityPersister checkPersister : entityPersisters().values() ) {
			if ( !Queryable.class.isInstance( checkPersister ) ) {
				continue;
			}
			final Queryable checkQueryable = Queryable.class.cast( checkPersister );
			final String checkQueryableEntityName = checkQueryable.getEntityName();
			final boolean isMappedClass = clazz.getName().equals( checkQueryableEntityName );
			if ( checkQueryable.isExplicitPolymorphism() ) {
				if ( isMappedClass ) {
					return new String[] { clazz.getName() }; // NOTE EARLY EXIT
				}
			}
			else {
				if ( isMappedClass ) {
					results.add( checkQueryableEntityName );
				}
				else {
					final Class<?> mappedClass = checkQueryable.getMappedClass();
					if ( mappedClass != null && clazz.isAssignableFrom( mappedClass ) ) {
						final boolean assignableSuperclass;
						if ( checkQueryable.isInherited() ) {
							Class<?> mappedSuperclass = entityPersister( checkQueryable.getMappedSuperclass() ).getMappedClass();
							assignableSuperclass = clazz.isAssignableFrom( mappedSuperclass );
						}
						else {
							assignableSuperclass = false;
						}
						if ( !assignableSuperclass ) {
							results.add( checkQueryableEntityName );
						}
					}
				}
			}
		}

		return results.toArray( new String[results.size()] );
	}
}
