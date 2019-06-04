/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.HEMLogging;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.MappedSuperclassDomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.internal.EntityTypeImpl;
import org.hibernate.metamodel.model.domain.internal.MappedSuperclassTypeImpl;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class InflightRuntimeMetamodel {
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( InflightRuntimeMetamodel.class );

	private final TypeConfiguration typeConfiguration;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hibernate metamodel

	private final Map<String, EntityPersister> entityPersisterMap = new ConcurrentHashMap<>();
	private final Map<Class, String> entityProxyInterfaceMap = new ConcurrentHashMap<>();
	private final Map<String, CollectionPersister> collectionPersisterMap = new ConcurrentHashMap<>();
	private final Map<String, Set<String>> collectionRolesByEntityParticipant = new ConcurrentHashMap<>();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA metamodel

	private final Map<String, EntityDomainType<?>> jpaEntityTypeMap = new ConcurrentHashMap<>();
	private final Map<Class<?>, MappedSuperclassDomainType<?>> jpaMappedSuperclassTypeMap = new ConcurrentHashMap<>();
	private Map<Class, EmbeddableDomainType<?>> jpaEmbeddableDescriptorMap = new ConcurrentHashMap<>();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Misc

	private final Map<String, String> nameToImportNameMap = new HashMap<>();
	private final Set<EntityNameResolver> entityNameResolvers = new CopyOnWriteArraySet<>();
	private final Map<String, String> imports = new ConcurrentHashMap<>(  );

	public InflightRuntimeMetamodel(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	public void processBootMetaModel(
			MetadataImplementor bootMetamodel,
			SqmCriteriaNodeBuilder criteriaBuilder,
			CacheImplementor cacheImplementor,
			PersisterFactory persisterFactory,
			PersisterCreationContext persisterCreationContext,
			JpaMetaModelPopulationSetting jpaMetaModelPopulationSetting,
			JpaStaticMetaModelPopulationSetting jpaStaticMetaModelPopulationSetting) {
		this.imports.putAll( bootMetamodel.getImports() );
		processBootEntities(
				bootMetamodel.getEntityBindings(),
				cacheImplementor,
				persisterFactory,
				persisterCreationContext
		);

		processBootCollections(
				bootMetamodel.getCollectionBindings(),
				cacheImplementor,
				persisterFactory,
				persisterCreationContext
		);

		finishDomainMetamodelInitialization();

		processJpa(
				bootMetamodel,
				criteriaBuilder,
				jpaMetaModelPopulationSetting,
				jpaStaticMetaModelPopulationSetting
		);

	}

	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	public Map<String, EntityPersister> getEntityPersisterMap() {
		return entityPersisterMap;
	}

	public Map<Class, String> getEntityProxyInterfaceMap() {
		return entityProxyInterfaceMap;
	}

	public Map<String, CollectionPersister> getCollectionPersisterMap() {
		return collectionPersisterMap;
	}

	public Map<String, Set<String>> getCollectionRolesByEntityParticipant() {
		return collectionRolesByEntityParticipant;
	}

	public Map<String, EntityDomainType<?>> getJpaEntityTypeMap() {
		return jpaEntityTypeMap;
	}

	public Map<Class<?>, MappedSuperclassDomainType<?>> getJpaMappedSuperclassTypeMap() {
		return jpaMappedSuperclassTypeMap;
	}

	public Map<Class, EmbeddableDomainType<?>> getJpaEmbeddableDescriptorMap() {
		return jpaEmbeddableDescriptorMap;
	}

	public Map<String, String> getNameToImportNameMap() {
		return nameToImportNameMap;
	}

	public Set<EntityNameResolver> getEntityNameResolvers() {
		return entityNameResolvers;
	}

	/**
	 * Get an entity mapping descriptor based on its Hibernate entity-name
	 *
	 * @throws IllegalArgumentException if the name does not refer to an entity
	 */
	public EntityPersister getEntityDescriptor(String entityName) {
		final EntityPersister entityPersister = entityPersisterMap.get( entityName );
		if ( entityPersister == null ) {
			throw new IllegalArgumentException( "Unable to locate persister: " + entityName );
		}
		return entityPersister;
	}

	/**
	 * Find an entity mapping descriptor based on its Hibernate entity-name.
	 *
	 * @apiNote Returns {@code null} rather than throwing exception
	 */
	public EntityPersister findEntityDescriptor(String entityName) {
		return entityPersisterMap.get( entityName );
	}

	private void processJpa(
			MetadataImplementor bootMetamodel,
			SqmCriteriaNodeBuilder criteriaBuilder,
			JpaMetaModelPopulationSetting jpaMetaModelPopulationSetting,
			JpaStaticMetaModelPopulationSetting jpaStaticMetaModelPopulationSetting) {
		if ( jpaMetaModelPopulationSetting != JpaMetaModelPopulationSetting.DISABLED ) {

			MetadataContext context = new MetadataContext(
					this,
					criteriaBuilder,
					bootMetamodel.getMappedSuperclassMappingsCopy(),
					typeConfiguration,
					jpaMetaModelPopulationSetting,
					jpaStaticMetaModelPopulationSetting
			);

			for ( PersistentClass entityBinding : bootMetamodel.getEntityBindings() ) {
				locateOrBuildEntityType( entityBinding, context, typeConfiguration );
			}
			handleUnusedMappedSuperclasses( context, typeConfiguration );

			context.wrapUp();

			this.nameToImportNameMap.putAll( bootMetamodel.getImports() );

			this.jpaEntityTypeMap.putAll( context.getEntityTypesByEntityName() );
			this.jpaMappedSuperclassTypeMap.putAll( context.getMappedSuperclassTypeMap() );

			for ( EmbeddableDomainType<?> embeddable : context.getEmbeddableTypeSet() ) {
				this.jpaEmbeddableDescriptorMap.put( embeddable.getJavaType(), embeddable );
			}

		}
	}

	private void processBootEntities(
			java.util.Collection<PersistentClass> entityBindings,
			CacheImplementor cacheImplementor,
			PersisterFactory persisterFactory,
			PersisterCreationContext persisterCreationContext) {
		for ( final PersistentClass model : entityBindings ) {
			final NavigableRole rootEntityRole = new NavigableRole( model.getRootClass().getEntityName() );
			final EntityDataAccess accessStrategy = cacheImplementor.getEntityRegionAccess( rootEntityRole );
			final NaturalIdDataAccess naturalIdAccessStrategy = cacheImplementor
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
	}

	private void processBootCollections(
			java.util.Collection<Collection> collectionBindings,
			CacheImplementor cacheImplementor,
			PersisterFactory persisterFactory,
			PersisterCreationContext persisterCreationContext) {
		for ( final Collection model : collectionBindings ) {
			final NavigableRole navigableRole = new NavigableRole( model.getRole() );

			final CollectionDataAccess accessStrategy = cacheImplementor.getCollectionRegionAccess(
					navigableRole );

			final CollectionPersister persister = persisterFactory.createCollectionPersister(
					model,
					accessStrategy,
					persisterCreationContext
			);
			collectionPersisterMap.put( model.getRole(), persister );
			Type indexType = persister.getIndexType();
			if ( indexType != null && indexType.isEntityType() && !indexType.isAnyType() ) {
				String entityName = ( (EntityType) indexType ).getAssociatedEntityName();
				Set<String> roles = collectionRolesByEntityParticipant.get( entityName );
				if ( roles == null ) {
					roles = new HashSet<>();
					collectionRolesByEntityParticipant.put( entityName, roles );
				}
				roles.add( persister.getRole() );
			}
			Type elementType = persister.getElementType();
			if ( elementType.isEntityType() && !elementType.isAnyType() ) {
				String entityName = ( (EntityType) elementType ).getAssociatedEntityName();
				Set<String> roles = collectionRolesByEntityParticipant.get( entityName );
				if ( roles == null ) {
					roles = new HashSet<>();
					collectionRolesByEntityParticipant.put( entityName, roles );
				}
				roles.add( persister.getRole() );
			}
		}
	}

	private void finishDomainMetamodelInitialization() {
		// after *all* persisters and named queries are registered
		entityPersisterMap.values().forEach( EntityPersister::generateEntityDefinition );

		for ( EntityPersister persister : entityPersisterMap.values() ) {
			persister.postInstantiate();
			registerEntityNameResolvers( persister, entityNameResolvers );
		}
		collectionPersisterMap.values().forEach( CollectionPersister::postInstantiate );
	}

	private static void registerEntityNameResolvers(
			EntityPersister persister,
			Set<EntityNameResolver> entityNameResolvers) {
		if ( persister.getEntityMetamodel() == null || persister.getEntityMetamodel().getTuplizer() == null ) {
			return;
		}
		registerEntityNameResolvers( persister.getEntityMetamodel().getTuplizer(), entityNameResolvers );
	}

	private static void registerEntityNameResolvers(
			EntityTuplizer tuplizer,
			Set<EntityNameResolver> entityNameResolvers) {
		EntityNameResolver[] resolvers = tuplizer.getEntityNameResolvers();
		if ( resolvers == null ) {
			return;
		}

		for ( EntityNameResolver resolver : resolvers ) {
			entityNameResolvers.add( resolver );
		}
	}

	private static void handleUnusedMappedSuperclasses(MetadataContext context, TypeConfiguration typeConfiguration) {
		final Set<MappedSuperclass> unusedMappedSuperclasses = context.getUnusedMappedSuperclasses();
		if ( !unusedMappedSuperclasses.isEmpty() ) {
			for ( MappedSuperclass mappedSuperclass : unusedMappedSuperclasses ) {
				log.unusedMappedSuperclass( mappedSuperclass.getMappedClass().getName() );
				locateOrBuildMappedSuperclassType( mappedSuperclass, context, typeConfiguration );
			}
		}
	}

	private static MappedSuperclassDomainType<?> locateOrBuildMappedSuperclassType(
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
	private static MappedSuperclassTypeImpl<?> buildMappedSuperclassType(
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
		final JavaTypeDescriptor javaTypeDescriptor = context.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( mappedSuperclass.getMappedClass() );
		MappedSuperclassTypeImpl mappedSuperclassType = new MappedSuperclassTypeImpl(
				javaTypeDescriptor,
				mappedSuperclass,
				superType,
				typeConfiguration
		);

		context.registerMappedSuperclassType( mappedSuperclass, mappedSuperclassType );
		return mappedSuperclassType;
	}

	private static EntityDomainType<?> locateOrBuildEntityType(
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
	private static EntityTypeImpl<?> buildEntityType(
			PersistentClass persistentClass,
			MetadataContext context,
			TypeConfiguration typeConfiguration) {
		final Class javaType = persistentClass.getMappedClass();
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

		final JavaTypeDescriptor javaTypeDescriptor = context.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( javaType );
		EntityTypeImpl entityType = new EntityTypeImpl(
				javaTypeDescriptor,
				superType,
				persistentClass,
				typeConfiguration
		);
		context.registerEntityType( persistentClass, entityType );
		context.popEntityWorkedOn( persistentClass );
		return entityType;
	}

	public Map<String, String> getImports() {
		return imports;
	}
}
