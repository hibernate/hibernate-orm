/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.internal.graph;

import javax.persistence.AttributeNode;
import javax.persistence.Subgraph;
import javax.persistence.metamodel.Attribute;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * @author Steve Ebersole
 */
public class GraphDelegate<T> {
	private static final Logger log = Logger.getLogger( GraphDelegate.class );

	private final AttributeDelegate<T> attributeDelegate;
	private final boolean mutable;
	private Map<String, GraphNode<?>> graphNodeMap;

	public GraphDelegate(AttributeDelegate<T> attributeDelegate) {
		this.attributeDelegate = attributeDelegate;
		this.mutable = true;
	}

	private GraphDelegate(
			AttributeDelegate<T> attributeDelegate,
			GraphDelegate<T> original,
			boolean mutable) {
		this.attributeDelegate = attributeDelegate;
		this.graphNodeMap = makeSafeGraphNodeMapCopy( original.graphNodeMap );
		this.mutable = mutable;
	}

	public GraphDelegate<T> makeImmutableCopy(AttributeDelegate<T> attributeDelegate) {
		return new GraphDelegate<T>( attributeDelegate, this, false );
	}

	public GraphDelegate<T> makeMutableCopy(AttributeDelegate<T> attributeDelegate) {
		return new GraphDelegate<T>( attributeDelegate, this, true );
	}

	private static Map<String, GraphNode<?>> makeSafeGraphNodeMapCopy(Map<String, GraphNode<?>> graphNodeMap) {
		if ( graphNodeMap == null ) {
			return null;
		}

		final HashMap<String,GraphNode<?>> copy
				= new HashMap<String,GraphNode<?>>( CollectionHelper.determineProperSizing( graphNodeMap ) );
		for ( Map.Entry<String,GraphNode<?>> attributeNodeEntry : graphNodeMap.entrySet() ) {
			copy.put(
					attributeNodeEntry.getKey(),
					attributeNodeEntry.getValue().makeImmutableCopy()
			);
		}
		return copy;
	}

	public Collection<GraphNode<?>> getGraphNodes() {
		return graphNodeMap.values();
	}

	public void addAttributeNodes(String... attributeNames) {
		checkMutability();

		for ( String attributeName : attributeNames ) {
			addNode( attributeDelegate.buildAttributeNode( attributeName ) );
		}
	}

	private void checkMutability() {
		if ( ! mutable ) {
			throw new IllegalStateException( "Entity graph is not mutable" );
		}
	}

	private void addNode(GraphNode graphNode) {
		if ( graphNodeMap == null ) {
			graphNodeMap = new HashMap<String, GraphNode<?>>();
		}
		else {
			final AttributeNode old = graphNodeMap.get( graphNode.getRegistrationName() );
			if ( old != null ) {
				log.debugf(
						"Encountered request to add entity graph node [%s] using a name [%s] under which another " +
								"node is already registered [%s]",
						old.getClass().getName(),
						graphNode.getRegistrationName(),
						graphNode.getClass().getName()
				);
			}
		}
		graphNodeMap.put( graphNode.getAttributeName(), graphNode );
	}

	public void addAttributeNodes(Attribute<T, ?>... attributes) {
		checkMutability();

		for ( Attribute<T,?> attribute : attributes ) {
			addNode( attributeDelegate.buildAttributeNode( attribute ) );
		}
	}

	public <X> Subgraph<? extends X> addSubgraph(Attribute<T, X> attribute, Class<? extends X> type) {
		checkMutability();

		final SubgraphImpl<? extends X> subgraph = attributeDelegate.buildSubgraph( attribute, type );
		addNode( subgraph );
		return subgraph;
	}

	@SuppressWarnings("unchecked")
	public <X> Subgraph<X> addSubgraph(String attributeName) {
		checkMutability();

		final SubgraphImpl<X> subgraph = attributeDelegate.buildSubgraph( attributeName );
		addNode( subgraph );
		return subgraph;
	}

	public <X> Subgraph<X> addSubgraph(String attributeName, Class<X> type) {
		checkMutability();

		final SubgraphImpl<X> subgraph = attributeDelegate.buildSubgraph( attributeName, type );
		addNode( subgraph );
		return subgraph;
	}

	public <X> Subgraph<? extends X> addKeySubgraph(Attribute<T, X> attribute, Class<? extends X> type) {
		checkMutability();

		final SubgraphImpl<? extends X> subgraph = attributeDelegate.buildKeySubgraph( attribute, type );
		addNode( subgraph );
		return subgraph;
	}

	@SuppressWarnings("unchecked")
	public <X> Subgraph<X> addKeySubgraph(String attributeName) {
		checkMutability();

		final SubgraphImpl<X> subgraph = attributeDelegate.buildKeySubgraph( attributeName );
		addNode( subgraph );
		return subgraph;
	}

	public <X> Subgraph<X> addKeySubgraph(String attributeName, Class<X> type) {
		checkMutability();

		final SubgraphImpl<X> subgraph = attributeDelegate.buildKeySubgraph( attributeName, type );
		addNode( subgraph );
		return subgraph;
	}


	public static interface AttributeDelegate<T> {
		public AttributeNodeImpl<?> buildAttributeNode(String attributeName);
		public AttributeNodeImpl<?> buildAttributeNode(Attribute<T,?> attribute);

		public SubgraphImpl buildSubgraph(String atributeName);
		public <X> SubgraphImpl<X> buildSubgraph(String atributeName, Class<X> type);
		public <X> SubgraphImpl<? extends X> buildSubgraph(Attribute<T, X> attribute, Class<? extends X> type);

		public SubgraphImpl buildKeySubgraph(String attributeName);
		public <X> SubgraphImpl<X> buildKeySubgraph(String attributeName, Class<X> type);
		public <X> SubgraphImpl<? extends X> buildKeySubgraph(Attribute<T,X> attribute, Class<? extends X> type);
	}

}
