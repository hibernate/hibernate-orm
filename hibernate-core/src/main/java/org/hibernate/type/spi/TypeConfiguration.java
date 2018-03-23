/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.internal.MetamodelImpl;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.AssociationType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.hibernate.type.TypeResolver;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * Defines a set of available Type instances as isolated from other configurations.  The
 * isolation is defined by each instance of a TypeConfiguration.
 * <p/>
 * Note that each Type is inherently "scoped" to a TypeConfiguration.  We only ever access
 * a Type through a TypeConfiguration - specifically the TypeConfiguration in effect for
 * the current persistence unit.
 * <p/>
 * Even though each Type instance is scoped to a TypeConfiguration, Types do not inherently
 * have access to that TypeConfiguration (mainly because Type is an extension contract - meaning
 * that Hibernate does not manage the full set of Types available in ever TypeConfiguration).
 * However Types will often want access to the TypeConfiguration, which can be achieved by the
 * Type simply implementing the {@link TypeConfigurationAware} interface.
 *
 * @author Steve Ebersole
 * @since 5.3
 */
@Incubating
public class TypeConfiguration implements SessionFactoryObserver, Serializable {
	private static final CoreMessageLogger log = messageLogger( TypeConfiguration.class );

	private final Scope scope;
	private final TypeFactory typeFactory;

	private final BasicTypeRegistry basicTypeRegistry;

	private final Map<String, String> imports = new ConcurrentHashMap<>();
	private final Map<String, EntityPersister> entityPersisters = new ConcurrentHashMap<>();
	private final Map<String,CollectionPersister> collectionPersisters = new ConcurrentHashMap<>();
	private final Map<String,Set<String>> collectionRolesByEntityParticipant = new ConcurrentHashMap<>();

	private final Map<Class, String> entityProxyInterfaces = new ConcurrentHashMap<>();

	private final Set<EntityNameResolver> entityNameResolvers = new HashSet<>();

	// temporarily needed to support deprecations
	private final TypeResolver typeResolver;

	public TypeConfiguration() {
		this.scope = new Scope();
		basicTypeRegistry = new BasicTypeRegistry();
		typeFactory = new TypeFactory( this );
		typeResolver = new TypeResolver( this, typeFactory );
	}

	public void initialize(MetadataImplementor mappingMetadata){
		final PersisterCreationContext persisterCreationContext = new PersisterCreationContext() {
			@Override
			public SessionFactoryImplementor getSessionFactory() {
				return scope.getSessionFactory();
			}

			@Override
			public MetadataImplementor getMetadata() {
				return mappingMetadata;
			}
		};

		final PersisterFactory persisterFactory = getSessionFactory().getServiceRegistry().getService( PersisterFactory.class );

		for ( final PersistentClass model : mappingMetadata.getEntityBindings() ) {
			final NavigableRole rootEntityRole = new NavigableRole( model.getRootClass().getEntityName() );
			final EntityDataAccess accessStrategy = getSessionFactory().getCache().getEntityRegionAccess( rootEntityRole );
			final NaturalIdDataAccess naturalIdAccessStrategy = getSessionFactory().getCache()
					.getNaturalIdCacheRegionAccessStrategy( rootEntityRole );

			final EntityPersister cp = persisterFactory.createEntityPersister(
					model,
					accessStrategy,
					naturalIdAccessStrategy,
					persisterCreationContext
			);
			register( cp );
		}

		for ( final org.hibernate.mapping.Collection model : mappingMetadata.getCollectionBindings() ) {
			final NavigableRole navigableRole = new NavigableRole( model.getRole() );

			final CollectionDataAccess accessStrategy = getSessionFactory()
					.getCache().getCollectionRegionAccess( navigableRole );

			final CollectionPersister persister = persisterFactory.createCollectionPersister(
					model,
					accessStrategy,
					persisterCreationContext
			);
			registerCollectionPersister( persister );
		}

		// after *all* persisters and named queries are registered
		java.util.Collection<EntityPersister> entityPersisters = getEntityPersisters();
		entityPersisters.forEach( EntityPersister::generateEntityDefinition );

		for ( EntityPersister persister : entityPersisters ) {
			persister.postInstantiate();
			registerEntityNameResolvers( persister );
		}
		getCollectionPersisters().forEach( CollectionPersister::postInstantiate );
	}

