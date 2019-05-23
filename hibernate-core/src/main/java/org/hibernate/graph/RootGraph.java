/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph;

import java.util.List;
import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.Subgraph;
import javax.persistence.metamodel.Attribute;

import org.hibernate.metamodel.model.domain.PersistentAttribute;

/**
 * Hibernate extension to the JPA {@link EntityGraph} contract.
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface RootGraph<J> extends Graph<J>, EntityGraph<J> {

	// todo (6.0) : do we want to consolidate this functionality on AttributeNodeContainer?

	boolean appliesTo(String entityName);

	boolean appliesTo(Class<? super J> entityType);

	@Override
	RootGraph<J> makeRootGraph(String name, boolean mutable);

	SubGraph<J> makeSubGraph(boolean mutable);

	@Override
	<T1> SubGraph<? extends T1> addSubclassSubgraph(Class<? extends T1> type);

	@Override
	@SuppressWarnings("unchecked")
	default List<AttributeNode<?>> getAttributeNodes() {
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
	default void addAttributeNodes(Attribute<J, ?>... attributes) {
		if ( attributes == null ) {
			return;
		}

		for ( Attribute<J, ?> attribute : attributes ) {
			addAttributeNode( (PersistentAttribute) attribute );
		}
	}

	@Override
	default <X> SubGraph<X> addSubgraph(Attribute<J, X> attribute) {
		//noinspection unchecked
		return addSubGraph( (PersistentAttribute) attribute );
	}

	@Override
	default <X> SubGraph<? extends X> addSubgraph(Attribute<J, X> attribute, Class<? extends X> type) {
		//noinspection unchecked
		return addSubGraph( (PersistentAttribute) attribute, type );
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
		//noinspection unchecked
		return addKeySubGraph( (PersistentAttribute) attribute );
	}

	@Override
	default <X> SubGraph<? extends X> addKeySubgraph(Attribute<J, X> attribute, Class<? extends X> type) {
		//noinspection unchecked
		return addKeySubGraph( (PersistentAttribute) attribute, type );
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
