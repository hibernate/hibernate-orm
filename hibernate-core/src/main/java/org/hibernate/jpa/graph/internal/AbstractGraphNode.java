/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.graph.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.AttributeNode;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphNodeImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.spi.HibernateEntityManagerFactoryAware;

import org.jboss.logging.Logger;

/**
 * Base class for EntityGraph and Subgraph implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractGraphNode<T> implements GraphNodeImplementor, HibernateEntityManagerFactoryAware {
	private static final Logger log = Logger.getLogger( AbstractGraphNode.class );

	private final SessionFactoryImplementor sessionFactory;
	private final boolean mutable;

	private Map<String, AttributeNodeImplementor<?>> attributeNodeMap;

	@SuppressWarnings("WeakerAccess")
	protected AbstractGraphNode(SessionFactoryImplementor sessionFactory, boolean mutable) {
		this.sessionFactory = sessionFactory;
		this.mutable = mutable;
	}

	@SuppressWarnings("WeakerAccess")
	protected AbstractGraphNode(AbstractGraphNode<T> original, boolean mutable) {
		this.sessionFactory = original.sessionFactory;
		this.mutable = mutable;
		this.attributeNodeMap = makeSafeMapCopy( original.attributeNodeMap );
	}

	private static Map<String, AttributeNodeImplementor<?>> makeSafeMapCopy(Map<String, AttributeNodeImplementor<?>> attributeNodeMap) {
		if ( attributeNodeMap == null ) {
			return null;
		}

		final int properSize = CollectionHelper.determineProperSizing( attributeNodeMap );
		final HashMap<String,AttributeNodeImplementor<?>> copy = new HashMap<>( properSize );
		for ( Map.Entry<String,AttributeNodeImplementor<?>> attributeNodeEntry : attributeNodeMap.entrySet() ) {
			copy.put(
					attributeNodeEntry.getKey(),
					( ( AttributeNodeImpl ) attributeNodeEntry.getValue() ).makeImmutableCopy()
			);
		}
		return copy;
	}


	@Override
	public SessionFactoryImplementor getFactory() {
		return sessionFactory;
	}

	@Override
	public List<AttributeNodeImplementor<?>> attributeImplementorNodes() {
		if ( attributeNodeMap == null ) {
			return Collections.emptyList();
		}
		else {
			return new ArrayList<>( attributeNodeMap.values() );
		}
	}

	@Override
	public List<AttributeNode<?>> attributeNodes() {
		if ( attributeNodeMap == null ) {
			return Collections.emptyList();
		}
		else {
			return new ArrayList<>( attributeNodeMap.values() );
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

	@SuppressWarnings("WeakerAccess")
	protected <X> AttributeNodeImpl<X> buildAttributeNode(Attribute<T, X> attribute) {
		return new AttributeNodeImpl<>( sessionFactory, getManagedType(), attribute );
	}

	@SuppressWarnings("WeakerAccess")
	protected AttributeNodeImpl addAttributeNode(AttributeNodeImpl attributeNode) {
		if ( ! mutable ) {
			throw new IllegalStateException( "Entity/sub graph is not mutable" );
		}

		if ( attributeNodeMap == null ) {
			attributeNodeMap = new HashMap<>();
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

	@SuppressWarnings("unchecked")
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

	abstract ManagedType getManagedType();
}
