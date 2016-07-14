/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.common.spi.IdentifiableTypeImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.internal.ImprovedEntityPersisterImpl;
import org.hibernate.persister.entity.spi.InheritanceType;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Temporary solution to inject building our "improved persister" impleentations as ORM
 * builds the older persisters.
 *
 * @author Steve Ebersole
 */
public class PersisterFactoryImpl implements PersisterFactory, ServiceRegistryAwareService {
	/**
	 * Singleton access
	 */
	public static final PersisterFactoryImpl INSTANCE = new PersisterFactoryImpl();

	private org.hibernate.persister.internal.PersisterFactoryImpl delegate;
	private ServiceRegistryImplementor serviceRegistry;

	private Set<TypeHierarchyNode> roots = new HashSet<TypeHierarchyNode>();
	private Map<String,TypeHierarchyNode> nameToHierarchyNodeMap = new HashMap<String, TypeHierarchyNode>();

	private Map<EntityPersister, ImprovedEntityPersisterImpl> entityPersisterMap = new HashMap<EntityPersister, ImprovedEntityPersisterImpl>();

	public Map<EntityPersister, ImprovedEntityPersisterImpl> getEntityPersisterMap() {
		return new HashMap<EntityPersister, ImprovedEntityPersisterImpl>( entityPersisterMap );
	}

	public void finishUp(DatabaseModel databaseModel, DomainMetamodelImpl domainMetamodel) {
		for ( TypeHierarchyNode root : roots ) {
			root.finishUp( null, databaseModel, domainMetamodel );
		}

		this.serviceRegistry = null;
		this.delegate = null;
		roots.clear();
		nameToHierarchyNodeMap.clear();
		entityPersisterMap.clear();
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.delegate = new org.hibernate.persister.internal.PersisterFactoryImpl();
		this.delegate.injectServices( serviceRegistry );
	}

	@Override
	public EntityPersister createEntityPersister(
			PersistentClass entityBinding,
			EntityRegionAccessStrategy entityCacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdCacheAccessStrategy,
			PersisterCreationContext creationContext) throws HibernateException {
		final EntityPersister legacyPersister = delegate.createEntityPersister(
				entityBinding,
				entityCacheAccessStrategy,
				naturalIdCacheAccessStrategy,
				creationContext
		);

		// See if we had an existing (partially created) node...
		TypeHierarchyNode typeHierarchyNode = nameToHierarchyNodeMap.get( entityBinding.getEntityName() );
		if ( typeHierarchyNode != null ) {
			assert typeHierarchyNode.type == null;
		}
		else {
			typeHierarchyNode = new TypeHierarchyNode( entityBinding.getEntityName(), entityBinding );
		}

		final TypeHierarchyNode superTypeNode = interpretSuperTypeNode( entityBinding );
		final InheritanceType inheritanceType = interpretInheritanceType( entityBinding );

		ImprovedEntityPersisterImpl improvedPersister = new ImprovedEntityPersisterImpl( legacyPersister );
		typeHierarchyNode.setType( improvedPersister );
		entityPersisterMap.put( legacyPersister, improvedPersister );
		nameToHierarchyNodeMap.put( legacyPersister.getEntityName(), typeHierarchyNode );

		if ( superTypeNode == null ) {
			roots.add( typeHierarchyNode );
		}
		else {
			superTypeNode.addSubType( typeHierarchyNode );
		}

		return legacyPersister;
	}

	private InheritanceType interpretInheritanceType(PersistentClass entityBinding) {
		if ( entityBinding instanceof RootClass ) {
			if ( !entityBinding.hasSubclasses() ) {
				return InheritanceType.NONE;
			}
			return interpret( (Subclass) entityBinding.getDirectSubclasses().next() );
		}
		else {
			return interpret( (Subclass) entityBinding );
		}
	}

	private InheritanceType interpret(Subclass subEntityBinding) {
		if ( subEntityBinding instanceof UnionSubclass ) {
			return InheritanceType.UNION;
		}
		else if ( subEntityBinding instanceof JoinedSubclass ) {
			return InheritanceType.JOINED;
		}
		else {
			return InheritanceType.DISCRIMINATOR;
		}
	}

