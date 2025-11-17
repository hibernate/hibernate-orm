/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.spi;

import org.hibernate.graph.AttributeNode;

import java.util.Map;


/**
 * Integration version of the {@link AttributeNode} contract.
 *
 * @param <J> The type of the attribute
 * @param <E> The element type, if this node represents a
 *        {@linkplain jakarta.persistence.metamodel.PluralAttribute plural attribute},
 *        or the type of the singular attribute, if it doesn't
 * @param <K> The map key type, if this node represents a
 *        {@linkplain jakarta.persistence.metamodel.MapAttribute map attribute}
 *
 * @author Strong Liu
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface AttributeNodeImplementor<J, E, K> extends AttributeNode<J>, GraphNodeImplementor<J> {

	@Override
	AttributeNodeImplementor<J, E, K> makeCopy(boolean mutable);

	/**
	 * Create a value subgraph, without knowing whether it represents a singular value or
	 * plural element, rooted at this attribute node.
	 *
	 * @apiNote This version is more lenient and is therefore disfavored. Prefer the use
	 *          of {@link #addSingularSubgraph()} and {@link #addElementSubgraph()}.
	 */
	SubGraphImplementor<E> addValueSubgraph();

	/**
	 * Create a value subgraph representing a singular value rooted at this attribute node.
	 */
	SubGraphImplementor<J> addSingularSubgraph();

	/**
	 * Create a value subgraph representing a plural element rooted at this attribute node.
	 */
	SubGraphImplementor<E> addElementSubgraph();

	/**
	 * Create a key subgraph rooted at this attribute node.
	 */
	SubGraphImplementor<K> addKeySubgraph();

	@Override @Deprecated
	SubGraphImplementor<?> makeSubGraph();

	@Override @Deprecated
	SubGraphImplementor<?> makeKeySubGraph();

	@Override @Deprecated
	<S> SubGraphImplementor<S> makeSubGraph(Class<S> subtype);

	@Override @Deprecated
	<S> SubGraphImplementor<S> makeKeySubGraph(Class<S> subtype);

	void merge(AttributeNodeImplementor<J,E,K> other);

	@Override
	Map<Class<?>, SubGraphImplementor<?>> getSubGraphs();

	@Override
	Map<Class<?>, SubGraphImplementor<?>> getKeySubGraphs();

	SubGraphImplementor<E> getValueSubgraph();

	SubGraphImplementor<K> getKeySubgraph();
}
