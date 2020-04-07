/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph.spi;

import java.util.Map;
import java.util.function.BiConsumer;
import javax.persistence.Subgraph;

import org.hibernate.graph.AttributeNode;
import org.hibernate.graph.SubGraph;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;

/**
 * Integration version of the AttributeNode contract
 *
 * @author <a href="mailto:stliu@hibernate.org">Strong Liu</a>
 * @author Steve Ebersole
 */
public interface AttributeNodeImplementor<J> extends AttributeNode<J>, GraphNodeImplementor<J> {
	@Override
	PersistentAttributeDescriptor<?, J> getAttributeDescriptor();

	Map<Class<? extends J>, SubGraphImplementor<? extends J>> getSubGraphMap();
	Map<Class<? extends J>, SubGraphImplementor<? extends J>> getKeySubGraphMap();

	default void visitSubGraphs(BiConsumer<Class<?>, SubGraphImplementor<?>> consumer) {
		getSubGraphMap().forEach( consumer );
	}

	default void visitKeySubGraphs(BiConsumer<Class<?>, SubGraphImplementor<?>> consumer) {
		getKeySubGraphMap().forEach( consumer );
	}

	@Override
	@SuppressWarnings("unchecked")
	default Map<Class<? extends J>, SubGraph<? extends J>> getSubGraphs() {
		return (Map) getSubGraphMap();
	}

	@Override
	@SuppressWarnings("unchecked")
	default Map<Class<? extends J>, SubGraph<? extends J>> getKeySubGraphs() {
		return (Map) getKeySubGraphMap();
	}

	@Override
	@SuppressWarnings("unchecked")
	default Map<Class, Subgraph> getSubgraphs() {
		return (Map) getSubGraphMap();
	}

	@Override
	@SuppressWarnings("unchecked")
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

	<S extends J> SubGraphImplementor<S> makeSubGraph(ManagedTypeDescriptor<S> subtype);

	<S extends J> SubGraphImplementor<S> makeKeySubGraph(ManagedTypeDescriptor<S> subtype);

	void merge(AttributeNodeImplementor<?> attributeNode);
}
