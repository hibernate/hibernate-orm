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
	 */
	RootGraph<J> makeRootGraph(String name, boolean mutable)
			throws CannotBecomeEntityGraphException;

	/**
	 * Create a new (mutable or immutable) {@link SubGraph} rooted at
	 * this {@link Graph}.
	 */
	SubGraph<J> makeSubGraph(boolean mutable);

	@Override
	Graph<J> makeCopy(boolean mutable);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// AttributeNode handling

	/**
	 * Ultimately only needed for implementing
	 * {@link jakarta.persistence.EntityGraph#getAttributeNodes()}
	 * and {@link jakarta.persistence.Subgraph#getAttributeNodes()}
	 */
	List<AttributeNode<?>> getGraphAttributeNodes();

	/**
	 * Find an already existing AttributeNode by attributeName within
	 * this container
	 */
	<AJ> AttributeNode<AJ> findAttributeNode(String attributeName);

	/**
	 * Find an already existing AttributeNode by corresponding attribute
	 * reference, within this container.
	 */
	<AJ> AttributeNode<AJ> findAttributeNode(PersistentAttribute<? super J, AJ> attribute);

	/**
	 * Get a list of all existing AttributeNodes within this container.
	 */
	List<AttributeNode<?>> getAttributeNodeList();

	/**
	 * Add an {@link AttributeNode} (with no associated {@link SubGraph})
	 * to this container by attribute reference.
	 */
	<AJ> AttributeNode<AJ> addAttributeNode(PersistentAttribute<? super J,AJ> attribute);



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// sub graph nodes

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the named {@link AttributeNode}.
	 *
	 * @apiNote If no such AttributeNode exists yet, it is created.
	 */
	<AJ> SubGraph<AJ> addSubGraph(String attributeName)
			throws CannotContainSubGraphException;

	<AJ> SubGraph<AJ> addSubGraph(String attributeName, Class<AJ> type)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the {@link AttributeNode} for the given attribute.
	 *
	 * @apiNote If no such AttributeNode exists yet, it is created.
	 */
	<AJ> SubGraph<AJ> addSubGraph(PersistentAttribute<? super J, AJ> attribute)
			throws CannotContainSubGraphException;

	<AJ> SubGraph<? extends AJ> addSubGraph(PersistentAttribute<? super J, AJ> attribute, Class<? extends AJ> type)
			throws CannotContainSubGraphException;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// key sub graph nodes

	<AJ> SubGraph<AJ> addKeySubGraph(String attributeName)
			throws CannotContainSubGraphException;
	<AJ> SubGraph<AJ> addKeySubGraph(String attributeName, Class<AJ> type)
			throws CannotContainSubGraphException;

	<AJ> SubGraph<AJ> addKeySubGraph(PersistentAttribute<? super J,AJ> attribute)
			throws CannotContainSubGraphException;
	<AJ> SubGraph<? extends AJ> addKeySubGraph(PersistentAttribute<? super J,AJ> attribute, Class<? extends AJ> type)
			throws CannotContainSubGraphException;


	@Override
	<Y> SubGraph<Y> addTreatedSubgraph(Attribute<? super J, ? super Y> attribute, Class<Y> type);

	@Override
	<E> SubGraph<E> addTreatedElementSubgraph(PluralAttribute<? super J, ?, ? super E> attribute, Class<E> type);

	@Override
	<K> SubGraph<K> addTreatedMapKeySubgraph(MapAttribute<? super J, ? super K, ?> attribute, Class<K> type);
}
