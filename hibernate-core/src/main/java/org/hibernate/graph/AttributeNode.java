/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;


import jakarta.annotation.Nonnull;
import jakarta.persistence.Subgraph;
import org.hibernate.metamodel.model.domain.PersistentAttribute;

import java.util.Map;

import static java.util.Collections.unmodifiableMap;

/**
 * Represents a fetched {@linkplain jakarta.persistence.metamodel.Attribute attribute} in an
 * {@linkplain Graph entity graph}.
 * <p>
 * An {@code AttributeNode} representing an attribute whose type is a managed type or collection
 * of some managed type may have an associated <em>value subgraph</em>, which is represented by
 * an instance of {@link SubGraph}.
 * <ul>
 * <li>For a {@linkplain jakarta.persistence.metamodel.SingularAttribute singular attribute},
 *     the value type is the type of the attribute.
 * <li>For a {@linkplain jakarta.persistence.metamodel.PluralAttribute plural attribute}, the
 *     value type is the collection element type.
 * </ul>
 * <p>
 * Or, if the represented attribute is a {@link Map}, the {@code AttributeNode} maye have an
 * associated <em>key subgraph</em>, similarly represented by a {@link SubGraph}.
 * <p>
 * Not every attribute node has a subgraph.
 * <p>
 * Extends the JPA-defined {@link jakarta.persistence.AttributeNode} with additional operations.
 *
 * @apiNote Historically, this interface declared operations with incorrect generic types,
 * leading to unsound code. This was in Hibernate 7, with possible breakage to older code.
 *
 * @param <J> The type of the {@linkplain #getAttributeDescriptor attribute}
 *
 * @author Strong Liu
 * @author Steve Ebersole
 * @author Andrea Boriero
 * @author Gavin King
 */
public interface AttributeNode<J> extends GraphNode<J>, jakarta.persistence.AttributeNode<J> {

	/**
	 * The {@link PersistentAttribute} represented by this node.
	 */
	@Nonnull
	PersistentAttribute<?, J> getAttributeDescriptor();

	/**
	 * Whether this attribute was {@linkplain Graph#removeAttributeNode removed}.
	 * Per the Jakarta Persistence specification, such nodes should actually suppress inclusion of the
	 * attribute mapped for eager fetching when the graph is used as a
	 * {@linkplain org.hibernate.graph.GraphSemantic#LOAD load graph}.
	 */
	boolean isRemoved();

	/**
	 * All value subgraphs rooted at this node.
	 * <p>
	 * Includes treated subgraphs.
	 *
	 * @see jakarta.persistence.AttributeNode#getSubgraphs
	 */
	@Nonnull
	Map<Class<?>, ? extends SubGraph<?>> getSubGraphs();

	/**
	 * All key subgraphs rooted at this node.
	 * <p>
	 * Includes treated subgraphs.
	 *
	 * @see jakarta.persistence.AttributeNode#getKeySubgraphs
	 */
	@Nonnull
	Map<Class<?>, ? extends SubGraph<?>> getKeySubGraphs();

	/**
	 * All value subgraphs rooted at this node.
	 * <p>
	 * Includes treated subgraphs.
	 *
	 * @apiNote This operation is declared with raw types by JPA
	 *
	 * @see #getSubGraphs()
	 */
	@Override
	@Nonnull
	default Map<Class<?>, Subgraph<?>> getSubgraphs() {
		return unmodifiableMap( getSubGraphs() );
	}

	/**
	 * All key subgraphs rooted at this node.
	 * <p>
	 * Includes treated subgraphs.
	 *
	 * @apiNote This operation is declared with raw types by JPA
	 *
	 * @see #getKeySubGraphs()
	 */
	@Override
	@Nonnull
	default Map<Class<?>, Subgraph<?>> getKeySubgraphs() {
		return unmodifiableMap( getKeySubGraphs() );
	}

}