	/**
	 * Temporarily needed to support deprecations
	 * <p>
	 * Retrieve the {@link Type} resolver associated with this factory.
	 *
	 * @return The type resolver
	 *
	 * @deprecated (since 5.3) No replacement, access to and handling of Types will be much different in 6.0
	 */
	@Deprecated
	public TypeResolver getTypeResolver() {
		return typeResolver;
	}

	public BasicTypeRegistry getBasicTypeRegistry() {
		return basicTypeRegistry;
	}

	public Map<String, String> getImports() {
		return Collections.unmodifiableMap( imports );
	}

	public Set<EntityNameResolver> getEntityNameResolvers() {
		return entityNameResolvers;
	}

	/**
	 * Locate an EntityPersister by the entity class.  The passed Class might refer to either
	 * the entity name directly, or it might name a proxy interface for the entity.  This
	 * method accounts for both, preferring the direct named entity name.
	 *
	 * @param javaType The concrete Class or proxy interface for the entity to locate the persister for.
	 *
	 * @return The located EntityPersister, never {@code null}
	 *
	 * @throws org.hibernate.UnknownEntityTypeException If a matching EntityPersister cannot be located
	*/
	public EntityPersister resolveEntityPersister(Class javaType) {
		EntityPersister entityPersister = findEntityPersister( javaType );

		if ( entityPersister == null ) {
			throw new UnknownEntityTypeException( "Unable to locate persister: " + javaType.getName() );
		}

		return entityPersister;
	}

	/**
	 * Locate an EntityPersister by the entity class.  The passed Class might refer to either
	 * the entity name directly, or it might name a proxy interface for the entity.  This
	 * method accounts for both, preferring the direct named entity name.
	 *
	 * @param javaType The concrete Class or proxy interface for the entity to locate the persister for.
	 *
	 * @return The located EntityPersister or {@code null} in no persister or proxy is found
	 */
	public EntityPersister findEntityPersister(Class javaType) {
		EntityPersister entityPersister = entityPersisters.get( javaType.getName() );
		if ( entityPersister == null ) {
			String mappedEntityName = entityProxyInterfaces.get( javaType );
			if ( mappedEntityName != null ) {
				entityPersister = entityPersisters.get( mappedEntityName );
			}
		}
		return entityPersister;
	}

	/**
	 * Locate the persister for an entity by the entity-name
	 *
	 * @param entityName The name of the entity for which to retrieve the persister.
	 *
	 * @return The persister
	 *
	 * @throws MappingException Indicates persister could not be found with that name.
	 */
	public EntityPersister entityPersister(String entityName) throws MappingException {
		EntityPersister result = findEntityPersister( entityName );
		if ( result == null ) {
			throw new MappingException( "Unknown entity: " + entityName );
		}
		return result;
	}

	/**
	 * Locate the entity persister by name.
	 *
	 * @param entityName The entity name
	 *
	 * @return The located EntityPersister, never {@code null}
	 *
	 * @throws org.hibernate.UnknownEntityTypeException If a matching EntityPersister cannot be located
	 */
	public EntityPersister resolveEntityPersister(String entityName) {
		final EntityPersister entityPersister = findEntityPersister( entityName );
		if ( entityPersister == null ) {
			throw new UnknownEntityTypeException( "Unable to locate persister: " + entityName );
		}
		return entityPersister;
	}

	/**
	 * Locate the entity persister by name.
	 *
	 * @param entityName The entity name
	 *
	 * @return The located EntityPersister or {@code null} in no persister is found
	 */
	public EntityPersister findEntityPersister(String entityName) {
		return entityPersisters.get( entityName );
	}

