/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;


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
	default @SuppressWarnings("rawtypes") Map<Class, Subgraph> getSubgraphs() {
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
	default @SuppressWarnings("rawtypes") Map<Class, Subgraph> getKeySubgraphs() {
		return unmodifiableMap( getKeySubGraphs() );
	}

	/**
	 * Create and return a new value {@link SubGraph} rooted at this node,
	 * or return an existing such {@link SubGraph} if there is one.
	 *
	 * @deprecated This operation is not properly type safe.
	 * Note that {@code graph.addAttributeNode(att).makeSubGraph()} is a
	 * synonym for {@code graph.addSubgraph(att)}.
	 *
	 * @see Graph#addSubgraph(jakarta.persistence.metamodel.Attribute)
	 */
	@Deprecated(since = "7.0")
	SubGraph<?> makeSubGraph();

	/**
	 * Create and return a new key {@link SubGraph} rooted at this node,
	 * or return an existing such {@link SubGraph} if there is one.
	 *
	 * @deprecated This operation is not properly type safe.
	 * Note that {@code graph.addAttributeNode(att).makeKeySubGraph()} is a
	 * synonym for {@code graph.addMapKeySubgraph(att)}.
	 *
	 * @see Graph#addMapKeySubgraph(jakarta.persistence.metamodel.MapAttribute)
	 */
	@Deprecated(since = "7.0")
	SubGraph<?> makeKeySubGraph();

	/**
	 * Create and return a new value {@link SubGraph} rooted at this node,
	 * with the given type, which may be a subtype of the value type,
	 * or return an existing such {@link SubGraph} if there is one.
	 * <p>
	 * If the given type is a proper subtype of the value type, the result
	 * is a treated subgraph.
	 *
	 * @deprecated This operation is not properly type safe.
	 * Note that {@code graph.addAttributeNode(att).makeSubGraph(cl)}
	 * is a synonym for {@code graph.addTreatedSubgraph(att,cl)}.
	 *
	 * @param subtype The type or treated type of the value type
	 *
	 * @see Graph#addTreatedSubgraph(jakarta.persistence.metamodel.Attribute, Class)
	 */
	@Deprecated(since = "7.0")
	<S> SubGraph<S> makeSubGraph(Class<S> subtype);

	/**
	 * Create and return a new value {@link SubGraph} rooted at this node,
	 * with the given type, which may be a subtype of the key type,
	 * or return an existing such {@link SubGraph} if there is one.
	 * <p>
	 * If the given type is a proper subtype of the key type, the result
	 * is a treated subgraph.
	 *
	 * @deprecated This operation is not properly type safe.
	 * Note that {@code graph.addAttributeNode(att).makeKeySubGraph(cl)}
	 * is a synonym for {@code graph.addTreatedMapKeySubgraph(att,cl)}.
	 *
	 * @param subtype The type or treated type of the key type
	 *
	 * @see Graph#addTreatedMapKeySubgraph(jakarta.persistence.metamodel.MapAttribute,Class)
	 */
	@Deprecated(since = "7.0")
	<S> SubGraph<S> makeKeySubGraph(Class<S> subtype);
}
