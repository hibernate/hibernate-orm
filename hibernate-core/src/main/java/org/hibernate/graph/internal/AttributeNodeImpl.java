/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;

import static java.util.Collections.emptyMap;

/**
 * Implementation of {@link jakarta.persistence.AttributeNode}.
 *
 * @author Steve Ebersole
 */
public class AttributeNodeImpl<J>
		extends AbstractGraphNode<J>
		implements AttributeNodeImplementor<J> {
	private final PersistentAttribute<?, J> attribute;

	private Map<Class<?>, SubGraphImplementor<?>> subgraphMap;
	private Map<Class<?>, SubGraphImplementor<?>> keySubgraphMap;

	public <X> AttributeNodeImpl(PersistentAttribute<X, J> attribute, boolean mutable) {
		this(attribute, null, null, mutable);
	}

	/**
	 * Intended only for use from making a copy
	 */
	private AttributeNodeImpl(
			PersistentAttribute<?, J> attribute,
			Map<Class<?>, SubGraphImplementor<?>> subgraphMap,
			Map<Class<?>, SubGraphImplementor<?>> keySubgraphMap,
			boolean mutable) {
		super( mutable );
		this.attribute = attribute;
		this.subgraphMap = subgraphMap;
		this.keySubgraphMap = keySubgraphMap;
	}

	@Override
	public String getAttributeName() {
		return getAttributeDescriptor().getName();
	}

	@Override
	public PersistentAttribute<?, J> getAttributeDescriptor() {
		return attribute;
	}

	@Override
	public Map<Class<?>, SubGraphImplementor<?>> getSubGraphMap() {
		return subgraphMap == null ? emptyMap() : subgraphMap;
	}

	@Override
	public Map<Class<?>, SubGraphImplementor<?>> getKeySubGraphMap() {
		return keySubgraphMap == null ? emptyMap() : keySubgraphMap;
	}

	@Override
	public SubGraphImplementor<?> makeSubGraph() {
		return makeSubGraph( (ManagedDomainType<?>) attribute.getValueGraphType() );
	}

	@Override
	public <S> SubGraphImplementor<S> makeSubGraph(Class<S> type) {
		return makeSubGraph( attribute.getDeclaringType().getMetamodel().managedType( type ) );
	}

	@Override
	public <S> SubGraphImplementor<S> makeSubGraph(ManagedDomainType<S> subtype) {
		verifyMutability();
		assert subtype != null;
		if ( !attribute.getValueGraphType().getBindableJavaType().isAssignableFrom( subtype.getJavaType() ) ) {
			throw new IllegalArgumentException( "Not a subtype: " + subtype.getJavaType() );
		}
		final SubGraphImplementor<S> subGraph = new SubGraphImpl<>( subtype, true );
		addSubGraph( subGraph );
		return subGraph;
	}

	@Override
	public void addSubGraph(SubGraphImplementor<?> subgraph) {
		if ( subgraphMap == null ) {
			subgraphMap = new HashMap<>();
			subgraphMap.put( subgraph.getClassType(), subgraph );
		}
		else {
			final SubGraphImplementor<?> existing = subgraphMap.get( subgraph.getClassType() );
			if ( existing == null ) {
				subgraphMap.put( subgraph.getClassType(), subgraph );
			}
			else {
				existing.merge( (SubGraphImplementor) subgraph );
			}
		}
	}

	@Override
	public void addKeySubGraph(SubGraphImplementor<?> subgraph) {
		if ( keySubgraphMap == null ) {
			keySubgraphMap = new HashMap<>();
			keySubgraphMap.put( subgraph.getClassType(), subgraph );
		}
		else {
			final SubGraphImplementor<?> existing = keySubgraphMap.get( subgraph.getClassType() );
			if ( existing == null ) {
				keySubgraphMap.put( subgraph.getClassType(), subgraph );
			}
			else {
				existing.merge( (SubGraphImplementor) subgraph );
			}
		}
	}

	@Override
	public SubGraphImplementor<?> makeKeySubGraph() {
		return makeKeySubGraph( (ManagedDomainType<?>) attribute.getKeyGraphType() );
	}

	@Override
	public <S> SubGraphImplementor<S> makeKeySubGraph(Class<S> type) {
		return makeKeySubGraph( attribute.getDeclaringType().getMetamodel().managedType( type ) );
	}

	@Override
	public <S> SubGraphImplementor<S> makeKeySubGraph(ManagedDomainType<S> subtype) {
		verifyMutability();
		assert subtype != null;
		if ( !attribute.getKeyGraphType().getBindableJavaType().isAssignableFrom( subtype.getJavaType() ) ) {
			throw new IllegalArgumentException( "Not a key subtype: " + subtype.getJavaType() );
		}
		final SubGraphImplementor<S> subgraph = new SubGraphImpl<>( subtype, true );
		addKeySubGraph( subgraph );
		return subgraph;
	}

	@Override
	public AttributeNodeImplementor<J> makeCopy(boolean mutable) {
		return new AttributeNodeImpl<>(
				this.attribute,
				makeMapCopy( mutable, subgraphMap ),
				makeMapCopy( mutable, keySubgraphMap ),
				mutable
		);
	}

	private <U,V> Map<Class<? extends U>, SubGraphImplementor<? extends V>> makeMapCopy(
			boolean mutable,
			Map<Class<? extends U>, SubGraphImplementor<? extends V>> nodeMap) {
		if ( nodeMap == null ) {
			return null;
		}
		else {
			final HashMap<Class<? extends U>, SubGraphImplementor<? extends V>> map = new HashMap<>( nodeMap.size() );
			nodeMap.forEach( (attribute, subgraph) -> map.put( attribute, subgraph.makeCopy( mutable ) ) );
			return map;
		}
	}

	@Override
	public void merge(AttributeNodeImplementor<J> other) {
		other.getSubGraphMap().values().forEach( this::mergeToSubgraph );
		other.getKeySubGraphMap().values().forEach( this::mergeToKeySubgraph );
	}

	private <T> void mergeToKeySubgraph(SubGraphImplementor<T> subgraph) {
		final SubGraphImplementor<T> existing = getKeySubgraphForPut( subgraph );
		if ( existing != null ) {
			existing.merge( subgraph );
		}
		else {
			addKeySubGraph( subgraph.makeCopy( true ) );
		}
	}

	private <T> void mergeToSubgraph(SubGraphImplementor<T> subgraph) {
		final SubGraphImplementor<T> existing = getSubgraphForPut( subgraph );
		if ( existing != null ) {
			existing.merge( subgraph );
		}
		else {
			addSubGraph( subgraph.makeCopy( true ) );
		}
	}

	private <T> SubGraphImplementor<T> getSubgraphForPut(SubGraphImplementor<T> subgraph) {
		if ( subgraphMap == null ) {
			subgraphMap = new HashMap<>();
			return null;
		}
		else {
			return getSubgraph( subgraph.getClassType() );
		}
	}

	private <T> SubGraphImplementor<T> getKeySubgraphForPut(SubGraphImplementor<T> subgraph) {
		if ( keySubgraphMap == null ) {
			keySubgraphMap = new HashMap<>();
			return null;
		}
		else {
			return getKeySubgraph( subgraph.getClassType() );
		}
	}

	@SuppressWarnings("unchecked")
	private <T> SubGraphImplementor<T> getSubgraph(Class<T> incomingSubtype) {
		return (SubGraphImplementor<T>) subgraphMap.get( incomingSubtype );
	}

	@SuppressWarnings("unchecked")
	private <T> SubGraphImplementor<T> getKeySubgraph(Class<T> incomingSubtype) {
		return (SubGraphImplementor<T>) keySubgraphMap.get( incomingSubtype );
	}
}
