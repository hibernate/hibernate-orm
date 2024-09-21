/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.hibernate.graph.AttributeNode;
import org.hibernate.graph.CannotBecomeEntityGraphException;
import org.hibernate.graph.CannotContainSubGraphException;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.SubGraph;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.sqm.SqmPathSource;

import jakarta.persistence.metamodel.Attribute;

import static java.util.Collections.emptyList;

/**
 *  Base class for {@link RootGraph} and {@link SubGraph} implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractGraph<J> extends AbstractGraphNode<J> implements GraphImplementor<J> {
	private final ManagedDomainType<J> managedType;
	private Map<PersistentAttribute<?,?>, AttributeNodeImplementor<?>> attrNodeMap;

	public AbstractGraph(ManagedDomainType<J> managedType, boolean mutable) {
		super( mutable );
		this.managedType = managedType;
	}

	protected AbstractGraph(GraphImplementor<J> original, boolean mutable) {
		this( original.getGraphedType(), mutable );
		this.attrNodeMap = new ConcurrentHashMap<>( original.getAttributeNodeList().size() );
		original.visitAttributeNodes(
				node -> attrNodeMap.put(
						node.getAttributeDescriptor(),
						node.makeCopy( mutable )
				)
		);
	}

	@Override
	public ManagedDomainType<J> getGraphedType() {
		return managedType;
	}

	@Override
	public RootGraphImplementor<J> makeRootGraph(String name, boolean mutable) {
		if ( getGraphedType() instanceof EntityDomainType ) {
			return new RootGraphImpl<>( name, this, mutable);
		}

		throw new CannotBecomeEntityGraphException(
				"Cannot transform Graph to RootGraph - " + getGraphedType() + " is not an EntityType"
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void merge(GraphImplementor<? extends J> other) {
		if ( other == null ) {
			return;
		}

		for ( AttributeNodeImplementor<?> attributeNode : other.getAttributeNodeImplementors() ) {
			final AttributeNodeImplementor<?> localAttributeNode = findAttributeNode(
					(PersistentAttribute<? super J,?>) attributeNode.getAttributeDescriptor()
			);
			if ( localAttributeNode != null ) {
				// keep the local one, but merge in the incoming one
				localAttributeNode.merge( attributeNode );
			}
			else {
				addAttributeNode( attributeNode.makeCopy( true ) );
			}
		}

	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// AttributeNode handling


	@Override
	public <Y> AttributeNodeImplementor<Y> getAttributeNode(String attributeName) {
		if ( attrNodeMap == null ) {
			return null;
		}
		final PersistentAttribute<? super J, ?> attribute = managedType.findAttributeInSuperTypes( attributeName );
		//noinspection unchecked
		return (AttributeNodeImplementor<Y>) attrNodeMap.get( attribute );
	}

	@Override
	public <Y> AttributeNodeImplementor<Y> getAttributeNode(Attribute<? super J, Y> attribute) {
		return null;
	}

	@Override
	public void visitAttributeNodes(Consumer<AttributeNodeImplementor<?>> consumer) {
		if ( attrNodeMap == null ) {
			return;
		}
		attrNodeMap.forEach( (persistentAttribute, attributeNodeImplementor) -> {
			consumer.accept( attributeNodeImplementor );
		} );
	}

	@Override
	public List<AttributeNode<?>> getAttributeNodeList() {
		if ( attrNodeMap == null ) {
			return emptyList();
		}
		final List<AttributeNode<?>> result = new ArrayList<>();
		visitAttributeNodes( result::add );
		return result;
	}

	@Override
	public AttributeNodeImplementor<?> addAttributeNode(AttributeNodeImplementor<?> incomingAttributeNode) {
		verifyMutability();

		AttributeNodeImplementor<?> attributeNode = null;
		if ( attrNodeMap == null ) {
			attrNodeMap = new HashMap<>();
		}
		else {
			attributeNode = attrNodeMap.get( incomingAttributeNode.getAttributeDescriptor() );
		}

		if ( attributeNode == null ) {
			attributeNode = incomingAttributeNode;
			attrNodeMap.put( incomingAttributeNode.getAttributeDescriptor(), attributeNode );
		}
		else {
			@SuppressWarnings("rawtypes")
			final AttributeNodeImplementor attributeNodeFinal = attributeNode;
			incomingAttributeNode.visitSubGraphs(
					// we assume the subGraph has been properly copied if needed
					(subType, subGraph) -> attributeNodeFinal.addSubGraph( subGraph )
			);
		}

		return attributeNode;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <AJ> AttributeNodeImplementor<AJ> findAttributeNode(String attributeName) {
		PersistentAttribute<? super J, ?> attribute = managedType.findAttributeInSuperTypes( attributeName );
		if ( attribute instanceof SqmPathSource && ( (SqmPathSource<?>) attribute ).isGeneric() ) {
			attribute = managedType.findConcreteGenericAttribute( attributeName );
		}
		return attribute == null ? null : findAttributeNode( (PersistentAttribute<? super J, AJ>) attribute );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <AJ> AttributeNodeImplementor<AJ> findAttributeNode(PersistentAttribute<? super J, AJ> attribute) {
		return attrNodeMap == null ? null : (AttributeNodeImplementor<AJ>) attrNodeMap.get( attribute );
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public List<AttributeNode<?>> getGraphAttributeNodes() {
		return (List) getAttributeNodeImplementors();
	}

	@Override
	public List<AttributeNodeImplementor<?>> getAttributeNodeImplementors() {
		return attrNodeMap == null ? emptyList() : new ArrayList<>( attrNodeMap.values() );
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ> addAttributeNode(String attributeName) throws CannotContainSubGraphException {
		return findOrCreateAttributeNode( attributeName );
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ> addAttributeNode(PersistentAttribute<? super J, AJ> attribute)
			throws CannotContainSubGraphException {
		return findOrCreateAttributeNode( attribute );
	}

	@Override
	public <Y> AttributeNodeImplementor<Y> addAttributeNode(Attribute<? super J, Y> attribute) {
		//noinspection unchecked
		return (AttributeNodeImplementor<Y>) findOrCreateAttributeNode( (PersistentAttribute<? super J, ?>) attribute );
	}

	@Override
	public void addAttributeNodes(String... attributeNames) {
		for ( int i = 0; i < attributeNames.length; i++ ) {
			addAttributeNode( attributeNames[i] );
		}
	}

	@Override
	public void addAttributeNodes(Attribute<? super J, ?>... attributes) {
		for ( int i = 0; i < attributes.length; i++ ) {
			addAttributeNode( attributes[i] );
		}
	}

	@Override
	public void removeAttributeNode(String attributeName) {
		final PersistentAttribute<? super J, ?> attribute = managedType.findAttribute( attributeName );
		attrNodeMap.remove( attribute );
	}

	@Override
	public void removeAttributeNode(Attribute<? super J, ?> attribute) {
		attrNodeMap.remove( (PersistentAttribute<? super J, ?>) attribute );
	}

	@Override
	public void removeAttributeNodes(Attribute.PersistentAttributeType nodeTypes) {
		attrNodeMap.entrySet().removeIf( entry -> entry.getKey().getPersistentAttributeType() == nodeTypes );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <AJ> AttributeNodeImplementor<AJ> findOrCreateAttributeNode(PersistentAttribute<? super J, AJ> attribute) {
		verifyMutability();

		AttributeNodeImplementor<AJ> attrNode = null;
		if ( attrNodeMap == null ) {
			attrNodeMap = new HashMap<>();
		}
		else {
			attrNode = (AttributeNodeImplementor<AJ>) attrNodeMap.get( attribute );
		}

		if ( attrNode == null ) {
			attrNode = new AttributeNodeImpl<>(attribute, isMutable());
			attrNodeMap.put( attribute, attrNode );
		}

		return attrNode;
	}
}
