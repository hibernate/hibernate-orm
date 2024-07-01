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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.HEMLogging;
import org.hibernate.internal.QueryParameterBindingTypeResolverImpl;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.TupleType;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.query.BindableType;
import org.hibernate.query.derived.AnonymousTupleSqmPathSource;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmFieldLiteral;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.type.BasicType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;

import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_STRING_ARRAY;
import static org.hibernate.metamodel.internal.JpaMetaModelPopulationSetting.determineJpaMetaModelPopulationSetting;
import static org.hibernate.metamodel.internal.JpaStaticMetaModelPopulationSetting.determineJpaStaticMetaModelPopulationSetting;

/**
 * Implementation of the JPA-defined contract {@link jakarta.persistence.metamodel.Metamodel}.
 * <p>
 * Really more of the {@linkplain MappingMetamodel mapping model} than the domain model, though
 * it does have reference to the {@link org.hibernate.metamodel.model.domain.JpaMetamodel}.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 * @author Andrea Boriero
 */
public class MappingMetamodelImpl extends QueryParameterBindingTypeResolverImpl
		implements MappingMetamodelImplementor, MetamodelImplementor, Serializable {
	// todo : Integrate EntityManagerLogger into CoreMessageLogger
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( MappingMetamodelImpl.class );

	//NOTE: we suppress deprecation warnings because at the moment we
	//implement a deprecated API so have to override deprecated things

	private static final String[] EMPTY_IMPLEMENTORS = EMPTY_STRING_ARRAY;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JpaMetamodel

	private final JpaMetamodelImplementor jpaMetamodel;

	private final Map<Class<?>, String> entityProxyInterfaceMap = new ConcurrentHashMap<>();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// RuntimeModel

	private final EntityPersisterConcurrentMap entityPersisterMap = new EntityPersisterConcurrentMap();
	private final Map<String, CollectionPersister> collectionPersisterMap = new ConcurrentHashMap<>();
	private final Map<String, Set<String>> collectionRolesByEntityParticipant = new ConcurrentHashMap<>();

	private final Map<NavigableRole, EmbeddableValuedModelPart> embeddableValuedModelPart = new ConcurrentHashMap<>();
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainMetamodel

	private final Set<EntityNameResolver> entityNameResolvers = new HashSet<>();


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

	/*
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

	private final Map<String, String[]> implementorsCache = new ConcurrentHashMap<>();
	private final Map<TupleType<?>, MappingModelExpressible<?>> tupleTypeCache = new ConcurrentHashMap<>();

	public MappingMetamodelImpl(TypeConfiguration typeConfiguration, ServiceRegistry serviceRegistry) {
		jpaMetamodel = new JpaMetamodelImpl( typeConfiguration, this, serviceRegistry );
	}

	public JpaMetamodelImplementor getJpaMetamodel() {
		return jpaMetamodel;
	}

	public void finishInitialization(RuntimeModelCreationContext context) {
		final MetadataImplementor bootModel = context.getBootModel();
		bootModel.visitRegisteredComponents( Component::prepareForMappingModel );
		bootModel.getMappedSuperclassMappingsCopy().forEach( MappedSuperclass::prepareForMappingModel );
		bootModel.getEntityBindings().forEach( persistentClass -> persistentClass.prepareForMappingModel( context ) );

		final PersisterFactory persisterFactory =
				jpaMetamodel.getServiceRegistry().requireService( PersisterFactory.class );
		final CacheImplementor cache = context.getCache();
		processBootEntities(
				bootModel.getEntityBindings(),
				cache,
				persisterFactory,
				context
		);
		processBootCollections(
				bootModel.getCollectionBindings(),
				cache,
				persisterFactory,
				context
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// after *all* persisters and named queries are registered

		MappingModelCreationProcess.process( entityPersisterMap, context );

		for ( EntityPersister persister : entityPersisterMap.values() ) {
			persister.postInstantiate();
			registerEntityNameResolvers( persister, entityNameResolvers );
		}

		for ( EntityPersister persister : entityPersisterMap.values() ) {
			persister.prepareLoaders();
		}

		collectionPersisterMap.values().forEach( CollectionPersister::postInstantiate );

		registerEmbeddableMappingType( bootModel );

		final Map<String, Object> settings = context.getSettings();
		( (JpaMetamodelImpl) jpaMetamodel ).processJpa(
				bootModel,
				this,
				entityProxyInterfaceMap,
				determineJpaStaticMetaModelPopulationSetting( settings ),
				determineJpaMetaModelPopulationSetting( settings ),
				bootModel.getNamedEntityGraphs().values(),
				context
		);
	}

	private void registerEmbeddableMappingType(MetadataImplementor bootModel) {
		bootModel.visitRegisteredComponents(
				composite -> {
					final EmbeddableValuedModelPart mappingModelPart = ((ComponentType) composite.getType()).getMappingModelPart();
					embeddableValuedModelPart.put(
							mappingModelPart.getNavigableRole(),
							mappingModelPart
					);
				}
		);
	}

	private void processBootEntities(
			java.util.Collection<PersistentClass> entityBindings,
			CacheImplementor cacheImplementor,
			PersisterFactory persisterFactory,
			RuntimeModelCreationContext modelCreationContext) {
		for ( final PersistentClass model : entityBindings ) {
			final NavigableRole rootEntityRole = new NavigableRole( model.getRootClass().getEntityName() );
			final EntityDataAccess accessStrategy = cacheImplementor.getEntityRegionAccess( rootEntityRole );
			final NaturalIdDataAccess naturalIdAccessStrategy = cacheImplementor
					.getNaturalIdCacheRegionAccessStrategy( rootEntityRole );

			final EntityPersister cp = persisterFactory.createEntityPersister(
					model,
					accessStrategy,
					naturalIdAccessStrategy,
					modelCreationContext
			);
			entityPersisterMap.put( model.getEntityName(), cp );
			// Also register the persister under the class name if available,
			// otherwise the getEntityDescriptor(Class) won't work for entities with custom entity names
			if ( model.getClassName() != null && !model.getClassName().equals( model.getEntityName() ) ) {
				// But only if the class name is not registered already,
				// as we can have the same class mapped to multiple entity names
				entityPersisterMap.putIfAbsent( model.getClassName(), cp );
			}

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
					if ( log.isDebugEnabled() ) {
						log.debugf(
								"Entity [%s] mapped same interface [%s] as class and proxy",
								cp.getEntityName(),
								cp.getMappedClass()
						);
					}
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
	}

	private void processBootCollections(
			java.util.Collection<Collection> collectionBindings,
			CacheImplementor cacheImplementor,
			PersisterFactory persisterFactory,
			RuntimeModelCreationContext modelCreationContext) {
		for ( final Collection model : collectionBindings ) {
			final NavigableRole navigableRole = new NavigableRole( model.getRole() );

			final CollectionDataAccess accessStrategy = cacheImplementor.getCollectionRegionAccess(
					navigableRole );

			final CollectionPersister persister = persisterFactory.createCollectionPersister(
					model,
					accessStrategy,
					modelCreationContext
			);
			collectionPersisterMap.put( model.getRole(), persister );
			Type indexType = persister.getIndexType();
			if ( indexType instanceof org.hibernate.type.EntityType ) {
				String entityName = ( (org.hibernate.type.EntityType) indexType ).getAssociatedEntityName();
				Set<String> roles = collectionRolesByEntityParticipant.get( entityName );
				//noinspection Java8MapApi
				if ( roles == null ) {
					roles = new HashSet<>();
					collectionRolesByEntityParticipant.put( entityName, roles );
				}
				roles.add( persister.getRole() );
			}
			Type elementType = persister.getElementType();
			if ( elementType instanceof org.hibernate.type.EntityType ) {
				String entityName = ( (org.hibernate.type.EntityType) elementType ).getAssociatedEntityName();
				Set<String> roles = collectionRolesByEntityParticipant.get( entityName );
				//noinspection Java8MapApi
				if ( roles == null ) {
					roles = new HashSet<>();
					collectionRolesByEntityParticipant.put( entityName, roles );
				}
				roles.add( persister.getRole() );
			}
		}
	}

	private static void registerEntityNameResolvers(
			EntityPersister persister,
			Set<EntityNameResolver> entityNameResolvers) {
		if ( persister.getRepresentationStrategy() == null ) {
			return;
		}
		registerEntityNameResolvers( persister.getRepresentationStrategy(), entityNameResolvers );
	}

	private static void registerEntityNameResolvers(
			EntityRepresentationStrategy representationStrategy,
			Set<EntityNameResolver> entityNameResolvers) {
		representationStrategy.visitEntityNameResolvers( entityNameResolvers::add );
	}

	@Override @SuppressWarnings("deprecation")
	public java.util.Collection<EntityNameResolver> getEntityNameResolvers() {
		return entityNameResolvers;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return jpaMetamodel.getTypeConfiguration();
	}

	@Override
	public MappingMetamodel getMappingMetamodel() {
		return this;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return jpaMetamodel.getServiceRegistry();
	}

	@Override
	public void forEachEntityDescriptor(Consumer<EntityPersister> action) {
		for ( EntityPersister value : entityPersisterMap.values() ) {
			action.accept( value );
		}
	}

	@Override
	public Stream<EntityPersister> streamEntityDescriptors() {
		return Arrays.stream( entityPersisterMap.values() );
	}

	@Override
	public EntityPersister getEntityDescriptor(String entityName) {
		final EntityPersister entityPersister = entityPersisterMap.get( entityName );
		if ( entityPersister == null ) {
			throw new UnknownEntityTypeException( "Unable to locate persister: " + entityName );
		}
		return entityPersister;
	}

	@Override
	public EntityPersister getEntityDescriptor(NavigableRole name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public EmbeddableValuedModelPart getEmbeddableValuedModelPart(NavigableRole role){
		EmbeddableValuedModelPart embeddableMappingType = embeddableValuedModelPart.get( role );
		if ( embeddableMappingType == null ) {
			throw new IllegalArgumentException( "Unable to locate EmbeddableValuedModelPart: " + role );
		}
		return embeddableMappingType;
	}

	@Override
	public EntityPersister findEntityDescriptor(String entityName) {
		return entityPersisterMap.get( entityName );
	}

	@Override
	public EntityPersister findEntityDescriptor(Class<?> entityJavaType) {
		return findEntityDescriptor( entityJavaType.getName() );
	}

	@Override
	public boolean isEntityClass(Class<?> entityJavaType) {
		return entityPersisterMap.containsKey( entityJavaType.getName() );
	}

	@Override
	public EntityPersister getEntityDescriptor(Class<?> entityJavaType) {
		EntityPersister entityPersister = entityPersisterMap.get( entityJavaType.getName() );
		if ( entityPersister == null ) {
			String mappedEntityName = entityProxyInterfaceMap.get( entityJavaType );
			if ( mappedEntityName != null ) {
				entityPersister = entityPersisterMap.get( mappedEntityName );
			}
		}

		if ( entityPersister == null ) {
			throw new UnknownEntityTypeException( "Unable to locate entity descriptor: " + entityJavaType.getName() );
		}

		return entityPersister;
	}

	@Override
	public EntityPersister locateEntityDescriptor(Class<?> byClass) {
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
	public <X> EntityDomainType<X> entity(Class<X> cls) {
		return jpaMetamodel.entity( cls );
	}

	@Override
	public <X> ManagedDomainType<X> managedType(Class<X> cls) {
		return jpaMetamodel.managedType( cls );
	}

	@Override
	public <X> EmbeddableDomainType<X> embeddable(Class<X> cls) {
		return jpaMetamodel.embeddable( cls );
	}

	@Override
	public Set<ManagedType<?>> getManagedTypes() {
		return jpaMetamodel.getManagedTypes();
	}

	@Override
	public Set<EntityType<?>> getEntities() {
		return jpaMetamodel.getEntities();
	}

	@Override
	public Set<EmbeddableType<?>> getEmbeddables() {
		return jpaMetamodel.getEmbeddables();
	}

	@Override
	public <X> ManagedDomainType<X> managedType(String typeName) {
		return jpaMetamodel.managedType( typeName );
	}

	@Override
	public <X> EntityDomainType<X> entity(String entityName) {
		return jpaMetamodel.entity( entityName );
	}

	@Override
	public <X> EmbeddableDomainType<X> embeddable(String embeddableName) {
		return jpaMetamodel.embeddable( embeddableName );
	}

	@Override
	public <X> EntityDomainType<X> getHqlEntityReference(String entityName) {
		return jpaMetamodel.getHqlEntityReference( entityName );
	}

	@Override
	public <X> EntityDomainType<X> resolveHqlEntityReference(String entityName) {
		return jpaMetamodel.resolveHqlEntityReference( entityName );
	}

	@Override
	public <X> ManagedDomainType<X> findManagedType(Class<X> cls) {
		return jpaMetamodel.findManagedType( cls );
	}

	@Override
	public <X> EntityDomainType<X> findEntityType(Class<X> cls) {
		return jpaMetamodel.findEntityType( cls );
	}

	@Override
	public String qualifyImportableName(String queryName) {
		return jpaMetamodel.qualifyImportableName( queryName );
	}

	@Override
	public Set<String> getEnumTypesForValue(String enumValue) {
		return jpaMetamodel.getEnumTypesForValue(enumValue);
	}

	@Override
	public JavaType<?> getJavaConstantType(String className, String fieldName) {
		return jpaMetamodel.getJavaConstantType( className, fieldName );
	}

	@Override
	public <T> T getJavaConstant(String className, String fieldName) {
		return jpaMetamodel.getJavaConstant( className, fieldName );
	}

	@Override
	public EnumJavaType<?> getEnumType(String className) {
		return jpaMetamodel.getEnumType(className);
	}

	@Override
	public <E extends Enum<E>> E enumValue(EnumJavaType<E> enumType, String enumValueName) {
		return jpaMetamodel.enumValue( enumType, enumValueName );
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
			final Class<?> clazz =
					jpaMetamodel.getServiceRegistry().requireService( ClassLoaderService.class )
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

	@Override @SuppressWarnings("deprecation")
	public Map<String, EntityPersister> entityPersisters() {
		return entityPersisterMap.convertToMap();
	}

	@Override @SuppressWarnings("deprecation")
	public CollectionPersister collectionPersister(String role) {
		final CollectionPersister persister = collectionPersisterMap.get( role );
		if ( persister == null ) {
			throw new MappingException( "Could not locate CollectionPersister for role : " + role );
		}
		return persister;
	}

	@Override @SuppressWarnings("deprecation")
	public Map<String, CollectionPersister> collectionPersisters() {
		return collectionPersisterMap;
	}

	@Override @SuppressWarnings("deprecation")
	public EntityPersister entityPersister(Class<?> entityClass) {
		return getEntityDescriptor( entityClass.getName() );
	}

	@Override @SuppressWarnings("deprecation")
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
	public String getImportedName(String name) {
		final String qualifiedName = jpaMetamodel.qualifyImportableName( name );
		return qualifiedName == null ? name : qualifiedName;
	}

	@Override
	public void forEachCollectionDescriptor(Consumer<CollectionPersister> action) {
		collectionPersisterMap.values().forEach( action );
	}

	@Override
	public Stream<CollectionPersister> streamCollectionDescriptors() {
		return collectionPersisterMap.values().stream();
	}

	@Override
	public CollectionPersister getCollectionDescriptor(String role) {
		CollectionPersister collectionPersister = collectionPersisterMap.get( role );
		if ( collectionPersister == null ) {
			throw new IllegalArgumentException( "Unable to locate persister: " + role );
		}
		return collectionPersister;
	}

	@Override
	public CollectionPersister getCollectionDescriptor(NavigableRole role) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CollectionPersister findCollectionDescriptor(NavigableRole role) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CollectionPersister findCollectionDescriptor(String role) {
		return collectionPersisterMap.get( role );
	}

	@Override @SuppressWarnings("deprecation")
	public Set<String> getCollectionRolesByEntityParticipant(String entityName) {
		return collectionRolesByEntityParticipant.get( entityName );
	}

	@Override
	public String[] getAllEntityNames() {
		return entityPersisterMap.keys();
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
	public <T> RootGraphImplementor<T> findEntityGraphByName(String name) {
		return jpaMetamodel.findEntityGraphByName( name );
	}

	@Override
	public <T> List<RootGraphImplementor<? super T>> findEntityGraphsByJavaType(Class<T> entityClass) {
		return jpaMetamodel.findEntityGraphsByJavaType( entityClass );
	}

	@Override
	public JpaCompliance getJpaCompliance() {
		return jpaMetamodel.getJpaCompliance();
	}

	@Override
	public RootGraph<?> findNamedGraph(String name) {
		return findEntityGraphByName( name );
	}

	@Override
	public void forEachNamedGraph(Consumer<RootGraph<?>> action) {
		throw new UnsupportedOperationException();
	}

	@Override
	public RootGraph<?> defaultGraph(String entityName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public RootGraph<?> defaultGraph(Class entityJavaType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public RootGraph<?> defaultGraph(EntityPersister entityDescriptor) {
		throw new UnsupportedOperationException();
	}

	@Override
	public RootGraph<?> defaultGraph(EntityDomainType<?> entityDomainType) {
		return null;
	}

	@Override
	public List<RootGraph<?>> findRootGraphsForType(Class baseEntityJavaType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<RootGraph<?>> findRootGraphsForType(String baseEntityName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<RootGraph<?>> findRootGraphsForType(EntityPersister baseEntityDescriptor) {
		return null;
	}

	@Override
	public void close() {
		// anything to do ?
	}

	private String[] doGetImplementors(Class<?> clazz) throws MappingException {
		ArrayList<String> results = new ArrayList<>();
		for ( EntityPersister checkPersister : entityPersisters().values() ) {
			if ( checkPersister instanceof Queryable ) {
				final String checkQueryableEntityName = ((EntityMappingType) checkPersister).getEntityName();
				final boolean isMappedClass = clazz.getName().equals( checkQueryableEntityName );
				if ( checkPersister.isExplicitPolymorphism() ) {
					if ( isMappedClass ) {
						return new String[] { clazz.getName() }; // NOTE EARLY EXIT
					}
				}
				else {
					if ( isMappedClass ) {
						results.add( checkQueryableEntityName );
					}
					else {
						final Class<?> mappedClass = checkPersister.getMappedClass();
						if ( mappedClass != null && clazz.isAssignableFrom( mappedClass ) ) {
							final boolean assignableSuperclass;
							if ( checkPersister.isInherited() ) {
								final String superTypeName = checkPersister.getSuperMappingType().getEntityName();
								Class<?> mappedSuperclass = getEntityDescriptor( superTypeName ).getMappedClass();
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
		}

		return results.toArray( EMPTY_STRING_ARRAY );
	}

	@Override
	public MappingModelExpressible<?> resolveMappingExpressible(
			SqmExpressible<?> sqmExpressible,
			Function<NavigablePath, TableGroup> tableGroupLocator) {
		if ( sqmExpressible instanceof SqmPath<?> ) {
			final SqmPath<?> sqmPath = (SqmPath<?>) sqmExpressible;
			final DomainType<?> sqmPathType = sqmPath.getResolvedModel().getSqmPathType();
			if ( sqmPathType instanceof MappingModelExpressible<?> ) {
				return (MappingModelExpressible<?>) sqmPathType;
			}
			final NavigablePath navigablePath = sqmPath.getNavigablePath();
			if ( navigablePath.getParent() != null ) {
				final TableGroup parentTableGroup = tableGroupLocator.apply( navigablePath.getParent() );
				return parentTableGroup.getModelPart().findSubPart( navigablePath.getLocalName(), null );
			}
			return tableGroupLocator.apply( navigablePath.getParent() ).getModelPart();
		}

		if ( sqmExpressible instanceof BasicType<?> ) {
			return (BasicType<?>) sqmExpressible;
		}

		if ( sqmExpressible instanceof BasicDomainType<?> ) {
			return getTypeConfiguration().getBasicTypeForJavaType( sqmExpressible.getRelationalJavaType().getJavaType() );
		}

		if ( sqmExpressible instanceof BasicSqmPathSource<?> ) {
			return resolveMappingExpressible( sqmExpressible.getSqmType(), tableGroupLocator );
		}

		if ( sqmExpressible instanceof SqmFieldLiteral ) {
			return getTypeConfiguration().getBasicTypeForJavaType( ( (SqmFieldLiteral<?>) sqmExpressible).getJavaType() );
		}

		if ( sqmExpressible instanceof CompositeSqmPathSource ) {
			throw new UnsupportedOperationException( "Resolution of embedded-valued SqmExpressible nodes not yet implemented" );
		}

		if ( sqmExpressible instanceof AnonymousTupleSqmPathSource<?> ) {
			return resolveMappingExpressible(
					( (AnonymousTupleSqmPathSource<?>) sqmExpressible ).getSqmPathType(),
					tableGroupLocator
			);
		}

		if ( sqmExpressible instanceof EmbeddableTypeImpl ) {
			return (MappingModelExpressible<?>) sqmExpressible;
		}

		if ( sqmExpressible instanceof EntityDomainType<?> ) {
			return getEntityDescriptor( ( (EntityDomainType<?>) sqmExpressible).getHibernateEntityName() );
		}

		if ( sqmExpressible instanceof TupleType<?> ) {
			final MappingModelExpressible<?> mappingModelExpressible = tupleTypeCache.get(sqmExpressible);
			if ( mappingModelExpressible != null ) {
				return mappingModelExpressible;
			}
			final TupleType<?> tupleType = (TupleType<?>) sqmExpressible;
			final MappingModelExpressible<?>[] components = new MappingModelExpressible<?>[tupleType.componentCount()];
			for ( int i = 0; i < components.length; i++ ) {
				components[i] = resolveMappingExpressible( tupleType.get( i ), tableGroupLocator );
			}
			final MappingModelExpressible<?> createdMappingModelExpressible = new TupleMappingModelExpressible( components );
			final MappingModelExpressible<?> existingMappingModelExpressible = tupleTypeCache.putIfAbsent(
					tupleType,
					createdMappingModelExpressible
			);
			return existingMappingModelExpressible == null
					? createdMappingModelExpressible
					: existingMappingModelExpressible;
		}
		return null;
	}

	@Override
	public  <T> BindableType<T> resolveQueryParameterType(Class<T> javaClass) {
		final BasicType<T> basicType = getTypeConfiguration().getBasicTypeForJavaType( javaClass );
		// For enums, we simply don't know the exact mapping if there is no basic type registered
		if ( basicType != null || javaClass.isEnum() ) {
			return basicType;
		}

		final ManagedDomainType<T> managedType = jpaMetamodel.findManagedType( javaClass );
		if ( managedType != null ) {
			return managedType;
		}

		final JavaTypeRegistry javaTypeRegistry = getTypeConfiguration().getJavaTypeRegistry();
		final JavaType<T> javaType = javaTypeRegistry.findDescriptor( javaClass );
		if ( javaType != null ) {
			final JdbcType recommendedJdbcType =
					javaType.getRecommendedJdbcType( getTypeConfiguration().getCurrentBaseSqlTypeIndicators() );
			if ( recommendedJdbcType != null ) {
				return getTypeConfiguration().getBasicTypeRegistry().resolve( javaType, recommendedJdbcType );
			}
		}

		if ( javaClass.isArray() && javaTypeRegistry.findDescriptor( javaClass.getComponentType() ) != null ) {
			final JavaType<T> resolvedJavaType = javaTypeRegistry.resolveDescriptor( javaClass );
			final JdbcType recommendedJdbcType =
					resolvedJavaType.getRecommendedJdbcType( getTypeConfiguration().getCurrentBaseSqlTypeIndicators() );
			if ( recommendedJdbcType != null ) {
				return getTypeConfiguration().getBasicTypeRegistry().resolve( resolvedJavaType, recommendedJdbcType );
			}
		}

		return null;
	}
}
