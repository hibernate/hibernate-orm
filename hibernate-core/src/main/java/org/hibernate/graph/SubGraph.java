/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph;

import java.util.List;
import javax.persistence.metamodel.Attribute;

/**
 * Hibernate extension to the JPA entity-graph Subgraph contract.
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface SubGraph<J> extends Graph<J>, javax.persistence.Subgraph<J> {
	@Override
	@SuppressWarnings("unchecked")
	default List<javax.persistence.AttributeNode<?>> getAttributeNodes() {
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
			addAttributeNode( node );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	default <X> SubGraph<X> addSubgraph(Attribute<J, X> attribute) {
		return addSubGraph( attribute );
	}

	@Override
	@SuppressWarnings("unchecked")
	default <X> SubGraph<? extends X> addSubgraph(Attribute<J, X> attribute, Class<? extends X> type) {
		return addSubGraph( attribute, type );
	}

	@Override
	@SuppressWarnings("unchecked")
	default <X> SubGraph<X> addSubgraph(String name) {
		return addSubGraph( name );
	}

	@Override
	@SuppressWarnings("unchecked")
	default <X> SubGraph<X> addSubgraph(String name, Class<X> type) {
		return addSubGraph( name, type );
	}

	@Override
	@SuppressWarnings("unchecked")
	default <X> SubGraph<X> addKeySubgraph(Attribute<J, X> attribute) {
		return addKeySubGraph( attribute );
	}

	@Override
	@SuppressWarnings("unchecked")
	default <X> SubGraph<? extends X> addKeySubgraph(Attribute<J, X> attribute, Class<? extends X> type) {
		return addKeySubGraph( attribute, type );
	}

	@Override
	default <X> SubGraph<X> addKeySubgraph(String name) {
		return addKeySubGraph( name );
	}

	@Override
	@SuppressWarnings("unchecked")
	default <X> SubGraph<X> addKeySubgraph(String name, Class<X> type) {
		return addKeySubGraph( name, type );
	}

	@Override
	@SuppressWarnings("unchecked")
	default Class<J> getClassType() {
		return getGraphedType().getJavaType();
	}
}
