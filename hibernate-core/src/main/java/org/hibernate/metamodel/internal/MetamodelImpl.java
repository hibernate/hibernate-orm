/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MappedSuperclassType;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.HEMLogging;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;

/**
 * Hibernate implementation of the JPA {@link javax.persistence.metamodel.Metamodel} contract.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class MetamodelImpl implements MetamodelImplementor, Serializable {
	// todo : Integrate EntityManagerLogger into CoreMessageLogger
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( MetamodelImpl.class );
	private static final Object ENTITY_NAME_RESOLVER_MAP_VALUE = new Object();

	private final SessionFactoryImplementor sessionFactory;

	private final Map<String,String> imports;
	private final Map<String,EntityPersister> entityPersisterMap;
	private final Map<Class,String> entityProxyInterfaceMap;
	private final Map<String,CollectionPersister> collectionPersisterMap;
	private final Map<String,Set<String>> collectionRolesByEntityParticipant;
	private final ConcurrentMap<EntityNameResolver,Object> entityNameResolvers;


	private final Map<Class<?>, EntityTypeImpl<?>> entities;
	private final Map<Class<?>, EmbeddableTypeImpl<?>> embeddables;
	private final Map<Class<?>, MappedSuperclassType<?>> mappedSuperclassTypeMap;
	private final Map<String, EntityTypeImpl<?>> entityTypesByEntityName;

	/**
	 * Build the metamodel using the information from the collection of Hibernate
	 * {@link PersistentClass} models as well as the Hibernate {@link org.hibernate.SessionFactory}.
	 *
	 * @param mappingMetadata Access to the mapping metadata
	 * @param sessionFactory The Hibernate session factory.
	 * @param jpaMetaModelPopulationSetting ignore unsupported/unknown annotations (like @Any)
	 *
	 * @return The built metamodel
	 */
	public static MetamodelImpl buildMetamodel(
			MetadataImplementor mappingMetadata,
			SessionFactoryImplementor sessionFactory,
			JpaMetaModelPopulationSetting jpaMetaModelPopulationSetting) {
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

		final PersisterFactory persisterFactory = sessionFactory.getServiceRegistry().getService( PersisterFactory.class );

		final Map<String,EntityPersister> entityPersisterMap = CollectionHelper.concurrentMap( mappingMetadata.getEntityBindings().size() );
		final Map<Class,String> entityProxyInterfaceMap = CollectionHelper.concurrentMap( mappingMetadata.getEntityBindings().size() );
		for ( final PersistentClass model : mappingMetadata.getEntityBindings() ) {
			final EntityRegionAccessStrategy accessStrategy = sessionFactory.getCache().determineEntityRegionAccessStrategy(
					model
			);

			final NaturalIdRegionAccessStrategy naturalIdAccessStrategy = sessionFactory.getCache().determineNaturalIdRegionAccessStrategy(
					model
			);

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
					log.debugf( "Entity [%s] mapped same interface [%s] as class and proxy", cp.getEntityName(), cp.getMappedClass() );
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

		final Map<String,CollectionPersister> collectionPersisterMap = new HashMap<>();
		final Map<String,Set<String>> entityToCollectionRoleMap = new HashMap<>();

		for ( final Collection model : mappingMetadata.getCollectionBindings() ) {
			final CollectionRegionAccessStrategy accessStrategy = sessionFactory.getCache().determineCollectionRegionAccessStrategy(
					model
			);

			final CollectionPersister persister = persisterFactory.createCollectionPersister(
					model,
					accessStrategy,
					persisterCreationContext
			);
			collectionPersisterMap.put( model.getRole(), persister );
			Type indexType = persister.getIndexType();
			if ( indexType != null && indexType.isAssociationType() && !indexType.isAnyType() ) {
				String entityName = ( (AssociationType) indexType ).getAssociatedEntityName( sessionFactory );
				Set<String> roles = entityToCollectionRoleMap.get( entityName );
				if ( roles == null ) {
					roles = new HashSet<>();
					entityToCollectionRoleMap.put( entityName, roles );
				}
				roles.add( persister.getRole() );
			}
			Type elementType = persister.getElementType();
			if ( elementType.isAssociationType() && !elementType.isAnyType() ) {
				String entityName = ( ( AssociationType ) elementType ).getAssociatedEntityName( sessionFactory );
				Set<String> roles = entityToCollectionRoleMap.get( entityName );
				if ( roles == null ) {
					roles = new HashSet<>();
					entityToCollectionRoleMap.put( entityName, roles );
				}
				roles.add( persister.getRole() );
			}
		}

		ConcurrentHashMap<EntityNameResolver, Object> entityNameResolvers = new ConcurrentHashMap<>();

		// afterQuery *all* persisters and named queries are registered
		entityPersisterMap.values().forEach( EntityPersister::generateEntityDefinition );

		for ( EntityPersister persister : entityPersisterMap.values() ) {
			persister.postInstantiate();
			registerEntityNameResolvers( persister, entityNameResolvers );
		}
		collectionPersisterMap.values().forEach( CollectionPersister::postInstantiate );

		MetadataContext context = new MetadataContext(
				sessionFactory,
				mappingMetadata.getMappedSuperclassMappingsCopy(),
				jpaMetaModelPopulationSetting
		);
		if ( jpaMetaModelPopulationSetting != JpaMetaModelPopulationSetting.DISABLED ) {
			for ( PersistentClass entityBinding : mappingMetadata.getEntityBindings() ) {
				locateOrBuildEntityType( entityBinding, context );
			}
			handleUnusedMappedSuperclasses( context );
		}
		context.wrapUp();

		return new MetamodelImpl(
				sessionFactory,
				mappingMetadata.getImports(),
				entityPersisterMap,
				entityProxyInterfaceMap,
				entityNameResolvers,
				collectionPersisterMap,
				entityToCollectionRoleMap,
				context.getEntityTypeMap(),
				context.getEmbeddableTypeMap(),
				context.getMappedSuperclassTypeMap(),
				context.getEntityTypesByEntityName()
		);
	}



	public java.util.Collection<EntityNameResolver> getEntityNameResolvers() {
		return entityNameResolvers.keySet();
	}

	private static void registerEntityNameResolvers(EntityPersister persister, Map<EntityNameResolver,Object> entityNameResolvers) {
		if ( persister.getEntityMetamodel() == null || persister.getEntityMetamodel().getTuplizer() == null ) {
			return;
		}
		registerEntityNameResolvers( persister.getEntityMetamodel().getTuplizer(), entityNameResolvers );
	}

	private static void registerEntityNameResolvers(EntityTuplizer tuplizer, Map<EntityNameResolver,Object> entityNameResolvers) {
		EntityNameResolver[] resolvers = tuplizer.getEntityNameResolvers();
		if ( resolvers == null ) {
			return;
		}

		for ( EntityNameResolver resolver : resolvers ) {
			entityNameResolvers.put( resolver, ENTITY_NAME_RESOLVER_MAP_VALUE );
		}
	}

	private static void handleUnusedMappedSuperclasses(MetadataContext context) {
		final Set<MappedSuperclass> unusedMappedSuperclasses = context.getUnusedMappedSuperclasses();
		if ( !unusedMappedSuperclasses.isEmpty() ) {
			for ( MappedSuperclass mappedSuperclass : unusedMappedSuperclasses ) {
				log.unusedMappedSuperclass( mappedSuperclass.getMappedClass().getName() );
				locateOrBuildMappedsuperclassType( mappedSuperclass, context );
			}
		}
	}

	private static EntityTypeImpl<?> locateOrBuildEntityType(PersistentClass persistentClass, MetadataContext context) {
		EntityTypeImpl<?> entityType = context.locateEntityType( persistentClass );
		if ( entityType == null ) {
			entityType = buildEntityType( persistentClass, context );
		}
		return entityType;
	}

	//TODO remove / reduce @SW scope
	@SuppressWarnings("unchecked")
	private static EntityTypeImpl<?> buildEntityType(PersistentClass persistentClass, MetadataContext context) {
		final Class javaType = persistentClass.getMappedClass();
		context.pushEntityWorkedOn( persistentClass );
		final MappedSuperclass superMappedSuperclass = persistentClass.getSuperMappedSuperclass();
		AbstractIdentifiableType<?> superType = superMappedSuperclass == null
				? null
				: locateOrBuildMappedsuperclassType( superMappedSuperclass, context );
		//no mappedSuperclass, check for a super entity
		if ( superType == null ) {
			final PersistentClass superPersistentClass = persistentClass.getSuperclass();
			superType = superPersistentClass == null
					? null
					: locateOrBuildEntityType( superPersistentClass, context );
		}
		EntityTypeImpl entityType = new EntityTypeImpl(
				javaType,
				superType,
				persistentClass
		);

		context.registerEntityType( persistentClass, entityType );
		context.popEntityWorkedOn( persistentClass );
		return entityType;
	}

	private static MappedSuperclassTypeImpl<?> locateOrBuildMappedsuperclassType(
			MappedSuperclass mappedSuperclass, MetadataContext context) {
		MappedSuperclassTypeImpl<?> mappedSuperclassType = context.locateMappedSuperclassType( mappedSuperclass );
		if ( mappedSuperclassType == null ) {
			mappedSuperclassType = buildMappedSuperclassType( mappedSuperclass, context );
		}
		return mappedSuperclassType;
	}

	//TODO remove / reduce @SW scope
	@SuppressWarnings("unchecked")
	private static MappedSuperclassTypeImpl<?> buildMappedSuperclassType(
			MappedSuperclass mappedSuperclass,
			MetadataContext context) {
		final MappedSuperclass superMappedSuperclass = mappedSuperclass.getSuperMappedSuperclass();
		AbstractIdentifiableType<?> superType = superMappedSuperclass == null
				? null
				: locateOrBuildMappedsuperclassType( superMappedSuperclass, context );
		//no mappedSuperclass, check for a super entity
		if ( superType == null ) {
			final PersistentClass superPersistentClass = mappedSuperclass.getSuperPersistentClass();
			superType = superPersistentClass == null
					? null
					: locateOrBuildEntityType( superPersistentClass, context );
		}
		final Class javaType = mappedSuperclass.getMappedClass();
		MappedSuperclassTypeImpl mappedSuperclassType = new MappedSuperclassTypeImpl(
				javaType,
				mappedSuperclass,
				superType
		);
		context.registerMappedSuperclassType( mappedSuperclass, mappedSuperclassType );
		return mappedSuperclassType;
	}

	/**
	 * Instantiate the metamodel.
	 *
	 * @param entityNameResolvers
	 * @param entities The entity mappings.
	 * @param embeddables The embeddable (component) mappings.
	 * @param mappedSuperclassTypeMap The {@link javax.persistence.MappedSuperclass} mappings
	 */
	private MetamodelImpl(
			SessionFactoryImplementor sessionFactory,
			Map<String, String> imports,
			Map<String, EntityPersister> entityPersisterMap,
			Map<Class, String> entityProxyInterfaceMap,
			ConcurrentHashMap<EntityNameResolver, Object> entityNameResolvers,
			Map<String, CollectionPersister> collectionPersisterMap,
			Map<String, Set<String>> collectionRolesByEntityParticipant,
			Map<Class<?>, EntityTypeImpl<?>> entities,
			Map<Class<?>, EmbeddableTypeImpl<?>> embeddables,
			Map<Class<?>, MappedSuperclassType<?>> mappedSuperclassTypeMap,
			Map<String, EntityTypeImpl<?>> entityTypesByEntityName) {
		this.sessionFactory = sessionFactory;
		this.imports = imports;
		this.entityPersisterMap = entityPersisterMap;
		this.entityProxyInterfaceMap = entityProxyInterfaceMap;
		this.entityNameResolvers = entityNameResolvers;
		this.collectionPersisterMap = collectionPersisterMap;
		this.collectionRolesByEntityParticipant = collectionRolesByEntityParticipant;
		this.entities = entities;
		this.embeddables = embeddables;
		this.mappedSuperclassTypeMap = mappedSuperclassTypeMap;
		this.entityTypesByEntityName = entityTypesByEntityName;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X> EntityType<X> entity(Class<X> cls) {
		final EntityType<?> entityType = entities.get( cls );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "Not an entity: " + cls );
		}
		return (EntityType<X>) entityType;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X> ManagedType<X> managedType(Class<X> cls) {
		ManagedType<?> type = entities.get( cls );
		if ( type == null ) {
			type = mappedSuperclassTypeMap.get( cls );
		}
		if ( type == null ) {
			type = embeddables.get( cls );
		}
		if ( type == null ) {
			throw new IllegalArgumentException( "Not a managed type: " + cls );
		}
		return (ManagedType<X>) type;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X> EmbeddableType<X> embeddable(Class<X> cls) {
		final EmbeddableType<?> embeddableType = embeddables.get( cls );
		if ( embeddableType == null ) {
			throw new IllegalArgumentException( "Not an embeddable: " + cls );
		}
		return (EmbeddableType<X>) embeddableType;
	}

	@Override
	public Set<ManagedType<?>> getManagedTypes() {
		final int setSize = CollectionHelper.determineProperSizing(
				entities.size() + mappedSuperclassTypeMap.size() + embeddables.size()
		);
		final Set<ManagedType<?>> managedTypes = new HashSet<ManagedType<?>>( setSize );
		managedTypes.addAll( entities.values() );
		managedTypes.addAll( mappedSuperclassTypeMap.values() );
		managedTypes.addAll( embeddables.values() );
		return managedTypes;
	}

	@Override
	public Set<EntityType<?>> getEntities() {
		return new HashSet<>( entityTypesByEntityName.values() );
	}

	@Override
	public Set<EmbeddableType<?>> getEmbeddables() {
		return new HashSet<>( embeddables.values() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> EntityType<X> entity(String entityName) {
		return (EntityType<X>) entityTypesByEntityName.get( entityName );
	}

	@Override
	public String getImportedClassName(String className) {
		String result = imports.get( className );
		if ( result == null ) {
			try {
				sessionFactory.getServiceRegistry().getService( ClassLoaderService.class ).classForName( className );
				imports.put( className, className );
				return className;
			}
			catch ( ClassLoadingException cnfe ) {
				return null;
			}
		}
		else {
			return result;
		}
	}

	/**
	 * Given the name of an entity class, determine all the class and interface names by which it can be
	 * referenced in an HQL query.
	 *
	 * @param className The name of the entity class
	 *
	 * @return the names of all persistent (mapped) classes that extend or implement the
	 *     given class or interface, accounting for implicit/explicit polymorphism settings
	 *     and excluding mapped subclasses/joined-subclasses of other classes in the result.
	 * @throws MappingException
	 */
	public String[] getImplementors(String className) throws MappingException {

		final Class clazz;
		try {
			clazz = getSessionFactory().getServiceRegistry().getService( ClassLoaderService.class ).classForName( className );
		}
		catch (ClassLoadingException e) {
			return new String[] { className }; //for a dynamic-class
		}

		ArrayList<String> results = new ArrayList<>();
		for ( EntityPersister checkPersister : entityPersisters().values() ) {
			if ( ! Queryable.class.isInstance( checkPersister ) ) {
				continue;
			}
			final Queryable checkQueryable = Queryable.class.cast( checkPersister );
			final String checkQueryableEntityName = checkQueryable.getEntityName();
			final boolean isMappedClass = className.equals( checkQueryableEntityName );
			if ( checkQueryable.isExplicitPolymorphism() ) {
				if ( isMappedClass ) {
					return new String[] { className }; //NOTE EARLY EXIT
				}
			}
			else {
				if ( isMappedClass ) {
					results.add( checkQueryableEntityName );
				}
				else {
					final Class mappedClass = checkQueryable.getMappedClass();
					if ( mappedClass != null && clazz.isAssignableFrom( mappedClass ) ) {
						final boolean assignableSuperclass;
						if ( checkQueryable.isInherited() ) {
							Class mappedSuperclass = entityPersister( checkQueryable.getMappedSuperclass() ).getMappedClass();
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
	public EntityPersister locateEntityPersister(Class byClass) {
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
	public EntityPersister locateEntityPersister(String byName) {
		final EntityPersister entityPersister = entityPersisterMap.get( byName );
		if ( entityPersister == null ) {
			throw new UnknownEntityTypeException( "Unable to locate persister: " + byName );
		}
		return entityPersister;
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
		return ArrayHelper.toStringArray( entityPersisterMap.keySet() );
	}

	@Override
	public void close() {
		// anything to do ?
	}
}