	private TypeHierarchyNode interpretSuperTypeNode(PersistentClass entityBinding) {
		if ( entityBinding.getSuperMappedSuperclass() != null ) {
			// If entityBinding#getSuperMappedSuperclass() is not null, that is the direct super type
			return interpretMappedSuperclass( entityBinding.getSuperMappedSuperclass() );
		}
		else if ( entityBinding.getSuperclass() != null ) {
			// else, if entityBinding#getSuperclass() is not null, that is the direct super type
			// 		in this case we want to create the TypeHierarchyNode (if not already there), but not the persisters...
			// 		that will happen on later call to
			final String superTypeName = entityBinding.getSuperclass().getEntityName();
			TypeHierarchyNode node = nameToHierarchyNodeMap.get( superTypeName );
			if ( node == null ) {
				node = new TypeHierarchyNode( superTypeName, entityBinding.getSuperclass() );
				nameToHierarchyNodeMap.put( superTypeName, node );
			}
			return node;
		}
		else {
			// else, there is no super.
			return null;
		}
	}

	private TypeHierarchyNode interpretMappedSuperclass(MappedSuperclass mappedSuperclass) {
		if ( mappedSuperclass == null ) {
			return null;
		}

		assert mappedSuperclass.getMappedClass() != null;
		final TypeHierarchyNode existing = nameToHierarchyNodeMap.get( mappedSuperclass.getMappedClass().getName() );
		if ( existing != null ) {
			return existing;
		}

		return makeMappedSuperclassTypeNode( mappedSuperclass );
	}

	private TypeHierarchyNode makeMappedSuperclassTypeNode(MappedSuperclass mappedSuperclass) {
		assert mappedSuperclass.getMappedClass() != null;

		final TypeHierarchyNode mappedSuperclassTypeSuperNode = interpretMappedSuperclass( mappedSuperclass.getSuperMappedSuperclass() );
		nameToHierarchyNodeMap.put(
				mappedSuperclass.getMappedClass().getName(),
				mappedSuperclassTypeSuperNode
		);

		return mappedSuperclassTypeSuperNode;
	}

	@Override
	public CollectionPersister createCollectionPersister(
			Collection collectionBinding,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			PersisterCreationContext creationContext) throws HibernateException {
		return delegate.createCollectionPersister( collectionBinding, cacheAccessStrategy, creationContext );
	}

	public class TypeHierarchyNode {
		private final String name;
		private final Object typeSource;

		private IdentifiableTypeImplementor type;
		private Set<TypeHierarchyNode> subTypeNodes = new HashSet<TypeHierarchyNode>();

		public TypeHierarchyNode(String name, Object typeSource) {
			this.name = name;
			this.typeSource = typeSource;
		}

		public TypeHierarchyNode(String name, Object typeSource, IdentifiableTypeImplementor type) {
			this.name = name;
			this.typeSource = typeSource;
			this.type = type;
		}

		public void setType(IdentifiableTypeImplementor type) {
			if ( this.type != null ) {
				throw new IllegalStateException( "TypeHierarchyNode.type ws already defined" );
			}
			this.type = type;
		}

		public void addSubType(TypeHierarchyNode subTypeNode) {
			subTypeNodes.add( subTypeNode );
		}

		public void finishUp(IdentifiableTypeImplementor superType, DatabaseModel databaseModel, DomainMetamodelImpl domainMetamodel) {
			type.finishInitialization( superType, typeSource , databaseModel, domainMetamodel );
			for ( TypeHierarchyNode subTypeNode : subTypeNodes ) {
				subTypeNode.finishUp( type, databaseModel, domainMetamodel );
			}
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof TypeHierarchyNode ) ) {
				return false;
			}

			TypeHierarchyNode that = (TypeHierarchyNode) o;

			return name.equals( that.name );

		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}
}
