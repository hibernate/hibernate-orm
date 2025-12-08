/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal;

import jakarta.persistence.metamodel.Attribute;
import org.hibernate.AssertionFailure;
import org.hibernate.graph.CannotContainSubGraphException;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;

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
public abstract sealed class AttributeNodeImpl<J, E, K>
		extends AbstractGraphNode<J>
		implements AttributeNodeImplementor<J, E, K>
		permits AttributeNodeImpl.SingularAttributeNodeImpl,
				AttributeNodeImpl.PluralAttributeNodeImpl,
				AttributeNodeImpl.MapAttributeNodeImpl {

	protected final PersistentAttribute<?, J> attribute;
	protected final DomainType<E> valueGraphType;
	protected final SimpleDomainType<K> keyGraphType;

	protected SubGraphImplementor<E> valueSubgraph;
	protected SubGraphImplementor<K> keySubgraph;

	static <J> AttributeNodeImpl<J,?,?> create(
			PersistentAttribute<?, J> attribute, boolean mutable) {
		if ( attribute instanceof PluralPersistentAttribute<?, J, ?> pluralAttribute ) {
			return create( pluralAttribute, mutable );
		}
		else if ( attribute instanceof SingularPersistentAttribute<?, J> singularAttribute ) {
			return new SingularAttributeNodeImpl<>( singularAttribute, mutable,
					singularAttribute.getValueGraphType() );
		}
		else {
			throw new AssertionFailure( "Unrecognized attribute type: " + attribute );
		}
	}

	static <J,E> AttributeNodeImpl<J,E,?> create(
			PluralPersistentAttribute<?, J, E> attribute, boolean mutable) {
		if ( attribute instanceof MapPersistentAttribute<?, ?, ?> mapAttribute ) {
			return create( attribute, mapAttribute, mutable );
		}
		else {
			return new PluralAttributeNodeImpl<>( attribute, mutable,
					attribute.getValueGraphType() );
		}
	}

	static <K,V> AttributeNodeImpl<Map<K,V>,V,K> create(
			MapPersistentAttribute<?, K, V> attribute, boolean mutable) {
		return new MapAttributeNodeImpl<>( attribute, attribute, mutable,
				attribute.getValueGraphType(), attribute.getKeyGraphType() );
	}

	private static <J,K,V> AttributeNodeImpl<J,V,K> create(
			PluralPersistentAttribute<?, J, V> plural, MapPersistentAttribute<?, K, ?> attribute, boolean mutable) {
		return new MapAttributeNodeImpl<>( plural, attribute, mutable,
				plural.getValueGraphType(), attribute.getKeyGraphType() );
	}

	AttributeNodeImpl(
			PersistentAttribute<?, J> attribute, boolean mutable,
			DomainType<E> valueGraphType, SimpleDomainType<K> keyGraphType) {
		super( mutable );
		this.attribute = attribute;
		this.valueGraphType = valueGraphType;
		this.keyGraphType = keyGraphType;
	}

	private AttributeNodeImpl(AttributeNodeImpl<J, E, K> that, boolean mutable) {
		super( mutable );
		attribute = that.attribute;
		valueGraphType = that.valueGraphType;
		keyGraphType = that.keyGraphType;
		valueSubgraph = that.valueSubgraph == null ? null : that.valueSubgraph.makeCopy( mutable );
		keySubgraph = that.keySubgraph == null ? null : that.keySubgraph.makeCopy( mutable );
	}

	static final class SingularAttributeNodeImpl<J> extends AttributeNodeImpl<J, J, Void> {
		private SingularAttributeNodeImpl(
				SingularPersistentAttribute<?,J> attribute,
				boolean mutable,
				DomainType<J> valueGraphType) {
			super( attribute, mutable, valueGraphType, null );
		}

		private SingularAttributeNodeImpl(AttributeNodeImpl<J, J, Void> that, boolean mutable) {
			super( that, mutable );
		}

		@Override
		public SubGraphImplementor<J> addSingularSubgraph() {
			checkToOne();
			verifyMutability();
			if ( valueSubgraph == null ) {
				valueSubgraph = new SubGraphImpl<>( asManagedType( valueGraphType ), true );
			}
			return valueSubgraph;
		}

		@Override
		public AttributeNodeImplementor<J, J, Void> makeCopy(boolean mutable) {
			return !mutable && !isMutable() ? this : new SingularAttributeNodeImpl<>( this, mutable );
		}
	}

	static final class PluralAttributeNodeImpl<J,E> extends AttributeNodeImpl<J, E, Void> {
		private PluralAttributeNodeImpl(
				PluralPersistentAttribute<?,J,E> attribute,
				boolean mutable,
				DomainType<E> valueGraphType) {
			super( attribute, mutable, valueGraphType, null );
		}

		private PluralAttributeNodeImpl(AttributeNodeImpl<J, E, Void> that, boolean mutable) {
			super( that, mutable );
		}

		@Override
		public SubGraphImplementor<E> addElementSubgraph() {
			checkToMany();
			verifyMutability();
			if ( valueSubgraph == null ) {
				valueSubgraph = new SubGraphImpl<>( asManagedType( valueGraphType ), true );
			}
			return valueSubgraph;
		}

		@Override
		public AttributeNodeImplementor<J, E, Void> makeCopy(boolean mutable) {
			return !mutable && !isMutable() ? this : new PluralAttributeNodeImpl<>( this, mutable );
		}
	}

	static final class MapAttributeNodeImpl<J,K,V> extends AttributeNodeImpl<J, V, K> {
		private MapAttributeNodeImpl(
				PluralPersistentAttribute<?,J,V> pluralAttribute,
				@SuppressWarnings("unused") // a "witness" that this is really a Map
				MapPersistentAttribute<?,K,?> attribute,
				boolean mutable,
				DomainType<V> valueGraphType, SimpleDomainType<K> keyGraphType) {
			super( pluralAttribute, mutable, valueGraphType, keyGraphType );
		}

		private MapAttributeNodeImpl(AttributeNodeImpl<J, V, K> that, boolean mutable) {
			super( that, mutable );
		}

		@Override
		public SubGraphImplementor<K> addKeySubgraph() {
			verifyMutability();
			if ( keySubgraph == null ) {
				keySubgraph = new SubGraphImpl<>( asManagedType( keyGraphType ), true );
			}
			return keySubgraph;
		}

		@Override
		public SubGraphImplementor<V> addElementSubgraph() {
			checkToMany();
			verifyMutability();
			if ( valueSubgraph == null ) {
				valueSubgraph = new SubGraphImpl<>( asManagedType( valueGraphType ), true );
			}
			return valueSubgraph;
		}

		@Override
		public AttributeNodeImplementor<J, V, K> makeCopy(boolean mutable) {
			return !mutable && !isMutable() ? this : new MapAttributeNodeImpl<>( this, mutable );
		}
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
		verifyMutability();
		// this one is intentionally lenient and disfavored
		if ( valueSubgraph == null ) {
			valueSubgraph = new SubGraphImpl<>( asManagedType( valueGraphType ), true );
		}
		return valueSubgraph;
	}

	@Override
	public SubGraphImplementor<J> addSingularSubgraph() {
		throw new UnsupportedOperationException("Not a singular attribute node");
	}

	@Override
	public SubGraphImplementor<E> addElementSubgraph() {
		throw new UnsupportedOperationException( "Not a collection-valued attribute node" );
	}

	@Override
	public SubGraphImplementor<K> addKeySubgraph() {
		throw new UnsupportedOperationException( "Not a Map-valued attribute node" );
	}

	protected void checkToOne() {
		final Attribute.PersistentAttributeType attributeType = attribute.getPersistentAttributeType();
		if ( attributeType != MANY_TO_ONE && attributeType != ONE_TO_ONE && attributeType != EMBEDDED ) {
			throw new CannotContainSubGraphException( "Attribute '" + attribute.getName() + "' is not a to-one association" );
		}
	}

	protected void checkToMany() {
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
		final var managedType = asManagedType( valueGraphType );
		if ( !managedType.getJavaType().isAssignableFrom( subtype ) ) {
			throw new IllegalArgumentException( "Not a subtype: " + subtype.getName() );
		}
		@SuppressWarnings("unchecked")
		final var castSuptype = (Class<? extends E>) subtype;
		final var result = makeSubGraph().addTreatedSubgraph( castSuptype );
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
		final var type = asManagedType( keyGraphType );
		if ( !type.getJavaType().isAssignableFrom( subtype ) ) {
			throw new IllegalArgumentException( "Not a key subtype: " + subtype.getName() );
		}
		@SuppressWarnings("unchecked")
		final var castType = (Class<? extends K>) subtype;
		final var result = makeKeySubGraph().addTreatedSubgraph( castType );
		//noinspection unchecked
		return (SubGraphImplementor<S>) result;
	}

	private void checkMap() {
		if ( keyGraphType == null ) {
			throw new CannotContainSubGraphException( "Attribute '" + description() + "' is not a Map" );
		}
	}

	protected <T> ManagedDomainType<T> asManagedType(DomainType<T> domainType) {
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
	public void merge(AttributeNodeImplementor<J, E, K> that) {
		assert that.isMutable() == isMutable();
		assert that.getAttributeDescriptor() == attribute;
		final var otherValueSubgraph = that.getValueSubgraph();
		if ( otherValueSubgraph != null ) {
			if ( valueSubgraph == null ) {
				valueSubgraph = otherValueSubgraph.makeCopy( isMutable() );
			}
			else {
				// even if immutable, we need to merge here
				valueSubgraph.mergeInternal( otherValueSubgraph );
			}
		}
		final var otherKeySubgraph = that.getKeySubgraph();
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
			final HashMap<Class<?>, SubGraphImplementor<?>> map =
					new HashMap<>( valueSubgraph.getTreatedSubgraphs() );
			map.put( attribute.getValueGraphType().getJavaType(), valueSubgraph );
			return map;
		}
	}

	@Override
	public Map<Class<?>, SubGraphImplementor<?>> getKeySubGraphs() {
		if ( keySubgraph == null ) {
			return emptyMap();
		}
		else {
			final HashMap<Class<?>, SubGraphImplementor<?>> map =
					new HashMap<>( keySubgraph.getTreatedSubgraphs() );
			map.put( attribute.getKeyGraphType().getJavaType(), keySubgraph );
			return map;
		}
	}

	@Override
	public SubGraphImplementor<E> getValueSubgraph() {
		return valueSubgraph;
	}

	@Override
	public SubGraphImplementor<K> getKeySubgraph() {
		return keySubgraph;
	}
}
