/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.spi;

import java.util.Map;
import java.util.function.BiConsumer;
import jakarta.persistence.Subgraph;

import org.hibernate.graph.AttributeNode;
import org.hibernate.graph.SubGraph;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

/**
 * Integration version of the {@link AttributeNode} contract
 *
 * @author Strong Liu
 * @author Steve Ebersole
 */
public interface AttributeNodeImplementor<J> extends AttributeNode<J>, GraphNodeImplementor<J> {
	Map<Class<? extends J>, SubGraphImplementor<? extends J>> getSubGraphMap();
	Map<Class<? extends J>, SubGraphImplementor<? extends J>> getKeySubGraphMap();

	default void visitSubGraphs(BiConsumer<Class<? extends J>, SubGraphImplementor<? extends J>> consumer) {
		getSubGraphMap().forEach( consumer );
	}

	default void visitKeySubGraphs(BiConsumer<Class<? extends J>, SubGraphImplementor<? extends J>> consumer) {
		getKeySubGraphMap().forEach( consumer );
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	default Map<Class<? extends J>, SubGraph<? extends J>> getSubGraphs() {
		return (Map) getSubGraphMap();
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	default Map<Class<? extends J>, SubGraph<? extends J>> getKeySubGraphs() {
		return (Map) getKeySubGraphMap();
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	default Map<Class, Subgraph> getSubgraphs() {
		return (Map) getSubGraphMap();
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	default Map<Class, Subgraph> getKeySubgraphs() {
		return (Map) getKeySubGraphMap();
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

	void merge(AttributeNodeImplementor<?> attributeNode);

	void addSubGraph(SubGraphImplementor<? extends J> subGraph);
}
