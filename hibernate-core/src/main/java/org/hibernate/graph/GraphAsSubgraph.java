/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
