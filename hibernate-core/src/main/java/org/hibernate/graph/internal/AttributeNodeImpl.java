/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal;

import org.hibernate.graph.CannotContainSubGraphException;
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
public class AttributeNodeImpl<J,V,K>
		extends AbstractGraphNode<J>
		implements AttributeNodeImplementor<J> {
	private final PersistentAttribute<?, J> attribute;

	private SubGraphImplementor<V> valueSubgraph;
	private SubGraphImplementor<K> keySubgraph;

	public <X> AttributeNodeImpl(PersistentAttribute<X, J> attribute, boolean mutable) {
		super( mutable );
		this.attribute = attribute;
	}

	private AttributeNodeImpl(AttributeNodeImpl<J,V,K> that, boolean mutable) {
		super( mutable );
		attribute = that.attribute;
		valueSubgraph = that.valueSubgraph == null ? null : that.valueSubgraph.makeCopy( mutable );
		keySubgraph = that.keySubgraph == null ? null : that.keySubgraph.makeCopy( mutable );
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
	public SubGraphImplementor<V> getSubGraph() {
		return valueSubgraph;
	}

	@Override
	public SubGraphImplementor<K> getKeySubGraph() {
		return keySubgraph;
	}

	@Override
	public SubGraphImplementor<V> makeSubGraph() {
		verifyMutability();
		if ( valueSubgraph == null ) {
			final ManagedDomainType<?> managedType = asManagedType( attribute.getValueGraphType() );
			@SuppressWarnings("unchecked")
			final ManagedDomainType<V> valueGraphType = (ManagedDomainType<V>) managedType;
			final SubGraphImplementor<V> graph = new SubGraphImpl<>( valueGraphType, true );
			valueSubgraph = graph;
			return graph;
		}
		else {
			return valueSubgraph;
		}
	}

	@Override
	public <S> SubGraphImplementor<S> makeSubGraph(Class<S> subtype) {
		final ManagedDomainType<?> managedType = asManagedType( attribute.getValueGraphType() );
		if ( !managedType.getBindableJavaType().isAssignableFrom( subtype ) ) {
			throw new IllegalArgumentException( "Not a subtype: " + subtype.getName() );
		}
		@SuppressWarnings("unchecked")
		final Class<? extends V> castSuptype = (Class<? extends V>) subtype;
		final SubGraphImplementor<? extends V> result = makeSubGraph().addTreatedSubGraph( castSuptype );
		//noinspection unchecked
		return (SubGraphImplementor<S>) result;
	}

	@Override
	public <S> SubGraphImplementor<S> makeSubGraph(ManagedDomainType<S> subtype) {
		final ManagedDomainType<?> managedType = asManagedType( attribute.getValueGraphType() );
		final Class<S> javaType = subtype.getBindableJavaType();
		if ( !managedType.getBindableJavaType().isAssignableFrom( javaType ) ) {
			throw new IllegalArgumentException( "Not a subtype: " + javaType.getName() );
		}
		@SuppressWarnings("unchecked")
		final ManagedDomainType<? extends V> castType = (ManagedDomainType<? extends V>) subtype;
		final SubGraphImplementor<? extends V> result = makeSubGraph().addTreatedSubGraph( castType );
		//noinspection unchecked
		return (SubGraphImplementor<S>) result;
	}

	@Override
	public SubGraphImplementor<K> makeKeySubGraph() {
		verifyMutability();
		if ( keySubgraph == null ) {
			final ManagedDomainType<?> managedType = asManagedType( attribute.getKeyGraphType() );
			@SuppressWarnings("unchecked")
			final ManagedDomainType<K> keyGraphType = (ManagedDomainType<K>) managedType;
			final SubGraphImplementor<K> graph = new SubGraphImpl<>( keyGraphType, true );
			keySubgraph = graph;
			return graph;
		}
		else {
			return keySubgraph;
		}
	}

	@Override
	public <S> SubGraphImplementor<S> makeKeySubGraph(Class<S> subtype) {
		final ManagedDomainType<?> type = asManagedType( attribute.getKeyGraphType() );
		if ( !type.getBindableJavaType().isAssignableFrom( subtype ) ) {
			throw new IllegalArgumentException( "Not a key subtype: " + subtype.getName() );
		}
		@SuppressWarnings("unchecked")
		final Class<? extends K> castType = (Class<? extends K>) subtype;
		final SubGraphImplementor<? extends K> result = makeKeySubGraph().addTreatedSubGraph( castType );
		//noinspection unchecked
		return (SubGraphImplementor<S>) result;
	}

	@Override
	public <S> SubGraphImplementor<S> makeKeySubGraph(ManagedDomainType<S> subtype) {
		final ManagedDomainType<?> type = asManagedType( attribute.getKeyGraphType() );
		final Class<S> javaType = subtype.getBindableJavaType();
		if ( !type.getBindableJavaType().isAssignableFrom( javaType ) ) {
			throw new IllegalArgumentException( "Not a key subtype: " + javaType.getName() );
		}
		@SuppressWarnings("unchecked")
		final ManagedDomainType<? extends K> castType = (ManagedDomainType<? extends K>) subtype;
		final SubGraphImplementor<? extends K> result = makeKeySubGraph().addTreatedSubGraph( castType );
		//noinspection unchecked
		return (SubGraphImplementor<S>) result;
	}

	private static <T> ManagedDomainType<T> asManagedType(DomainType<T> domainType) {
		if ( domainType instanceof ManagedDomainType<T> managedDomainType ) {
			return managedDomainType;
		}
		else {
			throw new CannotContainSubGraphException( "Not a managed domain type: " + domainType.getTypeName() );
		}
	}

	@Override
	public AttributeNodeImplementor<J> makeCopy(boolean mutable) {
		return !mutable && !isMutable() ? this : new AttributeNodeImpl<>( this, mutable );
	}

	@Override
	public void merge(AttributeNodeImplementor<J> other) {
		assert other.isMutable() == isMutable();
		assert other.getAttributeDescriptor() == attribute;
		final AttributeNodeImpl<J, V, K> that = (AttributeNodeImpl<J, V, K>) other;
		final SubGraphImplementor<V> otherValueSubgraph = that.valueSubgraph;
		if ( otherValueSubgraph != null ) {
			if ( valueSubgraph == null ) {
				valueSubgraph = otherValueSubgraph.makeCopy( isMutable() );
			}
			else {
				// even if immutable, we need to merge here
				valueSubgraph.mergeInternal( otherValueSubgraph );
			}
		}
		final SubGraphImplementor<K> otherKeySubgraph = that.keySubgraph;
		if ( otherKeySubgraph != null ) {
			if ( keySubgraph == null ) {
				keySubgraph = otherKeySubgraph.makeCopy( isMutable() );
			}
			else {
				// even if immutable, we need to merge here
				keySubgraph.mergeInternal( otherKeySubgraph );
			}
		}
	}

	@Override
	public Map<Class<?>, SubGraphImplementor<?>> getSubGraphs() {
		if ( valueSubgraph == null ) {
			return emptyMap();
		}
		else {
			final HashMap<Class<?>, SubGraphImplementor<?>> map = new HashMap<>( valueSubgraph.getSubGraphs() );
			map.put( attribute.getValueGraphType().getBindableJavaType(), valueSubgraph );
			return map;
		}
	}

	@Override
	public Map<Class<?>, SubGraphImplementor<?>> getKeySubGraphs() {
		if ( keySubgraph == null ) {
			return emptyMap();
		}
		else {
			final HashMap<Class<?>, SubGraphImplementor<?>> map = new HashMap<>( keySubgraph.getSubGraphs() );
			map.put( attribute.getKeyGraphType().getJavaType(), keySubgraph );
			return map;
		}
	}
}
