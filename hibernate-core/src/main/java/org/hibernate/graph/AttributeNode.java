/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;


import jakarta.persistence.Subgraph;
import jakarta.persistence.metamodel.ManagedType;
import org.hibernate.Incubating;
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
 * @author Strong Liu
 * @author Steve Ebersole
 * @author Andrea Boriero
 * @author Gavin King
 */
public interface AttributeNode<J> extends GraphNode<J>, jakarta.persistence.AttributeNode<J> {

	/**
	 * The {@link PersistentAttribute} represented by this node.
	 */
	PersistentAttribute<?, J> getAttributeDescriptor();

	/**
	 * All value subgraphs rooted at this node.
	 * <p>
	 * Includes treated subgraphs.
	 *
	 * @see jakarta.persistence.AttributeNode#getSubgraphs
	 */
	Map<Class<?>, ? extends SubGraph<?>> getSubGraphs();

	/**
	 * All key subgraphs rooted at this node.
	 * <p>
	 * Includes treated subgraphs.
	 *
	 * @see jakarta.persistence.AttributeNode#getKeySubgraphs
	 */
	Map<Class<?>, ? extends SubGraph<?>> getKeySubGraphs();

	@Override
	default @SuppressWarnings("rawtypes") Map<Class, Subgraph> getSubgraphs() {
		return unmodifiableMap( getSubGraphs() );
	}

	@Override
	default @SuppressWarnings("rawtypes") Map<Class, Subgraph> getKeySubgraphs() {
		return unmodifiableMap( getKeySubGraphs() );
	}

	/**
	 * Create and return a new value {@link SubGraph} rooted at this node,
	 * or return an existing such {@link SubGraph} if there is one.
	 */
	SubGraph<?> makeSubGraph();

	/**
	 * Create and return a new key {@link SubGraph} rooted at this node,
	 * or return an existing such {@link SubGraph} if there is one.
	 */
	SubGraph<?> makeKeySubGraph();

	/**
	 * Create and return a new value {@link SubGraph} rooted at this node,
	 * with the given type, which may be a subtype of the value type,
	 * or return an existing such {@link SubGraph} if there is one.
	 * <p>
	 * If the given type is a proper subtype of the value type, the result
	 * is a treated subgraph.
	 *
	 * @param subtype The type or treated type of the value type
	 */
	<S> SubGraph<S> makeSubGraph(Class<S> subtype);

	/**
	 * Create and return a new value {@link SubGraph} rooted at this node,
	 * with the given type, which may be a subtype of the key type,
	 * or return an existing such {@link SubGraph} if there is one.
	 * <p>
	 * If the given type is a proper subtype of the key type, the result
	 * is a treated subgraph.
	 *
	 * @param subtype The type or treated type of the key type
	 */
	<S> SubGraph<S> makeKeySubGraph(Class<S> subtype);

	/**
	 * Create and return a new value {@link SubGraph} rooted at this node,
	 * with the given type, which may be a subtype of the value type,
	 * or return an existing such {@link SubGraph} if there is one.
	 * <p>
	 * If the given type is a proper subtype of the value type, the result
	 * is a treated subgraph.
	 *
	 * @param subtype The type or treated type of the value type
	 */
	@Incubating
	<S> SubGraph<S> makeSubGraph(ManagedType<S> subtype);

	/**
	 * Create and return a new value {@link SubGraph} rooted at this node,
	 * with the given type, which may be a subtype of the key type,
	 * or return an existing such {@link SubGraph} if there is one.
	 * <p>
	 * If the given type is a proper subtype of the key type, the result
	 * is a treated subgraph.
	 *
	 * @param subtype The type or treated type of the key type
	 */
	@Incubating
	<S> SubGraph<S> makeKeySubGraph(ManagedType<S> subtype);
}
