/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.EntityNameResolver;
import org.hibernate.graph.RootGraph;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Steve Ebersole
 */
public interface RuntimeModel {
	void visitEntityHierarchies(Consumer<EntityHierarchy> action);

	<T> EntityTypeDescriptor<T> getEntityDescriptor(NavigableRole name) throws NotNavigableException;
	<T> EntityTypeDescriptor<T> getEntityDescriptor(Class<T> javaType) throws NotNavigableException;
	<T> EntityTypeDescriptor<T> getEntityDescriptor(String name) throws NotNavigableException;
	<T> EntityTypeDescriptor<T> findEntityDescriptor(Class<T> javaType);
	<T> EntityTypeDescriptor<T> findEntityDescriptor(String name);
	void visitEntityDescriptors(Consumer<EntityTypeDescriptor<?>> action);

	<T> MappedSuperclassTypeDescriptor<T> getMappedSuperclassDescriptor(NavigableRole name) throws NotNavigableException;
	<T> MappedSuperclassTypeDescriptor<T> getMappedSuperclassDescriptor(Class<T> javaType) throws NotNavigableException;
	<T> MappedSuperclassTypeDescriptor<T> getMappedSuperclassDescriptor(String name) throws NotNavigableException;
	<T> MappedSuperclassTypeDescriptor<T> findMappedSuperclassDescriptor(Class<T> javaType);
	<T> MappedSuperclassTypeDescriptor<T> findMappedSuperclassDescriptor(String name);
	void visitMappedSuperclassDescriptors(Consumer<MappedSuperclassTypeDescriptor<?>> action);

	<T> EmbeddedTypeDescriptor<T> findEmbeddedDescriptor(Class<T> javaType);
	<T> EmbeddedTypeDescriptor<T> findEmbeddedDescriptor(NavigableRole name);
	<T> EmbeddedTypeDescriptor<T> findEmbeddedDescriptor(String name);
	void visitEmbeddedDescriptors(Consumer<EmbeddedTypeDescriptor<?>> action);

	<O,C,E> PersistentCollectionDescriptor<O,C,E> getCollectionDescriptor(NavigableRole name) throws NotNavigableException;
	<O,C,E> PersistentCollectionDescriptor<O,C,E> getCollectionDescriptor(String name) throws NotNavigableException;
	<O,C,E> PersistentCollectionDescriptor<O,C,E> findCollectionDescriptor(NavigableRole name);
	<O,C,E> PersistentCollectionDescriptor<O,C,E> findCollectionDescriptor(String name);
	void visitCollectionDescriptors(Consumer<PersistentCollectionDescriptor<?,?,?>> action);

	<T> RootGraph<? super T> findRootGraph(String name);
	<T> List<RootGraph<? super T>> findRootGraphsForType(Class<T> baseType);
	<T> List<RootGraph<? super T>> findRootGraphsForType(String baseTypeName);
	void visitRootGraphs(Consumer<RootGraph<?>> action);

	// todo (6.0) : default-for-type as well?
	//		aka:
	//<T> EntityGraphImplementor<T> defaultGraph(Class<T> entityJavaType);

	String getImportedName(String name);

	Set<EntityNameResolver> getEntityNameResolvers();
	void visitEntityNameResolvers(Consumer<EntityNameResolver> action);
}
