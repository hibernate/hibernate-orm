/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph;

import java.util.List;

import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.Subgraph;
import javax.persistence.metamodel.Attribute;

/**
 * A proxy class that wraps an {@link EntityGraph} instance and mimics the {@link Subgraph} interface
 * by delegating method calls.
 * 
 * @author asusnjar
 *
 * @param <T> Root entity type.
 */
final class GraphAsSubgraph<T> implements Subgraph<T> {

	/**
	 * A wrapped (proxied) graph.
	 */
	private final EntityGraph<T> graph;

	/**
	 * A root entity type.
	 */
	private final Class<T> rootType;

	/**
	 * A sole constructor.
	 * 
	 * @param graph    Graph to wrap.
	 * 
	 * @param rootType Root entity type of the graph
	 *                 (which, unfortunately, cannot be obtained from the graph itself)
	 */
	GraphAsSubgraph(EntityGraph<T> graph, Class<T> rootType) {
		this.graph = graph;
		this.rootType = rootType;
	}

	/**
	 * @see Subgraph#addAttributeNodes(String...)
	 */
	@Override
	public void addAttributeNodes(String... attributeNames) {
		graph.addAttributeNodes( attributeNames );
	}

	/**
	 * @see Subgraph#addAttributeNodes(Attribute...)
	 */
	@Override
	public void addAttributeNodes(@SuppressWarnings("unchecked") Attribute<T, ?>... attributes) {
		graph.addAttributeNodes( attributes );
	}

	/**
	 * @see Subgraph#addSubgraph(Attribute)
	 */
	@Override
	public <X> Subgraph<X> addSubgraph(Attribute<T, X> attribute) {
		return graph.addSubgraph( attribute );
	}

	/**
	 * @see Subgraph#addSubgraph(Attribute, Class)
	 */
	@Override
	public <X> Subgraph<? extends X> addSubgraph(Attribute<T, X> attribute, Class<? extends X> type) {
		return graph.addSubgraph( attribute, type );
	}

	/**
	 * @see Subgraph#addSubgraph(String)
	 */
	@Override
	public <X> Subgraph<X> addSubgraph(String attributeName) {
		return graph.addSubgraph( attributeName );
	}

	/**
	 * @see Subgraph#addSubgraph(String, Class)
	 */
	@Override
	public <X> Subgraph<X> addSubgraph(String attributeName, Class<X> type) {
		return graph.addSubgraph( attributeName, type );
	}

	/**
	 * @see Subgraph#addKeySubgraph(Attribute)
	 */
	@Override
	public <X> Subgraph<X> addKeySubgraph(Attribute<T, X> attribute) {
		return graph.addKeySubgraph( attribute );
	}

	/**
	 * @see Subgraph#addKeySubgraph(Attribute, Class)
	 */
	@Override
	public <X> Subgraph<? extends X> addKeySubgraph(Attribute<T, X> attribute, Class<? extends X> type) {
		return graph.addKeySubgraph( attribute, type );
	}

	/**
	 * @see Subgraph#addKeySubgraph(String)
	 */
	@Override
	public <X> Subgraph<X> addKeySubgraph(String attributeName) {
		return graph.addKeySubgraph( attributeName );
	}

	/**
	 * @see Subgraph#addKeySubgraph(String, Class)
	 */
	@Override
	public <X> Subgraph<X> addKeySubgraph(String attributeName, Class<X> type) {
		return graph.addKeySubgraph( attributeName, type );
	}

	/**
	 * @see Subgraph#getAttributeNodes()
	 */
	@Override
	public List<AttributeNode<?>> getAttributeNodes() {
		return graph.getAttributeNodes();
	}

	/**
	 * @see Subgraph#getClassType()
	 */
	@Override
	public Class<T> getClassType() {
		return rootType;
	}
}
