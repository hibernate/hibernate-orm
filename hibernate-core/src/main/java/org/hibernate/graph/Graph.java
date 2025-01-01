/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;

import java.util.List;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;

/**
 * A container for {@link AttributeNode} references.
 *
 * @apiNote Acts as an abstraction over the JPA-defined interfaces
 *          {@link jakarta.persistence.EntityGraph} and
 *          {@link jakarta.persistence.Subgraph}, which have no
 *          common supertype.
 *
 * @author Strong Liu
 * @author Steve Ebersole
 * @author Andrea Boriero
 *
 * @see RootGraph
 * @see SubGraph
 * @see jakarta.persistence.EntityGraph
 * @see jakarta.persistence.Subgraph
 */
public interface Graph<J> extends GraphNode<J>, jakarta.persistence.Graph<J> {

	/**
	 * Add a subgraph rooted at a plural attribute, allowing further
	 * nodes to be added to the subgraph.
	 *
	 * @apiNote This method is missing in JPA, and nodes cannot be
	 *          added in a typesafe way to subgraphs representing
	 *          fetched collections
	 *
	 * @since 6.3
	 */
	default <AJ> SubGraph<AJ> addPluralSubgraph(PluralAttribute<? super J, ?, AJ> attribute) {
		return addSubGraph( attribute.getName() );
	}

	/**
	 * Graphs apply only to {@link jakarta.persistence.metamodel.ManagedType}s.
	 *
	 * @return the {@code ManagedType} being graphed here.
	 */
	ManagedDomainType<J> getGraphedType();

	/**
	 * Create a named root {@link Graph} if the given name is not null.
	 *
	 * @param mutable controls whether the resulting {@code Graph} is mutable
	 *
	 * @throws CannotBecomeEntityGraphException If the named attribute is not entity-valued
	 *
	 * @deprecated This will be removed
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	RootGraph<J> makeRootGraph(String name, boolean mutable)
			throws CannotBecomeEntityGraphException;

	/**
	 * Create a new (mutable or immutable) {@link SubGraph} rooted at
	 * this {@link Graph}.
	 *
	 * @deprecated This will be removed
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	SubGraph<J> makeSubGraph(boolean mutable);

	@Override
	Graph<J> makeCopy(boolean mutable);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// AttributeNode handling

	/**
	 * Find an already existing AttributeNode by attributeName within
	 * this container
	 *
	 * @see #getAttributeNode(String)
	 */
	<AJ> AttributeNode<AJ> findAttributeNode(String attributeName);

	@Override
	default <Y> jakarta.persistence.AttributeNode<Y> getAttributeNode(String attributeName) {
		return findAttributeNode( attributeName );
	}

	/**
	 * Find an already existing AttributeNode by corresponding attribute
	 * reference, within this container.
	 *
	 * @see #getAttributeNode(Attribute)
	 */
	<AJ> AttributeNode<AJ> findAttributeNode(PersistentAttribute<? super J, AJ> attribute);

	@Override
	default <Y> jakarta.persistence.AttributeNode<Y> getAttributeNode(Attribute<? super J, Y> attribute) {
		return findAttributeNode( (PersistentAttribute<? super J, Y>) attribute );
	}

	/**
	 * Add an {@link AttributeNode} (with no associated {@link SubGraph})
	 * to this container by attribute reference.
	 *
	 * @see #addAttributeNode(Attribute)
	 */
	<AJ> AttributeNode<AJ> addAttributeNode(PersistentAttribute<? super J,AJ> attribute)
			throws CannotContainSubGraphException;

	@Override
	default <Y> jakarta.persistence.AttributeNode<Y> addAttributeNode(Attribute<? super J, Y> attribute) {
		return addAttributeNode( (PersistentAttribute<? super J,Y>) attribute );
	}

	/**
	 * Get a list of all existing AttributeNodes within this container.
	 *
	 * @see #getAttributeNodes
	 */
	List<AttributeNode<?>> getAttributeNodeList();

	/**
	 * @deprecated Use {@link #getAttributeNodeList}
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	default List<AttributeNode<?>> getGraphAttributeNodes() {
		return getAttributeNodeList();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Subgraph nodes

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the named {@link AttributeNode}.
	 *
	 * @apiNote If no such AttributeNode exists yet, it is created.
	 */
	<AJ> SubGraph<AJ> addSubGraph(String attributeName);

	<AJ> SubGraph<AJ> addSubGraph(String attributeName, Class<AJ> type);

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the {@link AttributeNode} for the given attribute.
	 *
	 * @apiNote If no such AttributeNode exists yet, it is created.
	 */
	<AJ> SubGraph<AJ> addSubGraph(PersistentAttribute<? super J, AJ> attribute);

	<AJ> SubGraph<AJ> addSubGraph(PersistentAttribute<? super J, ? super AJ> attribute, Class<AJ> type);

	@Override
	default <Y> SubGraph<Y> addTreatedSubgraph(Attribute<? super J, ? super Y> attribute, Class<Y> type) {
		return addSubGraph( (PersistentAttribute<? super J, ? super Y>) attribute, type );
	}

	@Override
	default <X> SubGraph<X> addSubgraph(Attribute<? super J, X> attribute) {
		return addSubGraph( (PersistentAttribute<? super J, X>) attribute );
	}

	@Override
	default <X> SubGraph<? extends X> addSubgraph(Attribute<? super J, X> attribute, Class<? extends X> type) {
		return addSubGraph( (PersistentAttribute<? super J, X>) attribute, type );
	}

	@Override
	default <X> SubGraph<X> addSubgraph(String name) {
		return addSubGraph( name );
	}

	@Override
	default <X> SubGraph<X> addSubgraph(String name, Class<X> type) {
		return addSubGraph( name, type );
	}

	<AJ> SubGraph<AJ> addKeySubGraph(String attributeName);
	<AJ> SubGraph<AJ> addKeySubGraph(String attributeName, Class<AJ> type);

	@Override @Deprecated(forRemoval = true)
	default <X> SubGraph<X> addKeySubgraph(Attribute<? super J, X> attribute) {
		throw new UnsupportedOperationException("This operation will be removed in JPA 4");
	}

	@Override @Deprecated(forRemoval = true)
	default <X> SubGraph<? extends X> addKeySubgraph(Attribute<? super J, X> attribute, Class<? extends X> type) {
		throw new UnsupportedOperationException("This operation will be removed in JPA 4");
	}

	@Override
	default <X> SubGraph<X> addKeySubgraph(String name) {
		return addKeySubGraph( name );
	}

	@Override
	default <X> SubGraph<X> addKeySubgraph(String name, Class<X> type) {
		return addKeySubGraph( name, type );
	}

	@Override
	<E> SubGraph<E> addTreatedElementSubgraph(PluralAttribute<? super J, ?, ? super E> attribute, Class<E> type);

	@Override
	<K> SubGraph<K> addTreatedMapKeySubgraph(MapAttribute<? super J, ? super K, ?> attribute, Class<K> type);

}
