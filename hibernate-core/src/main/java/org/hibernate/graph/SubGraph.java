/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph;

import java.util.List;
import jakarta.persistence.metamodel.Attribute;

import org.hibernate.metamodel.model.domain.PersistentAttribute;

/**
 * Hibernate extension to the JPA entity-graph Subgraph contract.
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface SubGraph<J> extends Graph<J>, jakarta.persistence.Subgraph<J> {
	@Override
	@SuppressWarnings("unchecked")
	default List<jakarta.persistence.AttributeNode<?>> getAttributeNodes() {
		return (List) getAttributeNodeList();
	}

	@Override
	default void addAttributeNodes(String... names) {
		if ( names == null ) {
			return;
		}

		for ( String name : names ) {
			addAttributeNode( name );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	default void addAttributeNodes(Attribute<J, ?>... attribute) {
		if ( attribute == null ) {
			return;
		}

		for ( Attribute<J, ?> node : attribute ) {
			assert node instanceof PersistentAttribute;
			addAttributeNode( (PersistentAttribute<J, ?>) node );
		}
	}

	@Override
	default <X> SubGraph<X> addSubgraph(Attribute<J, X> attribute) {
		return addSubGraph( (PersistentAttribute<J, X>) attribute );
	}

	@Override
	default <X> SubGraph<? extends X> addSubgraph(Attribute<J, X> attribute, Class<? extends X> type) {
		return addSubGraph( (PersistentAttribute<J, X>) attribute, type );
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
	default <X> SubGraph<X> addKeySubgraph(Attribute<J, X> attribute) {
		return addKeySubGraph( (PersistentAttribute<J, X>) attribute );
	}

	@Override
	default <X> SubGraph<? extends X> addKeySubgraph(Attribute<J, X> attribute, Class<? extends X> type) {
		return addKeySubGraph( (PersistentAttribute<J, X>) attribute, type );
	}

	@Override
	default <X> SubGraph<X> addKeySubgraph(String name) {
		return addKeySubGraph( name );
	}

	@Override
	default <X> SubGraph<X> addKeySubgraph(String name, Class<X> type) {
		return addKeySubGraph( name, type );
	}

	@Override
	default Class<J> getClassType() {
		return getGraphedType().getJavaType();
	}
}
