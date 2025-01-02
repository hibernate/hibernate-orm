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
import org.hibernate.metamodel.model.domain.SimpleDomainType;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;


/**
 * Implementation of {@link jakarta.persistence.AttributeNode}.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class AttributeNodeImpl<J,V,K>
		extends AbstractGraphNode<J>
		implements AttributeNodeImplementor<J> {
	private final PersistentAttribute<?, J> attribute;
	private final DomainType<V> valueGraphType;
	private final SimpleDomainType<K> keyGraphType;

	private SubGraphImplementor<V> valueSubgraph;
	private SubGraphImplementor<K> keySubgraph;

	static <X,J> AttributeNodeImpl<J,?,?> create(PersistentAttribute<X, J> attribute, boolean mutable) {
		return new AttributeNodeImpl<>( attribute, mutable, attribute.getValueGraphType(), attribute.getKeyGraphType() );
	}

	private <X> AttributeNodeImpl(
			PersistentAttribute<X, J> attribute, boolean mutable,
			DomainType<V> valueGraphType, SimpleDomainType<K> keyGraphType) {
		super( mutable );
		this.attribute = attribute;
		this.valueGraphType = valueGraphType;
		this.keyGraphType = keyGraphType;
	}

	private AttributeNodeImpl(AttributeNodeImpl<J,V,K> that, boolean mutable) {
		super( mutable );
		attribute = that.attribute;
		valueGraphType = that.valueGraphType;
		keyGraphType = that.keyGraphType;
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
			valueSubgraph = new SubGraphImpl<>( asManagedType( valueGraphType ), true );
		}
		return valueSubgraph;
	}

	@Override
	public <S> SubGraphImplementor<S> makeSubGraph(Class<S> subtype) {
		final ManagedDomainType<V> managedType = asManagedType( valueGraphType );
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
		final ManagedDomainType<V> managedType = asManagedType( valueGraphType );
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
		checkMap();
		if ( keySubgraph == null ) {
			keySubgraph = new SubGraphImpl<>( asManagedType( keyGraphType ), true );
		}
		return keySubgraph;
	}

	@Override
	public <S> SubGraphImplementor<S> makeKeySubGraph(Class<S> subtype) {
		checkMap();
		final ManagedDomainType<K> type = asManagedType( keyGraphType );
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
		checkMap();
		final ManagedDomainType<K> type = asManagedType( keyGraphType );
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

	private void checkMap() {
		if ( keyGraphType == null ) {
			throw new CannotContainSubGraphException( "Attribute '" + description() + "' is not a Map" );
		}
	}

	private <T> ManagedDomainType<T> asManagedType(DomainType<T> domainType) {
		if ( domainType instanceof ManagedDomainType<T> managedDomainType ) {
			return managedDomainType;
		}
		else {
			throw new CannotContainSubGraphException( "Attribute '" + description()
					+ "' is of type '" + domainType.getTypeName()
					+ "' which is not a managed type" );
		}
	}

	private String description() {
		return attribute.getDeclaringType().getTypeName() + "." + attribute.getName();
	}

	@Override
	public String toString() {
		return "AttributeNode[" + description() + "]";
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
