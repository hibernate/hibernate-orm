/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal;

import jakarta.persistence.Subgraph;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;

import java.util.HashMap;
import java.util.Map;

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

	private SubGraphImplementor<?> subgraph;
	private SubGraphImplementor<?> keySubgraph;

	public <X> AttributeNodeImpl(PersistentAttribute<X, J> attribute, boolean mutable) {
		this(attribute, mutable, null, null);
	}

	/**
	 * Intended only for use from making a copy
	 */
	private AttributeNodeImpl(
			PersistentAttribute<?, J> attribute, boolean mutable,
			SubGraphImplementor<?> subgraph, SubGraphImplementor<?> keySubgraph) {
		super( mutable );
		this.attribute = attribute;
		this.subgraph = subgraph;
		this.keySubgraph = keySubgraph;
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
	public SubGraphImplementor<?> getSubGraph() {
		return subgraph;
	}

	@Override
	public SubGraphImplementor<?> getKeySubGraph() {
		return keySubgraph;
	}

	private <T> SubGraphImplementor<T> subgraph(ManagedDomainType<T> valueType) {
		if ( subgraph == null ) {
			final SubGraphImplementor<T> graph = new SubGraphImpl<>( valueType, true );
			subgraph = graph;
			return graph;
		}
		else {
			//noinspection unchecked
			return (SubGraphImplementor<T>) subgraph;
		}
	}

	@Override
	public SubGraphImplementor<?> makeSubGraph() {
		verifyMutability();
		return subgraph( (ManagedDomainType<?>) attribute.getValueGraphType() );
	}

	@Override
	public <S> SubGraphImplementor<S> makeSubGraph(Class<S> subtype) {
		verifyMutability();
		final DomainType<?> type = attribute.getValueGraphType();
		if ( !type.getBindableJavaType().isAssignableFrom( subtype ) ) {
			throw new IllegalArgumentException( "Not a subtype: " + subtype.getName() );
		}
		@SuppressWarnings("unchecked")
		final ManagedDomainType<? super S> valueType = (ManagedDomainType<? super S>) type;
		SubGraphImplementor<? super S> subgraph = subgraph( valueType );
		if ( type.getBindableJavaType() == subtype ) {
			//noinspection unchecked
			return (SubGraphImplementor<S>) subgraph;
		}
		else {
			return subgraph.addTreatedSubGraph( subtype );
		}
	}

	@Override
	public <S> SubGraphImplementor<S> makeSubGraph(ManagedDomainType<S> subtype) {
		verifyMutability();
		final DomainType<?> type = attribute.getValueGraphType();
		final Class<S> javaType = subtype.getBindableJavaType();
		if ( !type.getBindableJavaType().isAssignableFrom( javaType ) ) {
			throw new IllegalArgumentException( "Not a subtype: " + javaType.getName() );
		}
		@SuppressWarnings("unchecked")
		final ManagedDomainType<? super S> valueType = (ManagedDomainType<? super S>) type;
		SubGraphImplementor<? super S> subgraph = subgraph( valueType );
		if ( type.getBindableJavaType() == javaType ) {
			//noinspection unchecked
			return (SubGraphImplementor<S>) subgraph;
		}
		else {
			return subgraph.addTreatedSubGraph( subtype );
		}
	}

	private <T> SubGraphImplementor<T> keySubgraph(ManagedDomainType<T> keyType) {
		if ( keySubgraph == null ) {
			final SubGraphImplementor<T> graph = new SubGraphImpl<>( keyType, true );
			keySubgraph = graph;
			return graph;
		}
		else {
			//noinspection unchecked
			return (SubGraphImplementor<T>) keySubgraph;
		}
	}

	@Override
	public SubGraphImplementor<?> makeKeySubGraph() {
		verifyMutability();
		return keySubgraph( (ManagedDomainType<?>) attribute.getKeyGraphType() );
	}

	@Override
	public <S> SubGraphImplementor<S> makeKeySubGraph(Class<S> subtype) {
		verifyMutability();
		final DomainType<?> type = attribute.getKeyGraphType();
		if ( !type.getBindableJavaType().isAssignableFrom( subtype ) ) {
			throw new IllegalArgumentException( "Not a key subtype: " + subtype.getName() );
		}
		@SuppressWarnings("unchecked")
		final ManagedDomainType<? super S> keyType = (ManagedDomainType<? super S>) type;
		final SubGraphImplementor<? super S> keySubgraph = keySubgraph( keyType );
		if ( type.getBindableJavaType() == subtype ) {
			//noinspection unchecked
			return (SubGraphImplementor<S>) keySubgraph;
		}
		else {
			return keySubgraph.addTreatedSubGraph( subtype );
		}
	}

	@Override
	public <S> SubGraphImplementor<S> makeKeySubGraph(ManagedDomainType<S> subtype) {
		verifyMutability();
		final DomainType<?> type = attribute.getKeyGraphType();
		final Class<S> javaType = subtype.getBindableJavaType();
		if ( !type.getBindableJavaType().isAssignableFrom( javaType ) ) {
			throw new IllegalArgumentException( "Not a key subtype: " + javaType.getName() );
		}
		@SuppressWarnings("unchecked")
		final ManagedDomainType<? super S> keyType = (ManagedDomainType<? super S>) type;
		final SubGraphImplementor<? super S> keySubgraph = keySubgraph( keyType );
		if ( type.getBindableJavaType() == javaType ) {
			//noinspection unchecked
			return (SubGraphImplementor<S>) keySubgraph;
		}
		else {
			return keySubgraph.addTreatedSubGraph( subtype );
		}
	}

	@Override
	public AttributeNodeImplementor<J> makeCopy(boolean mutable) {
		return new AttributeNodeImpl<>( this.attribute, mutable,
				subgraph == null ? null : subgraph.makeCopy( mutable ),
				keySubgraph == null ? null : keySubgraph.makeCopy( mutable ) );
	}

	@Override
	public void merge(AttributeNodeImplementor<J> other, boolean mutable) {
		final SubGraphImplementor<?> otherSubgraph = other.getSubGraph();
		if ( otherSubgraph != null ) {
			if ( subgraph == null ) {
				subgraph = otherSubgraph.makeCopy( mutable );
			}
			else {
				subgraph.merge( (SubGraphImplementor) otherSubgraph, mutable );
			}
		}
		final SubGraphImplementor<?> otherKeySubgraph = other.getKeySubGraph();
		if ( otherKeySubgraph != null ) {
			if ( keySubgraph == null ) {
				keySubgraph = otherKeySubgraph.makeCopy( mutable );
			}
			else {
				keySubgraph.merge( (SubGraphImplementor) otherKeySubgraph, mutable );
			}
		}
	}

	@Override
	public Map<Class<?>, SubGraphImplementor<?>> getSubGraphMap() {
		if ( subgraph == null ) {
			return emptyMap();
		}
		else {
			final HashMap<Class<?>, SubGraphImplementor<?>> map = new HashMap<>( subgraph.getSubGraphMap() );
			map.put( attribute.getValueGraphType().getBindableJavaType(), subgraph );
			return map;
		}
	}

	@Override
	public Map<Class<?>, SubGraphImplementor<?>> getKeySubGraphMap() {
		if ( keySubgraph == null ) {
			return emptyMap();
		}
		else {
			final HashMap<Class<?>, SubGraphImplementor<?>> map = new HashMap<>( keySubgraph.getSubGraphMap() );
			map.put( attribute.getKeyGraphType().getJavaType(), keySubgraph );
			return map;
		}
	}

	@Override
	public @SuppressWarnings("rawtypes") Map<Class, Subgraph> getSubgraphs() {
		if ( subgraph == null ) {
			return emptyMap();
		}
		else {
			final HashMap<Class, Subgraph> map = new HashMap<>( subgraph.getSubGraphMap() );
			map.put( attribute.getValueGraphType().getBindableJavaType(), subgraph );
			return map;
		}
	}

	@Override
	public @SuppressWarnings("rawtypes") Map<Class, Subgraph> getKeySubgraphs() {
		if ( keySubgraph == null ) {
			return emptyMap();
		}
		else {
			final HashMap<Class, Subgraph> map = new HashMap<>( keySubgraph.getSubGraphMap() );
			map.put( attribute.getKeyGraphType().getJavaType(), keySubgraph );
			return map;
		}
	}
}
