/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;


import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;

import java.util.Map;

/**
 * Extends the JPA-defined {@link AttributeNode} with additional operations.
 *
 * @author Strong Liu
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface AttributeNode<J> extends GraphNode<J>, jakarta.persistence.AttributeNode<J> {

	PersistentAttribute<?, J> getAttributeDescriptor();

	Map<Class<?>, ? extends SubGraph<?>> getSubGraphs();
	Map<Class<?>, ? extends SubGraph<?>> getKeySubGraphs();

	SubGraph<?> makeSubGraph();
	SubGraph<?> makeKeySubGraph();

	<S> SubGraph<S> makeSubGraph(Class<S> subtype);
	<S> SubGraph<S> makeKeySubGraph(Class<S> subtype);

	<S> SubGraph<S> makeSubGraph(ManagedDomainType<S> subtype);
	<S> SubGraph<S> makeKeySubGraph(ManagedDomainType<S> subtype);

}
