/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph;

import java.util.List;
import jakarta.persistence.AttributeNode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.Subgraph;
import jakarta.persistence.metamodel.Attribute;

import org.hibernate.metamodel.model.domain.PersistentAttribute;

/**
 * Extends the JPA-defined {@link EntityGraph} with additional operations.
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 *
 * @see SubGraph
 */
public interface RootGraph<J> extends Graph<J>, EntityGraph<J> {

	@Override
	RootGraph<J> makeRootGraph(String name, boolean mutable);

	SubGraph<J> makeSubGraph(boolean mutable);

	@Override
	<T1> SubGraph<? extends T1> addSubclassSubgraph(Class<? extends T1> type);

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	default List<AttributeNode<?>> getAttributeNodes() {
		return (List) getAttributeNodeList();
	}

	@Override
	default void addAttributeNodes(String... names) {
		if ( names != null ) {
			for ( String name : names ) {
				addAttributeNode( name );
			}
		}
	}

	@Override
	default <X> SubGraph<X> addSubgraph(Attribute<? super J, X> attribute) {
		return addSubGraph( (PersistentAttribute<? super J,X>)  attribute );
	}

	@Override
	default <X> Subgraph<? extends X> addSubgraph(Attribute<? super J, X> attribute, Class<? extends X> type) {
		return addSubGraph( (PersistentAttribute<? super J,X>) attribute, type );
	}

	@Override
	default <X> SubGraph<X> addSubgraph(String name) {
		return addSubGraph( name );
	}

	@Override
	default <X> SubGraph<X> addSubgraph(String name, Class<X> type) {
		return addSubGraph( name, type );
	}

	@Override
	default <X> SubGraph<X> addKeySubgraph(Attribute<? super J, X> attribute) {
		return addKeySubGraph( (PersistentAttribute<? super J,X>) attribute );
	}

	@Override
	default <X> SubGraph<? extends X> addKeySubgraph(Attribute<? super J, X> attribute, Class<? extends X> type) {
		return addKeySubGraph( (PersistentAttribute<? super J,X>) attribute, type );
	}

	@Override
	default <X> SubGraph<X> addKeySubgraph(String name) {
		return addKeySubGraph( name );
	}

	@Override
	default <X> Subgraph<X> addKeySubgraph(String name, Class<X> type) {
		return addKeySubGraph( name, type );
	}
}
