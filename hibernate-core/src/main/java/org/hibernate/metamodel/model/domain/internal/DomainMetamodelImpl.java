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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.persistence.EntityGraph;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.EntityNameResolver;
import org.hibernate.MappingException;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.HEMLogging;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.internal.InflightRuntimeMetamodel;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.spi.DomainMetamodel;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;
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

	private final JpaMetamodel jpaMetamodel;

	private final Map<Class, String> entityProxyInterfaceMap;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// RuntimeModel

	private final Map<String, EntityPersister> entityPersisterMap;
	private final Map<String, CollectionPersister> collectionPersisterMap;
	private final Map<String, Set<String>> collectionRolesByEntityParticipant;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainMetamodel

	private final Set<EntityNameResolver> entityNameResolvers;


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

	public DomainMetamodelImpl(
			SessionFactoryImplementor sessionFactory,
			InflightRuntimeMetamodel runtimeMetamodel,
			JpaMetamodel jpaMetamodel) {
		this.sessionFactory = sessionFactory;
		this.jpaMetamodel = jpaMetamodel;
		this.typeConfiguration = runtimeMetamodel.getTypeConfiguration();
		this.entityPersisterMap = runtimeMetamodel.getEntityPersisterMap();
		this.collectionPersisterMap = runtimeMetamodel.getCollectionPersisterMap();
		this.collectionRolesByEntityParticipant = runtimeMetamodel.getCollectionRolesByEntityParticipant();
		this.entityNameResolvers = runtimeMetamodel.getEntityNameResolvers();
		this.entityProxyInterfaceMap = runtimeMetamodel.getEntityProxyInterfaceMap();
	}

	@Override
	public java.util.Collection<EntityNameResolver> getEntityNameResolvers() {
		return entityNameResolvers;
	}


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
		return findEntityDescriptor( entity.getClass() );
	}

	@Override
	public void visitEntityDescriptors(Consumer<EntityPersister> action) {
		entityPersisterMap.values().forEach( action );
	}

	@Override
	public EntityPersister getEntityDescriptor(String entityName) {
		final EntityPersister entityPersister = entityPersisterMap.get( entityName );
		if ( entityPersister == null ) {
			throw new IllegalArgumentException( "Unable to locate persister: " + entityName );
		}
		return entityPersister;
	}

	@Override
	public EntityPersister findEntityDescriptor(String entityName) {
		return entityPersisterMap.get( entityName );
	}

	@Override
	public EntityPersister findEntityDescriptor(Class entityJavaType) {
		return findEntityDescriptor( entityJavaType.getName() );
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public EntityPersister getEntityDescriptor(Class entityJavaType) {
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
	public EntityPersister locateEntityDescriptor(Class byClass) {
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
	public void visitCollectionDescriptors(Consumer<CollectionPersister> action) {
		collectionPersisterMap.values().forEach( action );
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
	public CollectionPersister findCollectionDescriptor(String role) {
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
	public RootGraph<?> findNamedGraph(String name) {
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
