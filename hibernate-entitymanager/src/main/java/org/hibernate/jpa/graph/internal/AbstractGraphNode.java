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
package org.hibernate.jpa.graph.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.AttributeNode;
import javax.persistence.metamodel.Attribute;

import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphNodeImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.jpa.spi.HibernateEntityManagerFactoryAware;
import org.jboss.logging.Logger;

/**
 * Base class for EntityGraph and Subgraph implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractGraphNode<T> implements GraphNodeImplementor, HibernateEntityManagerFactoryAware {
	private static final Logger log = Logger.getLogger( AbstractGraphNode.class );

	private final HibernateEntityManagerFactory entityManagerFactory;
	private final boolean mutable;

	private Map<String, AttributeNodeImplementor<?>> attributeNodeMap;

	protected AbstractGraphNode(HibernateEntityManagerFactory entityManagerFactory, boolean mutable) {
		this.entityManagerFactory = entityManagerFactory;
		this.mutable = mutable;
	}

	protected AbstractGraphNode(AbstractGraphNode<T> original, boolean mutable) {
		this.entityManagerFactory = original.entityManagerFactory;
		this.mutable = mutable;
		this.attributeNodeMap = makeSafeMapCopy( original.attributeNodeMap );
	}

	private static Map<String, AttributeNodeImplementor<?>> makeSafeMapCopy(Map<String, AttributeNodeImplementor<?>> attributeNodeMap) {
		if ( attributeNodeMap == null ) {
			return null;
		}

		final int properSize = CollectionHelper.determineProperSizing( attributeNodeMap );
		final HashMap<String,AttributeNodeImplementor<?>> copy = new HashMap<String,AttributeNodeImplementor<?>>( properSize );
		for ( Map.Entry<String,AttributeNodeImplementor<?>> attributeNodeEntry : attributeNodeMap.entrySet() ) {
			copy.put(
					attributeNodeEntry.getKey(),
					( ( AttributeNodeImpl ) attributeNodeEntry.getValue() ).makeImmutableCopy()
			);
		}
		return copy;
	}

	@Override
	public HibernateEntityManagerFactory getFactory() {
		return entityManagerFactory;
	}

	@Override
	public List<AttributeNodeImplementor<?>> attributeImplementorNodes() {
		if ( attributeNodeMap == null ) {
			return Collections.emptyList();
		}
		else {
			return new ArrayList<AttributeNodeImplementor<?>>( attributeNodeMap.values() );
		}
	}

	@Override
	public List<AttributeNode<?>> attributeNodes() {
		if ( attributeNodeMap == null ) {
			return Collections.emptyList();
		}
		else {
			return new ArrayList<AttributeNode<?>>( attributeNodeMap.values() );
		}
	}

	public void addAttributeNodes(String... attributeNames) {
		for ( String attributeName : attributeNames ) {
			addAttribute( attributeName );
		}
	}

	public AttributeNodeImpl addAttribute(String attributeName) {
		return addAttributeNode( buildAttributeNode( attributeName ) );
	}

	@SuppressWarnings("unchecked")
	private AttributeNodeImpl<?> buildAttributeNode(String attributeName) {
		return buildAttributeNode( resolveAttribute( attributeName ) );
	}

	protected abstract Attribute<T,?> resolveAttribute(String attributeName);

	protected <X> AttributeNodeImpl<X> buildAttributeNode(Attribute<T, X> attribute) {
		return new AttributeNodeImpl<X>( entityManagerFactory, attribute );
	}

	protected AttributeNodeImpl addAttributeNode(AttributeNodeImpl attributeNode) {
		if ( ! mutable ) {
			throw new IllegalStateException( "Entity/sub graph is not mutable" );
		}

		if ( attributeNodeMap == null ) {
			attributeNodeMap = new HashMap<String, AttributeNodeImplementor<?>>();
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
	public <X> SubgraphImpl<X> addSubgraph(Attribute<T, X> attribute) {
		return addAttribute( attribute ).makeSubgraph();
	}

	@SuppressWarnings("unchecked")
	public <X> SubgraphImpl<? extends X> addSubgraph(Attribute<T, X> attribute, Class<? extends X> type) {
		return addAttribute( attribute ).makeSubgraph( type );
	}

	@SuppressWarnings("unchecked")
	public <X> SubgraphImpl<X> addSubgraph(String attributeName) {
		return addAttribute( attributeName ).makeSubgraph();
	}

	@SuppressWarnings("unchecked")
	public <X> SubgraphImpl<X> addSubgraph(String attributeName, Class<X> type) {
		return addAttribute( attributeName ).makeSubgraph( type );
	}

	@SuppressWarnings("unchecked")
	public <X> SubgraphImpl<X> addKeySubgraph(Attribute<T, X> attribute) {
		return addAttribute( attribute ).makeKeySubgraph();
	}

	@SuppressWarnings("unchecked")
	public <X> SubgraphImpl<? extends X> addKeySubgraph(Attribute<T, X> attribute, Class<? extends X> type) {
		return addAttribute( attribute ).makeKeySubgraph( type );
	}

	@SuppressWarnings("unchecked")
	public <X> SubgraphImpl<X> addKeySubgraph(String attributeName) {
		return addAttribute( attributeName ).makeKeySubgraph();
	}

	@SuppressWarnings("unchecked")
	public <X> SubgraphImpl<X> addKeySubgraph(String attributeName, Class<X> type) {
		return addAttribute( attributeName ).makeKeySubgraph( type );
	}
	
	@Override
	public boolean containsAttribute(String name) {
		return attributeNodeMap != null && attributeNodeMap.containsKey( name );
	}
}
