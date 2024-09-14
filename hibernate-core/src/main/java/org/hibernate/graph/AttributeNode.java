/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph;

import java.util.Map;
import jakarta.persistence.Subgraph;

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

	Map<Class<? extends J>, SubGraph<? extends J>> getSubGraphs();
	Map<Class<? extends J>, SubGraph<? extends J>> getKeySubGraphs();

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	default Map<Class, Subgraph> getSubgraphs() {
		return (Map) getSubGraphs();
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	default Map<Class, Subgraph> getKeySubgraphs() {
		return (Map) getKeySubGraphs();
	}

	<S extends J> void addSubGraph(Class<S> subType, SubGraph<S> subGraph);
	<S extends J> void addKeySubGraph(Class<S> subType, SubGraph<S> subGraph);

	SubGraph<J> makeSubGraph();
	SubGraph<J> makeKeySubGraph();

	<S extends J> SubGraph<S> makeSubGraph(Class<S> subtype);
	<S extends J> SubGraph<S> makeKeySubGraph(Class<S> subtype);
}
