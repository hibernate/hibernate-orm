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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.HibernateEntityManagerFactory;

/**
 * @author Steve Ebersole
 */
public abstract class GraphNode<T> {
	private static final Logger log = Logger.getLogger( GraphNode.class );

	private final HibernateEntityManagerFactory entityManagerFactory;
	private final boolean mutable;

	private Map<String, AttributeNode<?>> attributeNodeMap;

	protected GraphNode(HibernateEntityManagerFactory entityManagerFactory, boolean mutable) {
		this.entityManagerFactory = entityManagerFactory;
		this.mutable = mutable;
	}

	protected GraphNode(GraphNode<T> original, boolean mutable) {
		this.entityManagerFactory = original.entityManagerFactory;
		this.mutable = mutable;
		this.attributeNodeMap = makeSafeMapCopy( original.attributeNodeMap );
	}

	private static Map<String, AttributeNode<?>> makeSafeMapCopy(Map<String, AttributeNode<?>> attributeNodeMap) {
		if ( attributeNodeMap == null ) {
			return null;
		}

		final int properSize = CollectionHelper.determineProperSizing( attributeNodeMap );
		final HashMap<String,AttributeNode<?>> copy = new HashMap<String,AttributeNode<?>>( properSize );
		for ( Map.Entry<String,AttributeNode<?>> attributeNodeEntry : attributeNodeMap.entrySet() ) {
			copy.put(
					attributeNodeEntry.getKey(),
					( ( AttributeNodeImpl ) attributeNodeEntry.getValue() ).makeImmutableCopy()
			);
		}
		return copy;
	}

	protected HibernateEntityManagerFactory entityManagerFactory() {
		return entityManagerFactory;
	}

	protected List<AttributeNode<?>> attributeNodes() {
		if ( attributeNodeMap == null ) {
			return Collections.emptyList();
		}
		else {
			return new ArrayList<AttributeNode<?>>( attributeNodeMap.values() );
		}
	}

	protected void addAttributeNodes(String... attributeNames) {
		for ( String attributeName : attributeNames ) {
			addAttribute( attributeName );
		}
	}

	protected AttributeNodeImpl addAttribute(String attributeName) {
		return addAttributeNode( buildAttributeNode( attributeName ) );
	}

	@SuppressWarnings("unchecked")
	private AttributeNodeImpl<?> buildAttributeNode(String attributeName) {
		return buildAttributeNode( resolveAttribute( attributeName ) );
	}

	protected abstract Attribute<T,?> resolveAttribute(String attributeName);

	protected AttributeNodeImpl<?> buildAttributeNode(Attribute<T, ?> attribute) {
		return new AttributeNodeImpl<Object>( entityManagerFactory, attribute );
	}

	protected AttributeNodeImpl addAttributeNode(AttributeNodeImpl attributeNode) {
		if ( ! mutable ) {
			throw new IllegalStateException( "Entity/sub graph is not mutable" );
		}

		if ( attributeNodeMap == null ) {
			attributeNodeMap = new HashMap<String, AttributeNode<?>>();
		}
		else {
			final AttributeNode old = attributeNodeMap.get( attributeNode.getRegistrationName() );
			if ( old != null ) {
				log.debugf(
						"Encountered request to add entity graph node [%s] using a name [%s] under which another " +
								"node is already registered [%s]",
						old.getClass().getName(),
						attributeNode.getRegistrationName(),
						attributeNode.getClass().getName()
				);
			}
		}
		attributeNodeMap.put( attributeNode.getRegistrationName(), attributeNode );

		return attributeNode;
	}

	protected void addAttributeNodes(Attribute<T, ?>... attributes) {
		for ( Attribute attribute : attributes ) {
			addAttribute( attribute );
		}
	}

	@SuppressWarnings("unchecked")
	protected AttributeNodeImpl addAttribute(Attribute attribute) {
		return addAttributeNode( buildAttributeNode( attribute ) );
	}

	@SuppressWarnings("unchecked")
	public <X> Subgraph<X> addSubgraph(Attribute<T, X> attribute) {
		return addAttribute( attribute ).makeSubgraph();
	}

	@SuppressWarnings("unchecked")
	public <X> Subgraph<? extends X> addSubgraph(Attribute<T, X> attribute, Class<? extends X> type) {
		return addAttribute( attribute ).makeSubgraph( type );
	}

	@SuppressWarnings("unchecked")
	public <X> Subgraph<X> addSubgraph(String attributeName) {
		return addAttribute( attributeName ).makeSubgraph();
	}

	@SuppressWarnings("unchecked")
	public <X> Subgraph<X> addSubgraph(String attributeName, Class<X> type) {
		return addAttribute( attributeName ).makeSubgraph( type );
	}

	@SuppressWarnings("unchecked")
	public <X> Subgraph<X> addKeySubgraph(Attribute<T, X> attribute) {
		return addAttribute( attribute ).makeKeySubgraph();
	}

	@SuppressWarnings("unchecked")
	public <X> Subgraph<? extends X> addKeySubgraph(Attribute<T, X> attribute, Class<? extends X> type) {
		return addAttribute( attribute ).makeKeySubgraph( type );
	}

	@SuppressWarnings("unchecked")
	public <X> Subgraph<X> addKeySubgraph(String attributeName) {
		return addAttribute( attributeName ).makeKeySubgraph();
	}

	@SuppressWarnings("unchecked")
	public <X> Subgraph<X> addKeySubgraph(String attributeName, Class<X> type) {
		return addAttribute( attributeName ).makeKeySubgraph( type );
	}
}
