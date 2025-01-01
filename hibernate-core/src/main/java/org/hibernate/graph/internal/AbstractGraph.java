/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import org.hibernate.AssertionFailure;
import org.hibernate.graph.AttributeNode;
import org.hibernate.graph.CannotBecomeEntityGraphException;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.SubGraph;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.sqm.SqmPathSource;

import jakarta.persistence.metamodel.Attribute;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

/**
 *  Base class for {@link RootGraph} and {@link SubGraph} implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractGraph<J> extends AbstractGraphNode<J> implements GraphImplementor<J> {
	private final ManagedDomainType<J> managedType;
	private Map<PersistentAttribute<? super J,?>, AttributeNodeImplementor<?>> attributeNodes;

	public AbstractGraph(ManagedDomainType<J> managedType, boolean mutable) {
		super( mutable );
		this.managedType = managedType;
	}

	protected AbstractGraph(ManagedDomainType<J> managedType, GraphImplementor<? super J> graph, boolean mutable) {
		this( managedType, mutable );
		attributeNodes = new HashMap<>( graph.getAttributeNodesByAttribute().size() );
		graph.getAttributeNodesByAttribute()
				.forEach( (attribute, node) -> attributeNodes.put( attribute, node.makeCopy( mutable ) ) );
	}

	protected AbstractGraph(GraphImplementor<J> graph, boolean mutable) {
		this( graph.getGraphedType(), graph, mutable );
	}

	@Override
	public final ManagedDomainType<J> getGraphedType() {
		return managedType;
	}

	@Override @Deprecated(forRemoval = true)
	public RootGraphImplementor<J> makeRootGraph(String name, boolean mutable) {
		if ( getGraphedType() instanceof EntityDomainType ) {
			return new RootGraphImpl<>( name, this, mutable);
		}
		else {
			throw new CannotBecomeEntityGraphException(
					"Cannot transform Graph to RootGraph because '" + getGraphedType() + "' is not an entity type"
			);
		}
	}

	@Override
	public void merge(GraphImplementor<J> graph) {
		if ( graph != null ) {
			verifyMutability();
			graph.getAttributeNodesByAttribute().forEach( (attribute, node) -> {
				final AttributeNodeImplementor<?> existingNode = findAttributeNode( attribute );
				if ( existingNode != null ) {
					// keep the local one, but merge in the incoming one
					mergeNode( node, existingNode );
				}
				else {
					addAttributeNode( attribute, node.makeCopy( true ) );
				}
			} );
		}
	}

	private static <T> void mergeNode(AttributeNodeImplementor<?> node, AttributeNodeImplementor<T> existingNode) {
		if ( existingNode.getAttributeDescriptor() == node.getAttributeDescriptor() ) {
			@SuppressWarnings("unchecked") // safe, we just checked
			final AttributeNodeImplementor<T> castNode = (AttributeNodeImplementor<T>) node;
			existingNode.merge( castNode );
		}
		else {
			throw new AssertionFailure( "Attributes should have been identical" );
		}
	}

	private <T> void addAttributeNode(PersistentAttribute<? super J, ?> attribute, AttributeNodeImplementor<T> node) {
		final AttributeNodeImplementor<T> attributeNode = getNodeForPut( node.getAttributeDescriptor() );
		if ( attributeNode == null ) {
			attributeNodes.put( attribute, node );
		}
		else {
			// we assume the subgraph has been properly copied if needed
			node.getSubGraphMap().forEach( (subtype, subgraph) -> attributeNode.addSubGraph( subgraph ) );
		}
	}

	@Override
	public List<AttributeNode<?>> getAttributeNodeList() {
		return attributeNodes == null ? emptyList() : new ArrayList<>( attributeNodes.values() );
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ> findAttributeNode(String attributeName) {
		final PersistentAttribute<? super J, ?> attribute = findAttributeInSupertypes( attributeName );
		@SuppressWarnings("unchecked") // The JPA API is unsafe by nature
		final PersistentAttribute<? super J, AJ> result = (PersistentAttribute<? super J, AJ>) attribute;
		return attribute == null ? null : findAttributeNode( result );
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ> findAttributeNode(PersistentAttribute<? super J, AJ> attribute) {
		return attributeNodes == null ? null : getNode( attribute );
	}

	@Override
	public List<jakarta.persistence.AttributeNode<?>> getAttributeNodes() {
		return attributeNodes == null ? emptyList() : new ArrayList<>( attributeNodes.values() );
	}

	@Override
	public List<AttributeNodeImplementor<?>> getAttributeNodeImplementors() {
		return attributeNodes == null ? emptyList() : new ArrayList<>( attributeNodes.values() );
	}

	@Override
	public Map<PersistentAttribute<? super J, ?>, AttributeNodeImplementor<?>> getAttributeNodesByAttribute() {
		return attributeNodes == null ? emptyMap() : unmodifiableMap( attributeNodes );
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ> addAttributeNode(String attributeName) {
		return findOrCreateAttributeNode( attributeName );
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ> addAttributeNode(PersistentAttribute<? super J, AJ> attribute) {
		return findOrCreateAttributeNode( attribute );
	}

	@Override
	public <Y> AttributeNodeImplementor<Y> addAttributeNode(Attribute<? super J, Y> attribute) {
		return addAttributeNode( (PersistentAttribute<? super J, Y>) attribute  );
	}

	@Override
	public void addAttributeNodes(String... attributeNames) {
		for ( String attributeName : attributeNames ) {
			addAttributeNode( attributeName );
		}
	}

	@Override @SafeVarargs
	public final void addAttributeNodes(Attribute<? super J, ?>... attributes) {
		for ( Attribute<? super J, ?> attribute : attributes ) {
			addAttributeNode( attribute );
		}
	}

	@Override
	public void removeAttributeNode(String attributeName) {
		attributeNodes.remove( managedType.findAttribute( attributeName ) );
	}

	@Override
	public void removeAttributeNode(Attribute<? super J, ?> attribute) {
		attributeNodes.remove( (PersistentAttribute<? super J, ?>) attribute );
	}

	@Override
	public void removeAttributeNodes(Attribute.PersistentAttributeType nodeType) {
		attributeNodes.keySet().removeIf( entry -> entry.getPersistentAttributeType() == nodeType );
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ> findOrCreateAttributeNode(PersistentAttribute<? super J, AJ> attribute) {
		verifyMutability();
		final AttributeNodeImplementor<AJ> node = getNodeForPut( attribute );
		if ( node == null ) {
			final AttributeNodeImpl<AJ> newAttrNode = new AttributeNodeImpl<>( attribute, isMutable() );
			attributeNodes.put( attribute, newAttrNode );
			return newAttrNode;
		}
		else {
			return node;
		}
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ> findOrCreateAttributeNode(String attributeName) {
		final PersistentAttribute<? super J, ?> attribute = getAttribute( attributeName );
		@SuppressWarnings("unchecked") // The JPA API is unsafe by nature
		final PersistentAttribute<? super J, AJ> persistentAttribute = (PersistentAttribute<? super J, AJ>) attribute;
		return findOrCreateAttributeNode( persistentAttribute );
	}

	private PersistentAttribute<? super J, ?> findAttributeInSupertypes(String attributeName) {
		final PersistentAttribute<? super J, ?> attribute = managedType.findAttributeInSuperTypes( attributeName );
		return attribute instanceof SqmPathSource<?> sqmPathSource && sqmPathSource.isGeneric()
				? managedType.findConcreteGenericAttribute( attributeName )
				: attribute;
	}

	private PersistentAttribute<? super J, ?> getAttribute(String attributeName) {
		final PersistentAttribute<? super J, ?> attribute = managedType.getAttribute( attributeName );
		return attribute instanceof SqmPathSource<?> sqmPathSource && sqmPathSource.isGeneric()
				? managedType.findConcreteGenericAttribute( attributeName )
				: attribute;
	}

	private <AJ> AttributeNodeImplementor<AJ> getNodeForPut(PersistentAttribute<?, AJ> attribute) {
		if ( attributeNodes == null ) {
			attributeNodes = new HashMap<>();
			return null;
		}
		else {
			return getNode( attribute );
		}
	}

	@SuppressWarnings("unchecked")
	private <T> AttributeNodeImplementor<T> getNode(PersistentAttribute<?, ? extends T> attribute) {
		return (AttributeNodeImplementor<T>) attributeNodes.get( attribute );
	}

	@Override
	@SuppressWarnings("unchecked") // The JPA API is unsafe by nature
	public <AJ> SubGraphImplementor<AJ> addSubGraph(String attributeName)  {
		return (SubGraphImplementor<AJ>) findOrCreateAttributeNode( attributeName ).makeSubGraph();
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addSubGraph(String attributeName, Class<AJ> subType) {
		return findOrCreateAttributeNode( attributeName ).makeSubGraph( subType );
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addSubGraph(PersistentAttribute<? super J, AJ> attribute) {
		return findOrCreateAttributeNode( attribute ).makeSubGraph();
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addSubGraph(
			PersistentAttribute<? super J, ? super AJ> attribute, Class<AJ> subType) {
		return findOrCreateAttributeNode( attribute ).makeSubGraph( subType );
	}

	@Override
	public <Y> SubGraphImplementor<Y> addTreatedSubgraph(Attribute<? super J, ? super Y> attribute, Class<Y> type) {
		// TODO Test this it's probably not right!
		final ManagedDomainType<Y> managedDomainType = getGraphedType().getMetamodel().managedType( type );
		return new SubGraphImpl<>( managedDomainType,false );
	}

	@Override
	@SuppressWarnings("unchecked") // The JPA API is unsafe by nature
	public <AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName) {
		return (SubGraphImplementor<AJ>) findOrCreateAttributeNode( attributeName ).makeKeySubGraph();
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName, Class<AJ> subtype) {
		return findOrCreateAttributeNode( attributeName ).makeKeySubGraph( subtype );
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addKeySubGraph(PersistentAttribute<? super J, AJ> attribute) {
		return findOrCreateAttributeNode( attribute ).makeKeySubGraph();
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addKeySubGraph(
			PersistentAttribute<? super J, ? super AJ> attribute, Class<AJ> subType) {
		return findOrCreateAttributeNode( attribute ).makeKeySubGraph( subType );
	}

	////////////////// TODO //////////////////

	@Override
	public <E> SubGraphImplementor<E> addTreatedElementSubgraph(
			PluralAttribute<? super J, ?, ? super E> attribute,
			Class<E> type) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public <K> SubGraphImplementor<K> addTreatedMapKeySubgraph(MapAttribute<? super J, ? super K, ?> attribute, Class<K> type) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public <E> SubGraphImplementor<E> addElementSubgraph(PluralAttribute<? super J, ?, E> attribute) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public <X> SubGraphImplementor<X> addElementSubgraph(String attributeName) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public <X> SubGraphImplementor<X> addElementSubgraph(String attributeName, Class<X> type) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public <K> SubGraphImplementor<K> addMapKeySubgraph(MapAttribute<? super J, K, ?> attribute) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}
}
