/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.spi;

import java.util.Map;

import jakarta.persistence.Subgraph;

import org.hibernate.graph.AttributeNode;
import org.hibernate.graph.SubGraph;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

import static java.util.Collections.unmodifiableMap;

/**
 * Integration version of the {@link AttributeNode} contract
 *
 * @author Strong Liu
 * @author Steve Ebersole
 */
public interface AttributeNodeImplementor<J> extends AttributeNode<J>, GraphNodeImplementor<J> {

	Map<Class<?>, SubGraphImplementor<?>> getSubGraphMap();
	Map<Class<?>, SubGraphImplementor<?>> getKeySubGraphMap();

	@Override
	default Map<Class<?>, ? extends SubGraph<?>> getSubGraphs() {
		return unmodifiableMap( getSubGraphMap() );
	}

	@Override
	default Map<Class<?>, ? extends SubGraph<?>> getKeySubGraphs() {
		return unmodifiableMap( getKeySubGraphMap() );
	}

	@Override // JPA API uses raw types
	default @SuppressWarnings("rawtypes") Map<Class, Subgraph> getSubgraphs() {
		return unmodifiableMap( getSubGraphMap() );
	}

	@Override // JPA API uses raw types
	default @SuppressWarnings("rawtypes") Map<Class, Subgraph> getKeySubgraphs() {
		return unmodifiableMap( getKeySubGraphMap() );
	}

	@Override
	AttributeNodeImplementor<J> makeCopy(boolean mutable);

	@Override
	SubGraphImplementor<?> makeSubGraph();

	@Override
	SubGraphImplementor<?> makeKeySubGraph();

	@Override
	<S> SubGraphImplementor<S> makeSubGraph(Class<S> type);

	@Override
	<S> SubGraphImplementor<S> makeKeySubGraph(Class<S> type);

	@Override
	<S> SubGraphImplementor<S> makeSubGraph(ManagedDomainType<S> subtype);

	@Override
	<S> SubGraphImplementor<S> makeKeySubGraph(ManagedDomainType<S> subtype);

	void merge(AttributeNodeImplementor<J> other);

	void addSubGraph(SubGraphImplementor<?> subgraph);

	void addKeySubGraph(SubGraphImplementor<?> subgraph);
}
