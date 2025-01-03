/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal;

import jakarta.persistence.metamodel.Attribute;
import org.hibernate.graph.CannotContainSubGraphException;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;

import java.util.HashMap;
import java.util.Map;

import static jakarta.persistence.metamodel.Attribute.PersistentAttributeType.EMBEDDED;
import static jakarta.persistence.metamodel.Attribute.PersistentAttributeType.MANY_TO_MANY;
import static jakarta.persistence.metamodel.Attribute.PersistentAttributeType.MANY_TO_ONE;
import static jakarta.persistence.metamodel.Attribute.PersistentAttributeType.ONE_TO_MANY;
import static jakarta.persistence.metamodel.Attribute.PersistentAttributeType.ONE_TO_ONE;
import static java.util.Collections.emptyMap;


/**
 * Implementation of {@link jakarta.persistence.AttributeNode}.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class AttributeNodeImpl<J, E, K>
		extends AbstractGraphNode<J>
		implements AttributeNodeImplementor<J, E, K> {
	private final PersistentAttribute<?, J> attribute;
	private final DomainType<E> valueGraphType;
	private final SimpleDomainType<K> keyGraphType;

	private SubGraphImplementor<E> valueSubgraph;
	private SubGraphImplementor<K> keySubgraph;

	static <X,J> AttributeNodeImpl<J,?,?> create(PersistentAttribute<X, J> attribute, boolean mutable) {
		return new AttributeNodeImpl<>( attribute, mutable, attribute.getValueGraphType(), attribute.getKeyGraphType() );
	}

	static <X,J,E> AttributeNodeImpl<J,E,?> create(PluralPersistentAttribute<X, J, E> attribute, boolean mutable) {
		return new AttributeNodeImpl<>( attribute, mutable, attribute.getValueGraphType(), attribute.getKeyGraphType() );
	}

	static <X,K,V> AttributeNodeImpl<Map<K,V>,V,K> create(MapPersistentAttribute<X, K, V> attribute, boolean mutable) {
		return new AttributeNodeImpl<>( attribute, mutable, attribute.getValueGraphType(), attribute.getKeyGraphType() );
	}

	private <X> AttributeNodeImpl(
			PersistentAttribute<X, J> attribute, boolean mutable,
			DomainType<E> valueGraphType, SimpleDomainType<K> keyGraphType) {
		super( mutable );
		this.attribute = attribute;
		this.valueGraphType = valueGraphType;
		this.keyGraphType = keyGraphType;
	}

	private AttributeNodeImpl(AttributeNodeImpl<J, E,K> that, boolean mutable) {
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
	public SubGraphImplementor<E> addValueSubgraph() {
		// this one is intentionally lenient and disfavored
		if ( valueSubgraph == null ) {
			valueSubgraph = new SubGraphImpl<>( asManagedType( valueGraphType ), true );
		}
		return valueSubgraph;
	}

	@Override
	public SubGraphImplementor<J> addSingularSubgraph() {
		checkToOne();
		if ( valueSubgraph == null ) {
			valueSubgraph = new SubGraphImpl<>( asManagedType( valueGraphType ), true );
		}
		// Safe cast, in this case E = J
		// TODO: would be more elegant to separate singularSubgraph vs elementSubgraph fields
		//noinspection unchecked
		return (SubGraphImplementor<J>) valueSubgraph;
	}

	@Override
	public SubGraphImplementor<E> addElementSubgraph() {
		checkToMany();
		if ( valueSubgraph == null ) {
			valueSubgraph = new SubGraphImpl<>( asManagedType( valueGraphType ), true );
		}
		return valueSubgraph;
	}

	@Override
	public SubGraphImplementor<K> addKeySubgraph() {
		checkMap();
		if ( keySubgraph == null ) {
			keySubgraph = new SubGraphImpl<>( asManagedType( keyGraphType ), true );
		}
		return keySubgraph;
	}

	private void checkToOne() {
		final Attribute.PersistentAttributeType attributeType = attribute.getPersistentAttributeType();
		if ( attributeType != MANY_TO_ONE && attributeType != ONE_TO_ONE && attributeType != EMBEDDED ) {
			throw new CannotContainSubGraphException( "Attribute '" + attribute.getName() + "' is not a to-one association" );
		}
	}

	private void checkToMany() {
		final Attribute.PersistentAttributeType attributeType = attribute.getPersistentAttributeType();
		if ( attributeType != MANY_TO_MANY && attributeType != ONE_TO_MANY ) {
			throw new CannotContainSubGraphException( "Attribute '" + attribute.getName() + "' is not a to-many association" );
		}
	}

	@Override @Deprecated
	public SubGraphImplementor<E> makeSubGraph() {
		verifyMutability();
		if ( valueSubgraph == null ) {
			valueSubgraph = new SubGraphImpl<>( asManagedType( valueGraphType ), true );
		}
		return valueSubgraph;
	}

	@Override @Deprecated
	public <S> SubGraphImplementor<S> makeSubGraph(Class<S> subtype) {
		final ManagedDomainType<E> managedType = asManagedType( valueGraphType );
		if ( !managedType.getBindableJavaType().isAssignableFrom( subtype ) ) {
			throw new IllegalArgumentException( "Not a subtype: " + subtype.getName() );
		}
		@SuppressWarnings("unchecked")
		final Class<? extends E> castSuptype = (Class<? extends E>) subtype;
		final SubGraphImplementor<? extends E> result = makeSubGraph().addTreatedSubgraph( castSuptype );
		//noinspection unchecked
		return (SubGraphImplementor<S>) result;
	}

	@Override @Deprecated
	public SubGraphImplementor<K> makeKeySubGraph() {
		verifyMutability();
		checkMap();
		if ( keySubgraph == null ) {
			keySubgraph = new SubGraphImpl<>( asManagedType( keyGraphType ), true );
		}
		return keySubgraph;
	}

	@Override @Deprecated
	public <S> SubGraphImplementor<S> makeKeySubGraph(Class<S> subtype) {
		checkMap();
		final ManagedDomainType<K> type = asManagedType( keyGraphType );
		if ( !type.getBindableJavaType().isAssignableFrom( subtype ) ) {
			throw new IllegalArgumentException( "Not a key subtype: " + subtype.getName() );
		}
		@SuppressWarnings("unchecked")
		final Class<? extends K> castType = (Class<? extends K>) subtype;
		final SubGraphImplementor<? extends K> result = makeKeySubGraph().addTreatedSubgraph( castType );
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
	public AttributeNodeImplementor<J, E, K> makeCopy(boolean mutable) {
		return !mutable && !isMutable() ? this : new AttributeNodeImpl<>( this, mutable );
	}

	@Override
	public void merge(AttributeNodeImplementor<J, E, K> other) {
		assert other.isMutable() == isMutable();
		assert other.getAttributeDescriptor() == attribute;
		final AttributeNodeImpl<J, E, K> that = (AttributeNodeImpl<J, E, K>) other;
		final SubGraphImplementor<E> otherValueSubgraph = that.valueSubgraph;
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
			final HashMap<Class<?>, SubGraphImplementor<?>> map = new HashMap<>( valueSubgraph.getTreatedSubgraphs() );
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
			final HashMap<Class<?>, SubGraphImplementor<?>> map = new HashMap<>( keySubgraph.getTreatedSubgraphs() );
			map.put( attribute.getKeyGraphType().getJavaType(), keySubgraph );
			return map;
		}
	}
}
