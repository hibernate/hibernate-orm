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

	Map<Class<? extends J>, SubGraphImplementor<? extends J>> getSubGraphMap();
	Map<Class<? extends J>, SubGraphImplementor<? extends J>> getKeySubGraphMap();

	@Override
	default Map<Class<? extends J>, ? extends SubGraph<? extends J>> getSubGraphs() {
		return unmodifiableMap( getSubGraphMap() );
	}

	@Override
	default Map<Class<? extends J>, ? extends SubGraph<? extends J>> getKeySubGraphs() {
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
	SubGraphImplementor<J> makeSubGraph();

	@Override
	SubGraphImplementor<J> makeKeySubGraph();

	@Override
	<S extends J> SubGraphImplementor<S> makeSubGraph(Class<S> subtype);

	@Override
	<S extends J> SubGraphImplementor<S> makeKeySubGraph(Class<S> subtype);

	<S extends J> SubGraphImplementor<S> makeSubGraph(ManagedDomainType<S> subtype);

	<S extends J> SubGraphImplementor<S> makeKeySubGraph(ManagedDomainType<S> subtype);

	void merge(AttributeNodeImplementor<J> other);

	void addSubGraph(SubGraphImplementor<? extends J> subgraph);
}
