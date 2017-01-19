/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.persister.embedded.spi.EmbeddedContainer;
import org.hibernate.persister.embedded.spi.EmbeddedPersister;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.entity.spi.InheritanceStrategy;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * The standard ORM implementation of the {@link PersisterFactory} contract
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class PersisterFactoryImpl implements PersisterFactory, ServiceRegistryAwareService {
	private ServiceRegistryImplementor serviceRegistry;

	private PersisterClassResolver persisterClassResolver;

	private Set<EntityHierarchyNode> roots = new HashSet<>();
	private Map<String,EntityHierarchyNode> nameToHierarchyNodeMap = new HashMap<>();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// IMPL NOTE
	//
	// 	As a PersisterFactory implementation this class is responsible for
	//	generated both EntityPersister and CollectionPersister.
	//
	// todo : add creation of EmbeddablePersisters to PersisterFactory contract?
	//
	// The general flow for persister creation is as follows:
	//		1) #createPersister is called directly from the SessionFactory's
	//			Metamodel object during initialization.  It creates the
	//			EntityPersister instance and begins categorizing them in
	// 			relation to their hierarchy (nameToHierarchyNodeMap, roots) for
	// 			later processing
	//		2) After #createPersister has been called for all defined entities,
	//			#finishUp will be called.  This is the trigger to walk all the
	//			previously created persisters *in a specific order*: starting
	// 			from #roots we walk down each hierarchy meaning that as we
	//			perform EntityPersister#finishInitialization processing we know
	//			that the super is completely initialized.  Part of this
	//			EntityPersister#finishInitialization process is creating the
	//			entity's Attribute definitions.  As PluralAttribute definitions
	//			are recognized we create CollectionPersisters
	//
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		persisterClassResolver = serviceRegistry.getService( PersisterClassResolver.class );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public EntityPersister createEntityPersister(
			PersistentClass entityBinding,
			EntityRegionAccessStrategy entityCacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdCacheAccessStrategy,
			PersisterCreationContext creationContext) throws HibernateException {

		// todo : MappedSuperclass...

		// See if we had an existing (partially created) node...
		EntityHierarchyNode entityHierarchyNode = nameToHierarchyNodeMap.get( entityBinding.getEntityName() );
		if ( entityHierarchyNode != null ) {
			// we create the EntityHierarchyNode for all super types before we
			//		actually create the super's EntityPersister.  This check
			//		makes sure we do not have multiple call paths attempting to
			//		create the same EntityPersister multiple times
			assert entityHierarchyNode.getEntityPersister() == null;
		}
		else {
			entityHierarchyNode = new EntityHierarchyNode( entityBinding );
			nameToHierarchyNodeMap.put( entityBinding.getEntityName(), entityHierarchyNode );
		}

		final EntityPersister entityPersister = instantiateEntityPersister(
				entityBinding,
				entityCacheAccessStrategy,
				naturalIdCacheAccessStrategy,
				creationContext
		);

		entityHierarchyNode.setEntityPersister( entityPersister );

		final EntityHierarchyNode superTypeNode = interpretSuperTypeNode( entityBinding );

		if ( superTypeNode == null ) {
			roots.add( entityHierarchyNode );
		}
		else {
			superTypeNode.addSubEntityNode( entityHierarchyNode );
		}

		return entityPersister;
	}

	@SuppressWarnings("unchecked")
	private EntityPersister instantiateEntityPersister(
			PersistentClass entityBinding,
			EntityRegionAccessStrategy entityCacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdCacheAccessStrategy,
			PersisterCreationContext creationContext) {
		// If the metadata for the entity specified an explicit persister class, use it...
		Class<? extends EntityPersister> persisterClass = entityBinding.getEntityPersisterClass();
		if ( persisterClass == null ) {
			// Otherwise, use the persister class indicated by the PersisterClassResolver service
			persisterClass = persisterClassResolver.getEntityPersisterClass( entityBinding );
		}

		return instantiateEntityPersister(
				persisterClass,
				entityBinding,
				entityCacheAccessStrategy,
				naturalIdCacheAccessStrategy,
				creationContext
		);
	}

	@SuppressWarnings( {"unchecked"})
	private EntityPersister instantiateEntityPersister(
			Class<? extends EntityPersister> persisterClass,
			PersistentClass entityBinding,
			EntityRegionAccessStrategy entityCacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdCacheAccessStrategy,
			PersisterCreationContext creationContext) {
		try {
			final Constructor<? extends EntityPersister> constructor = persisterClass.getConstructor( EntityPersister.STANDARD_CONSTRUCTOR_SIG );
			try {
				return constructor.newInstance(
						entityBinding,
						entityCacheAccessStrategy,
						naturalIdCacheAccessStrategy,
						creationContext
				);
			}
			catch (MappingException e) {
				throw e;
			}
			catch (InvocationTargetException e) {
				Throwable target = e.getTargetException();
				if ( target instanceof HibernateException ) {
					throw (HibernateException) target;
				}
				else {
					throw new MappingException( "Could not instantiate persister " + persisterClass.getName(), target );
				}
			}
			catch (Exception e) {
				throw new MappingException( "Could not instantiate persister " + persisterClass.getName(), e );
			}
		}
		catch (MappingException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MappingException( "Could not get constructor for " + persisterClass.getName(), e );
		}
	}

	private InheritanceStrategy interpretInheritanceStrategy(PersistentClass entityBinding) {
		if ( entityBinding instanceof RootClass ) {
			if ( !entityBinding.hasSubclasses() ) {
				return InheritanceStrategy.NONE;
			}
			return interpretInheritanceStrategy( (Subclass) entityBinding.getDirectSubclasses().next() );
		}
		else {
			return interpretInheritanceStrategy( (Subclass) entityBinding );
		}
	}

	private InheritanceStrategy interpretInheritanceStrategy(Subclass subEntityBinding) {
		if ( subEntityBinding instanceof UnionSubclass ) {
			return InheritanceStrategy.UNION;
		}
		else if ( subEntityBinding instanceof JoinedSubclass ) {
			return InheritanceStrategy.JOINED;
		}
		else {
			return InheritanceStrategy.DISCRIMINATOR;
		}
	}

	private EntityHierarchyNode interpretSuperTypeNode(PersistentClass entityBinding) {
		if ( entityBinding.getSuperMappedSuperclass() != null ) {
			// If entityBinding#getSuperMappedSuperclass() is not null, that is the direct super type
			return interpretMappedSuperclass( entityBinding.getSuperMappedSuperclass() );
		}
		else if ( entityBinding.getSuperclass() != null ) {
			// else, if entityBinding#getSuperclass() is not null, that is the direct super type
			// 		in this case we want to create the TypeHierarchyNode (if not already there), but not the persisters...
			// 		that will happen on later call to
			final String superTypeName = entityBinding.getSuperclass().getEntityName();
			EntityHierarchyNode node = nameToHierarchyNodeMap.computeIfAbsent(
					superTypeName,
					k -> new EntityHierarchyNode( entityBinding.getSuperclass() )
			);
			return node;
		}
		else {
			// else, there is no super.
			return null;
		}
	}

	private EntityHierarchyNode interpretMappedSuperclass(MappedSuperclass mappedSuperclass) {
		if ( mappedSuperclass == null ) {
			return null;
		}

		assert mappedSuperclass.getMappedClass() != null;
		final EntityHierarchyNode existing = nameToHierarchyNodeMap.get( mappedSuperclass.getMappedClass().getName() );
		if ( existing != null ) {
			return existing;
		}

		return makeMappedSuperclassTypeNode( mappedSuperclass );
	}

	private EntityHierarchyNode makeMappedSuperclassTypeNode(MappedSuperclass mappedSuperclass) {
		assert mappedSuperclass.getMappedClass() != null;

		final EntityHierarchyNode mappedSuperclassTypeSuperNode = interpretMappedSuperclass( mappedSuperclass.getSuperMappedSuperclass() );
		nameToHierarchyNodeMap.put(
				mappedSuperclass.getMappedClass().getName(),
				mappedSuperclassTypeSuperNode
		);

		return mappedSuperclassTypeSuperNode;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public CollectionPersister createCollectionPersister(
			Collection collectionBinding,
			ManagedTypeImplementor source,
			String localName,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			PersisterCreationContext creationContext) throws HibernateException {
		// If the metadata for the collection specified an explicit persister class, use it
		Class<? extends CollectionPersister> persisterClass = collectionBinding.getCollectionPersisterClass();
		if ( persisterClass == null ) {
			// Otherwise, use the persister class indicated by the PersisterClassResolver service
			persisterClass = persisterClassResolver.getCollectionPersisterClass( collectionBinding );
		}
		return createCollectionPersister( persisterClass, collectionBinding, source, localName, cacheAccessStrategy, creationContext );
	}

	@SuppressWarnings( {"unchecked"})
	private CollectionPersister createCollectionPersister(
			Class<? extends CollectionPersister> persisterClass,
			Collection collectionBinding,
			ManagedTypeImplementor source,
			String localName,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			PersisterCreationContext creationContext) {
		try {
			Constructor<? extends CollectionPersister> constructor = persisterClass.getConstructor( CollectionPersister.CONSTRUCTOR_SIGNATURE );
			try {
				return constructor.newInstance(
						collectionBinding,
						source,
						localName,
						cacheAccessStrategy,
						creationContext
				);
			}
			catch (MappingException e) {
				throw e;
			}
			catch (InvocationTargetException e) {
				Throwable target = e.getTargetException();
				if ( target instanceof HibernateException ) {
					throw (HibernateException) target;
				}
				else {
					throw new MappingException( "Could not instantiate collection persister " + persisterClass.getName(), target );
				}
			}
			catch (Exception e) {
				throw new MappingException( "Could not instantiate collection persister " + persisterClass.getName(), e );
			}
		}
		catch (MappingException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MappingException( "Could not get constructor for " + persisterClass.getName(), e );
		}
	}

	@Override
	public EmbeddedPersister createEmbeddablePersister(
			Component componentBinding,
			EmbeddedContainer source,
			String localName,
			PersisterCreationContext creationContext) {
		final Class<? extends EmbeddedPersister> persisterClass = persisterClassResolver.getEmbeddablePersisterClass( componentBinding );

		try {
			Constructor<? extends EmbeddedPersister> constructor = persisterClass.getConstructor( EmbeddedPersister.STANDARD_CTOR_SIGNATURE );
			try {
				return constructor.newInstance(
						componentBinding,
						source,
						localName,
						creationContext
				);
			}
			catch (MappingException e) {
				throw e;
			}
			catch (InvocationTargetException e) {
				Throwable target = e.getTargetException();
				if ( target instanceof HibernateException ) {
					throw (HibernateException) target;
				}
				else {
					throw new MappingException( "Could not instantiate collection persister " + persisterClass.getName(), target );
				}
			}
			catch (Exception e) {
				throw new MappingException( "Could not instantiate collection persister " + persisterClass.getName(), e );
			}
		}
		catch (MappingException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MappingException( "Could not get constructor for " + persisterClass.getName(), e );
		}
	}

	@Override
	public void finishUp(PersisterCreationContext creationContext) {
		for ( EntityHierarchyNode root : roots ) {
			root.finishUp( null, creationContext );
		}

		this.serviceRegistry = null;
		this.roots.clear();
		this.nameToHierarchyNodeMap.clear();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Inner classes

	public static class EntityHierarchyNode {
		private final PersistentClass entityBinding;

		private EntityPersister entityPersister;
		private Set<EntityHierarchyNode> subEntityNodes;

		public EntityHierarchyNode(PersistentClass entityBinding) {
			this.entityBinding = entityBinding;
		}

		public PersistentClass getEntityBinding() {
			return entityBinding;
		}

		public EntityPersister getEntityPersister() {
			return entityPersister;
		}

		public void setEntityPersister(EntityPersister entityPersister) {
			if ( this.entityPersister != null ) {
				throw new IllegalStateException( "TypeHierarchyNode.entityPersister ws already defined" );
			}
			this.entityPersister = entityPersister;
		}

		public void addSubEntityNode(EntityHierarchyNode subEntityNode) {
			if ( subEntityNodes == null ) {
				subEntityNodes = new HashSet<>();
			}
			subEntityNodes.add( subEntityNode );
		}

		public void finishUp(EntityPersister superType, PersisterCreationContext creationContext) {
			if ( getEntityPersister() == null ) {
				throw new HibernateException( "EntityPersister not yet known; cannot finishUp" );
			}

			// initialize the EntityPersister represented by this hierarchy node
			getEntityPersister().finishInitialization( superType, entityBinding , creationContext );
			getEntityPersister().postInstantiate();

			if ( subEntityNodes != null ) {
				// pass finishUp processing to each of the sub-entity hierarchy nodes (recursive)
				for ( EntityHierarchyNode subTypeNode : subEntityNodes ) {
					subTypeNode.finishUp( getEntityPersister(), creationContext );
				}
			}
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof EntityHierarchyNode ) ) {
				return false;
			}

			EntityHierarchyNode that = (EntityHierarchyNode) o;

			return entityBinding.getEntityName().equals( that.entityBinding.getEntityName() );

		}

		@Override
		public int hashCode() {
			return entityBinding.getEntityName().hashCode();
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations

	/**
	 * @deprecated Use {@link PersisterFactoryImpl#ENTITY_PERSISTER_CONSTRUCTOR_ARGS} instead.
	 */
	@Deprecated
	public static final Class[] ENTITY_PERSISTER_CONSTRUCTOR_ARGS = EntityPersister.STANDARD_CONSTRUCTOR_SIG;

	/**
	 * @deprecated Use {@link CollectionPersister#CONSTRUCTOR_SIGNATURE} instead
	 */
	@Deprecated
	public static final Class[] COLLECTION_PERSISTER_CONSTRUCTOR_ARGS = CollectionPersister.CONSTRUCTOR_SIGNATURE;
}