	/**
	 * Retrieve all EntityPersisters
	 */
	public Collection<EntityPersister> getEntityPersisters() {
		return entityPersisters.values();
	}

	/**
	 * Get all entity persisters as a Map, which entity name its the key and the persister is the value.
	 *
	 * @return The Map contains all entity persisters.
	 */
	public Map<String, EntityPersister> entityPersisters() {
		return entityPersisters;
	}

	/**
	 * Retrieves a set of all the collection roles in which the given entity is a participant, as either an
	 * index or an element.
	 *
	 * @param entityName The entity name for which to get the collection roles.
	 *
	 * @return set of all the collection roles in which the given entityName participates.
	 */
	public Set<String> getCollectionRolesByEntityParticipant(String entityName) {
		return collectionRolesByEntityParticipant.get( entityName );
	}

	/**
	 * Get the persister object for a collection role.
	 *
	 * @param role The role of the collection for which to retrieve the persister.
	 *
	 * @return The persister
	 *
	 * @throws MappingException Indicates persister could not be found with that role.
	 */
	public CollectionPersister collectionPersister(String role) {
		final CollectionPersister persister = collectionPersisters.get( role );
		if ( persister == null ) {
			throw new MappingException( "Could not locate CollectionPersister for role : " + role );
		}
		return persister;
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

	/**
	 * Get all collection persisters as a Map, which collection role as the key and the persister is the value.
	 *
	 * @return The Map contains all collection persisters.
	 */
	public Map<String,CollectionPersister> collectionPersisters(){
		return collectionPersisters;
	}

	public Collection<CollectionPersister> getCollectionPersisters(){
		return collectionPersisters.values();
	}

	private void registerEntityNameResolvers(EntityPersister persister) {
		if ( persister.getEntityMetamodel() != null && persister.getEntityMetamodel().getTuplizer() != null ) {
			registerEntityNameResolvers( persister.getEntityMetamodel().getTuplizer() );
		}
	}

	private void register(EntityPersister entityPersister) {
		entityPersisters.put( entityPersister.getEntityName(), entityPersister );
		if ( entityPersister.getConcreteProxyClass() != null
				&& entityPersister.getConcreteProxyClass().isInterface()
				&& !Map.class.isAssignableFrom( entityPersister.getConcreteProxyClass() )
				&& entityPersister.getMappedClass() != entityPersister.getConcreteProxyClass() ) {
			// IMPL NOTE : we exclude Map based proxy interfaces here because that should
			//		indicate MAP entity mode.0

			if ( entityPersister.getMappedClass().equals( entityPersister.getConcreteProxyClass() ) ) {
				// this part handles an odd case in the Hibernate test suite where we map an interface
				// as the class and the proxy.  I cannot think of a real life use case for that
				// specific test, but..
				log.debugf(
						"Entity [%s] mapped same interface [%s] as class and proxy",
						entityPersister.getEntityName(),
						entityPersister.getMappedClass()
				);
			}
			else {
				final String old = entityProxyInterfaces.put(
						entityPersister.getConcreteProxyClass(),
						entityPersister.getEntityName()
				);
				if ( old != null ) {
					throw new HibernateException(
							String.format(
									Locale.ENGLISH,
									"Multiple entities [%s, %s] named the same interface [%s] as their proxy which is not supported",
									old,
									entityPersister.getEntityName(),
									entityPersister.getConcreteProxyClass().getName()
							)
					);
				}
			}
		}
	}

	private void registerCollectionPersister(CollectionPersister persister) {
		collectionPersisters.put( persister.getRole(), persister );
		Type indexType = persister.getIndexType();
		if ( indexType != null && indexType.isAssociationType() && !indexType.isAnyType() ) {
			String entityName = ( (AssociationType) indexType ).getAssociatedEntityName( scope.getSessionFactory() );
			Set<String> roles = collectionRolesByEntityParticipant.get( entityName );
			if ( roles == null ) {
				roles = new HashSet<>();
				collectionRolesByEntityParticipant.put( entityName, roles );
			}
			roles.add( persister.getRole() );
		}
		Type elementType = persister.getElementType();
		if ( elementType.isAssociationType() && !elementType.isAnyType() ) {
			String entityName = ( (AssociationType) elementType ).getAssociatedEntityName( scope.getSessionFactory() );
			Set<String> roles = collectionRolesByEntityParticipant.get( entityName );
			if ( roles == null ) {
				roles = new HashSet<>();
				collectionRolesByEntityParticipant.put( entityName, roles );
			}
			roles.add( persister.getRole() );
		}
	}

	private void registerEntityNameResolvers(EntityTuplizer tuplizer) {
		EntityNameResolver[] resolvers = tuplizer.getEntityNameResolvers();
		if ( resolvers != null ) {
			for ( EntityNameResolver resolver : resolvers ) {
				entityNameResolvers.add( resolver );
			}
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Scoping

	/**
	 * Obtain the MetadataBuildingContext currently scoping the
	 * TypeConfiguration.
	 *
	 * @return
	 *
	 * @apiNote This will throw an exception if the SessionFactory is not yet
	 * bound here.  See {@link Scope} for more details regarding the stages
	 * a TypeConfiguration goes through
	 */
	public MetadataBuildingContext getMetadataBuildingContext() {
		return scope.getMetadataBuildingContext();
	}

	public void scope(MetadataBuildingContext metadataBuildingContext) {
		log.debugf( "Scoping TypeConfiguration [%s] to MetadataBuildingContext [%s]", this, metadataBuildingContext );
		scope.setMetadataBuildingContext( metadataBuildingContext );
	}

	public MetamodelImplementor scope(SessionFactoryImplementor sessionFactory, BootstrapContext bootstrapContext) {
		log.debugf( "Scoping TypeConfiguration [%s] to SessionFactoryImpl [%s]", this, sessionFactory );
		scope.setSessionFactory( sessionFactory );
		typeFactory.injectSessionFactory( sessionFactory );
		log.debugf( "Scoping TypeConfiguration [%s] to SessionFactory [%s]", this, sessionFactory );

		for ( Map.Entry<String, String> importEntry : scope.getMetadataBuildingContext()
				.getMetadataCollector()
				.getImports()
				.entrySet() ) {
			if ( imports.containsKey( importEntry.getKey() ) ) {
				continue;
			}

			imports.put( importEntry.getKey(), importEntry.getValue() );
		}

		scope.setSessionFactory( sessionFactory );
		sessionFactory.addObserver( this );

		return new MetamodelImpl( sessionFactory, this );
	}

	/**
	 * Obtain the SessionFactory currently scoping the TypeConfiguration.
	 *
	 * @return The SessionFactory
	 *
	 * @throws IllegalStateException if the TypeConfiguration is currently not
	 * associated with a SessionFactory (in "runtime stage").
	 * @apiNote This will throw an exception if the SessionFactory is not yet
	 * bound here.  See {@link Scope} for more details regarding the stages
	 * a TypeConfiguration goes through (this is "runtime stage")
	 */
	public SessionFactoryImplementor getSessionFactory() {
		return scope.getSessionFactory();
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		// Instead of allowing scope#setSessionFactory to influence this, we use the SessionFactoryObserver callback
		// to handle this, allowing any SessionFactory constructor code to be able to continue to have access to the
		// MetadataBuildingContext through TypeConfiguration until this callback is fired.
		log.tracef( "Handling #sessionFactoryCreated from [%s] for TypeConfiguration", factory );
		scope.setMetadataBuildingContext( null );
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
		log.tracef( "Handling #sessionFactoryClosed from [%s] for TypeConfiguration", factory );
		scope.unsetSessionFactory( factory );

		imports.clear();
		entityPersisters.clear();
		collectionPersisters.clear();
		collectionRolesByEntityParticipant.clear();
		entityProxyInterfaces.clear();
		entityNameResolvers.clear();
	}
}
