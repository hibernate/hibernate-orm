/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.hibernate.EntityNameResolver;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.NotNavigableException;
import org.hibernate.metamodel.RuntimeModel;
import org.hibernate.metamodel.model.creation.spi.InFlightRuntimeModel;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractRuntimeModel implements RuntimeModel {
	private final Set<EntityHierarchy> entityHierarchySet;
	private final Map<String, EntityTypeDescriptor<?>> entityDescriptorMap;
	private final Map<String, MappedSuperclassTypeDescriptor<?>> mappedSuperclassDescriptorMap;
	private final Map<String,EmbeddedTypeDescriptor<?>> embeddedDescriptorMap;
	private final Map<String,PersistentCollectionDescriptor<?,?,?>> collectionDescriptorMap;

	private final Map<String,String> nameImportMap;
	private final Set<EntityNameResolver> entityNameResolvers;

	private final Map<String, RootGraphImplementor<?>> entityGraphMap;

	public AbstractRuntimeModel() {
		this.entityDescriptorMap = new ConcurrentHashMap<>();
		this.entityHierarchySet = ConcurrentHashMap.newKeySet();
		this.mappedSuperclassDescriptorMap = new ConcurrentHashMap<>();
		this.embeddedDescriptorMap = new ConcurrentHashMap<>();
		this.collectionDescriptorMap = new ConcurrentHashMap<>();
		this.nameImportMap = new ConcurrentHashMap<>();
		this.entityNameResolvers = ConcurrentHashMap.newKeySet();
		this.entityGraphMap = new ConcurrentHashMap<>();
	}

	public AbstractRuntimeModel(InFlightRuntimeModel inFlightModel) {
		this(
				inFlightModel.getEntityHierarchySet(),
				inFlightModel.getEntityDescriptorMap(),
				inFlightModel.getMappedSuperclassDescriptorMap(),
				inFlightModel.getEmbeddedDescriptorMap(),
				inFlightModel.getCollectionDescriptorMap(),
				inFlightModel.getEntityNameResolvers(),
				inFlightModel.getNameImportMap(),
				inFlightModel.getRootGraphMap()
		);
	}

	private AbstractRuntimeModel(
			Set<EntityHierarchy> entityHierarchySet,
			Map<String, EntityTypeDescriptor<?>> entityDescriptorMap,
			Map<String, MappedSuperclassTypeDescriptor<?>> mappedSuperclassDescriptorMap,
			Map<String, EmbeddedTypeDescriptor<?>> embeddedDescriptorMap,
			Map<String, PersistentCollectionDescriptor<?, ?, ?>> collectionDescriptorMap,
			Set<EntityNameResolver> entityNameResolvers,
			Map<String, String> nameImportMap,
			Map<String, RootGraphImplementor<?>> entityGraphMap) {
		this.entityHierarchySet = Collections.unmodifiableSet( entityHierarchySet );
		this.entityDescriptorMap = Collections.unmodifiableMap( entityDescriptorMap );
		this.mappedSuperclassDescriptorMap = Collections.unmodifiableMap( mappedSuperclassDescriptorMap );
		this.embeddedDescriptorMap = Collections.unmodifiableMap( embeddedDescriptorMap );
		this.collectionDescriptorMap = Collections.unmodifiableMap( collectionDescriptorMap );
		this.entityNameResolvers = Collections.unmodifiableSet( entityNameResolvers );

		// NOTE : EntityGraph map is mutable during runtime
		this.entityGraphMap = new ConcurrentHashMap<>( entityGraphMap );

		// NOTE : currently this needs to be mutable, but can''t these really all be known up front?
		this.nameImportMap = new ConcurrentHashMap<>( nameImportMap );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityHierarchy

	protected Set<EntityHierarchy> getEntityHierarchySet() {
		return entityHierarchySet;
	}

	@Override
	public void visitEntityHierarchies(Consumer<EntityHierarchy> action) {
		entityHierarchySet.forEach( action );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityDescriptor

	protected Map<String, EntityTypeDescriptor<?>> getEntityDescriptorMap() {
		return entityDescriptorMap;
	}

	@Override
	public <T> EntityTypeDescriptor<T> getEntityDescriptor(Class<T> javaType) {
		return getEntityDescriptor( javaType.getName() );
	}

	@Override
	public <T> EntityTypeDescriptor<T> getEntityDescriptor(NavigableRole name) {
		return getEntityDescriptor( name.getFullPath() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> EntityTypeDescriptor<T> getEntityDescriptor(String entityName) throws NotNavigableException {
		final EntityTypeDescriptor<T> descriptor = (EntityTypeDescriptor<T>) entityDescriptorMap.get( entityName );

		if ( descriptor == null ) {
			throw new NotNavigableException( entityName );
		}

		return descriptor;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> EntityTypeDescriptor<T> findEntityDescriptor(Class<T> javaType) {
		return (EntityTypeDescriptor<T>) entityDescriptorMap.get( javaType.getName() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> EntityTypeDescriptor<T> findEntityDescriptor(String entityName) {
		entityName = getImportedName( entityName );
		return (EntityTypeDescriptor<T>) entityDescriptorMap.get( entityName );
	}

	@Override
	public void visitEntityDescriptors(Consumer<EntityTypeDescriptor<?>> action) {
		entityDescriptorMap.values().forEach( action );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// MappedSuperclassDescriptor


	protected Map<String, MappedSuperclassTypeDescriptor<?>> getMappedSuperclassDescriptorMap() {
		return mappedSuperclassDescriptorMap;
	}

	@Override
	public <T> MappedSuperclassTypeDescriptor<T> getMappedSuperclassDescriptor(NavigableRole role) throws NotNavigableException {
		return getMappedSuperclassDescriptor( role.getFullPath() );
	}

	@Override
	public <T> MappedSuperclassTypeDescriptor<T> getMappedSuperclassDescriptor(Class<T> javaType) throws NotNavigableException {
		return getMappedSuperclassDescriptor( javaType.getName() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> MappedSuperclassTypeDescriptor<T> getMappedSuperclassDescriptor(String name) throws NotNavigableException {
		final MappedSuperclassTypeDescriptor<T> descriptor = (MappedSuperclassTypeDescriptor<T>) mappedSuperclassDescriptorMap.get( name );

		if ( descriptor == null ) {
			throw new NotNavigableException( name );
		}

		return descriptor;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> MappedSuperclassTypeDescriptor<T> findMappedSuperclassDescriptor(Class<T> javaType) {
		return (MappedSuperclassTypeDescriptor<T>) mappedSuperclassDescriptorMap.get( javaType.getName() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> MappedSuperclassTypeDescriptor<T> findMappedSuperclassDescriptor(String name) {
		name = getImportedName( name );
		return (MappedSuperclassTypeDescriptor<T>) mappedSuperclassDescriptorMap.get( name );
	}

	@Override
	public void visitMappedSuperclassDescriptors(Consumer<MappedSuperclassTypeDescriptor<?>> action) {
		mappedSuperclassDescriptorMap.values().forEach( action );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddedTypeDescriptor


	protected Map<String, EmbeddedTypeDescriptor<?>> getEmbeddedDescriptorMap() {
		return embeddedDescriptorMap;
	}

	@Override
	public <T> EmbeddedTypeDescriptor<T> findEmbeddedDescriptor(Class<T> javaType) {
		return findEmbeddedDescriptor( javaType.getName() );
	}

	@Override
	public <T> EmbeddedTypeDescriptor<T> findEmbeddedDescriptor(NavigableRole name) {
		return findEmbeddedDescriptor( name.getFullPath() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> EmbeddedTypeDescriptor<T> findEmbeddedDescriptor(String name) {
		return (EmbeddedTypeDescriptor<T>) embeddedDescriptorMap.get( name );
	}

	@Override
	public void visitEmbeddedDescriptors(Consumer<EmbeddedTypeDescriptor<?>> action) {
		embeddedDescriptorMap.values().forEach( action );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// PersistentCollectionDescriptor

	protected Map<String, PersistentCollectionDescriptor<?, ?, ?>> getCollectionDescriptorMap() {
		return collectionDescriptorMap;
	}

	@Override
	public <O, C, E> PersistentCollectionDescriptor<O, C, E> getCollectionDescriptor(NavigableRole name) throws NotNavigableException {
		return getCollectionDescriptor( name.getFullPath() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <O, C, E> PersistentCollectionDescriptor<O, C, E> getCollectionDescriptor(String name) throws NotNavigableException {
		final PersistentCollectionDescriptor descriptor = findCollectionDescriptor( name );
		if ( descriptor == null ) {
			throw new NotNavigableException( name );
		}
		return descriptor;
	}

	@Override
	public <O,C,E> PersistentCollectionDescriptor<O,C,E> findCollectionDescriptor(NavigableRole name) {
		return findCollectionDescriptor( name.getFullPath() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <O,C,E> PersistentCollectionDescriptor<O,C,E> findCollectionDescriptor(String name) {
		return (PersistentCollectionDescriptor<O,C,E>) collectionDescriptorMap.get( name );
	}

	@Override
	public void visitCollectionDescriptors(Consumer<PersistentCollectionDescriptor<?,?,?>> action) {
		collectionDescriptorMap.values().forEach( action );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityGraph


	protected Map<String, RootGraphImplementor<?>> getRootGraphMap() {
		return entityGraphMap;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> RootGraphImplementor<? super T> findRootGraph(String name) {
		return (RootGraphImplementor<T>) entityGraphMap.get( name );
	}

	@Override
	public <T> List<RootGraph<? super T>> findRootGraphsForType(Class<T> baseType) {
		return findRootGraphsForType( baseType.getName() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<RootGraph<? super T>> findRootGraphsForType(String baseTypeName) {
		final EntityTypeDescriptor<? extends T> entityDescriptor = findEntityDescriptor( baseTypeName );
		if ( entityDescriptor == null ) {
			throw new IllegalArgumentException( "Not an entity : " + baseTypeName );
		}

		final List<RootGraph<? super T>> results = new ArrayList<>();

		for ( RootGraphImplementor rootGraph : entityGraphMap.values() ) {
			if ( rootGraph.appliesTo( entityDescriptor ) ) {
				results.add( rootGraph );
			}
		}

		return results;
	}

	@Override
	public void visitRootGraphs(Consumer<RootGraph<?>> action) {
		entityGraphMap.values().forEach( action );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityNameResolver

	@Override
	public Set<EntityNameResolver> getEntityNameResolvers() {
		return entityNameResolvers;
	}

	@Override
	public void visitEntityNameResolvers(Consumer<EntityNameResolver> action) {
		entityNameResolvers.forEach( action );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// name imports

	protected Map<String, String> getNameImportMap() {
		return nameImportMap;
	}

	@Override
	public String getImportedName(String name) {
		return nameImportMap.getOrDefault( name, name );
	}
}
