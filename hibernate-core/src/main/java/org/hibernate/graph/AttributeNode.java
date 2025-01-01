/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;

import java.util.Map;

import org.hibernate.metamodel.model.domain.PersistentAttribute;

/**
 * Extends the JPA-defined {@link AttributeNode} with additional operations.
 *
 * @author Strong Liu
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface AttributeNode<J> extends GraphNode<J>, jakarta.persistence.AttributeNode<J> {

	PersistentAttribute<?, J> getAttributeDescriptor();

	Map<Class<? extends J>, ? extends SubGraph<? extends J>> getSubGraphs();
	Map<Class<? extends J>, ? extends SubGraph<? extends J>> getKeySubGraphs();

	<S extends J> void addSubGraph(Class<S> subType, SubGraph<S> subGraph);
	<S extends J> void addKeySubGraph(Class<S> subType, SubGraph<S> subGraph);

	SubGraph<J> makeSubGraph();
	SubGraph<J> makeKeySubGraph();

	<S extends J> SubGraph<S> makeSubGraph(Class<S> subtype);
	<S extends J> SubGraph<S> makeKeySubGraph(Class<S> subtype);
}
