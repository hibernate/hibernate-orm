/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.MappedSuperclassMapping;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedContainer;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.InheritanceStrategy;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelNodeClassResolver;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelDescriptorFactory;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * The standard ORM implementation of the {@link RuntimeModelDescriptorFactory} contract
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class RuntimeModelDescriptorFactoryImpl
		implements RuntimeModelDescriptorFactory, ServiceRegistryAwareService {
	private ServiceRegistryImplementor serviceRegistry;

	private RuntimeModelNodeClassResolver persisterClassResolver;

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
	//			perform EntityPersister#finishInstantiation processing we know
	//			that the super is completely initialized.  Part of this
	//			EntityPersister#finishInstantiation process is creating the
	//			entity's Attribute definitions.  As PluralAttribute definitions
	//			are recognized we create CollectionPersisters
	//
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		persisterClassResolver = serviceRegistry.getService( RuntimeModelNodeClassResolver.class );
	}

	@Override
	public <J> EntityDescriptor<J> createEntityDescriptor(
			EntityMapping bootMapping,
			RuntimeModelCreationContext creationContext) {
		// todo : MappedSuperclass...

		// See if we had an existing (partially created) node...
		EntityHierarchyNode entityHierarchyNode = nameToHierarchyNodeMap.get( bootMapping.getEntityName() );
		if ( entityHierarchyNode != null ) {
			// we create the EntityHierarchyNode for all super types before we
			//		actually create the super's EntityPersister.  This check
			//		makes sure we do not have multiple call paths attempting to
			//		create the same EntityPersister multiple times
			assert entityHierarchyNode.getEntityPersister() == null;
		}
		else {
			entityHierarchyNode = new EntityHierarchyNode( bootMapping );
			nameToHierarchyNodeMap.put( bootMapping.getEntityName(), entityHierarchyNode );
		}

		final EntityDescriptor entityPersister = instantiateEntityPersister(
				bootMapping,
				creationContext
		);


		entityHierarchyNode.setEntityPersister( entityPersister );

		final EntityHierarchyNode superTypeNode = interpretSuperTypeNode( bootMapping );

		if ( superTypeNode == null ) {
			roots.add( entityHierarchyNode );
		}
		else {
			superTypeNode.addSubEntityNode( entityHierarchyNode );
		}

		return entityPersister;
	}

	@SuppressWarnings("unchecked")
	private EntityDescriptor instantiateEntityPersister(
			EntityMapping bootMapping,
			RuntimeModelCreationContext creationContext) {
		// If the metadata for the entity specified an explicit persister class, use it...
		Class<? extends EntityDescriptor> persisterClass = bootMapping.getEntityPersisterClass();
		if ( persisterClass == null ) {
			// Otherwise, use the persister class indicated by the PersisterClassResolver service
			persisterClass = persisterClassResolver.getEntityPersisterClass( bootMapping );
		}

		return instantiateEntityPersister(
				persisterClass,
				bootMapping,
				creationContext
		);
	}

	@SuppressWarnings( {"unchecked"})
	private EntityDescriptor instantiateEntityPersister(
			Class<? extends EntityDescriptor> persisterClass,
			EntityMapping bootMapping,
			RuntimeModelCreationContext creationContext) {
		try {
			final Constructor<? extends EntityDescriptor> constructor = persisterClass.getConstructor( EntityDescriptor.STANDARD_CONSTRUCTOR_SIG );
			try {
				return constructor.newInstance(
						bootMapping,
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

	private EntityHierarchyNode interpretSuperTypeNode(IdentifiableTypeMapping bootMapping) {
		if ( bootMapping.getSuperTypeMapping() == null ) {
			return null;
		}

		final String superTypeName = bootMapping.getSuperTypeMapping().getName();
		return nameToHierarchyNodeMap.computeIfAbsent(
				superTypeName,
				k -> new EntityHierarchyNode( bootMapping.getSuperTypeMapping() )
		);
	}

	private EntityHierarchyNode makeMappedSuperclassTypeNode(MappedSuperclassMapping mappedSuperclass) {

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
	public PersistentCollectionDescriptor createPersistentCollectionDescriptor(
			Collection collectionBinding,
			ManagedTypeDescriptor source,
			String localName,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			RuntimeModelCreationContext creationContext) throws HibernateException {
		// If the metadata for the collection specified an explicit persister class, use it
		Class<? extends PersistentCollectionDescriptor> persisterClass = collectionBinding.getCollectionPersisterClass();
		if ( persisterClass == null ) {
			// Otherwise, use the persister class indicated by the PersisterClassResolver service
			persisterClass = persisterClassResolver.getCollectionPersisterClass( collectionBinding );
		}
		return createCollectionPersister( persisterClass, collectionBinding, source, localName, cacheAccessStrategy, creationContext );
	}

	@SuppressWarnings( {"unchecked"})
	private PersistentCollectionDescriptor createCollectionPersister(
			Class<? extends PersistentCollectionDescriptor> persisterClass,
			Collection collectionBinding,
			ManagedTypeDescriptor source,
			String localName,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			RuntimeModelCreationContext creationContext) {
		try {
			Constructor<? extends PersistentCollectionDescriptor> constructor = persisterClass.getConstructor( PersistentCollectionDescriptor.CONSTRUCTOR_SIGNATURE );
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
	public EmbeddedTypeDescriptor createEmbeddedTypeDescriptor(
			EmbeddedValueMapping embeddedValueMapping,
			EmbeddedContainer source,
			String localName,
			RuntimeModelCreationContext creationContext) {
		final Class<? extends EmbeddedTypeDescriptor> persisterClass = persisterClassResolver.getEmbeddablePersisterClass( embeddedValueMapping );

		try {
			Constructor<? extends EmbeddedTypeDescriptor> constructor = persisterClass.getConstructor( EmbeddedTypeDescriptor.STANDARD_CTOR_SIGNATURE );
			try {
				return constructor.newInstance(
						embeddedValueMapping,
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
	public void finishUp(RuntimeModelCreationContext creationContext) {
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
		private final IdentifiableTypeMapping identifiableTypeMapping;

		private EntityDescriptor entityPersister;
		private Set<EntityHierarchyNode> subEntityNodes;

		public EntityHierarchyNode(IdentifiableTypeMapping identifiableTypeMapping) {
			this.identifiableTypeMapping = identifiableTypeMapping;
		}

		public IdentifiableTypeMapping getIdentifiableTypeMapping() {
			return identifiableTypeMapping;
		}

		public EntityDescriptor getEntityPersister() {
			return entityPersister;
		}

		public void setEntityPersister(EntityDescriptor entityPersister) {
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

		public void finishUp(IdentifiableTypeDescriptor superType, RuntimeModelCreationContext creationContext) {
			if ( getEntityPersister() == null ) {
				throw new HibernateException( "EntityPersister not yet known; cannot finishUp" );
			}

			// initialize the EntityPersister represented by this hierarchy node
			getEntityPersister().finishInitialization( superType, identifiableTypeMapping, creationContext );
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

			return identifiableTypeMapping.getEntityName().equals( that.identifiableTypeMapping.getEntityName() );

		}

		@Override
		public int hashCode() {
			return identifiableTypeMapping.getEntityName().hashCode();
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations

	/**
	 * @deprecated Use {@link RuntimeModelDescriptorFactoryImpl#ENTITY_PERSISTER_CONSTRUCTOR_ARGS} instead.
	 */
	@Deprecated
	public static final Class[] ENTITY_PERSISTER_CONSTRUCTOR_ARGS = EntityDescriptor.STANDARD_CONSTRUCTOR_SIG;

	/**
	 * @deprecated Use {@link PersistentCollectionDescriptor#CONSTRUCTOR_SIGNATURE} instead
	 */
	@Deprecated
	public static final Class[] COLLECTION_PERSISTER_CONSTRUCTOR_ARGS = PersistentCollectionDescriptor.CONSTRUCTOR_SIGNATURE;
}
