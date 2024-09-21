/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal;

import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;

/**
 * Implementation of the JPA-defined {@link jakarta.persistence.Subgraph} interface.
 *
 * @author Steve Ebersole
 */
public class SubGraphImpl<J> extends AbstractGraph<J> implements SubGraphImplementor<J> {

	public SubGraphImpl(ManagedDomainType<J> managedType, boolean mutable) {
		super( managedType, mutable );
	}

	public SubGraphImpl(AbstractGraph<J> original, boolean mutable) {
		super(original, mutable);
	}

	@Override
	public SubGraphImplementor<J> makeCopy(boolean mutable) {
		return new SubGraphImpl<>(this, mutable);
	}

	@Override
	public SubGraphImplementor<J> makeSubGraph(boolean mutable) {
		return !mutable && !isMutable() ? this : makeCopy( true );
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName) {
		return super.addKeySubGraph( attributeName );
	}

	@Override
	public <Y> SubGraphImplementor<Y> addTreatedSubgraph(Attribute<? super J, ? super Y> attribute, Class<Y> type) {
		return null;
	}

	@Override
	public <E> SubGraphImplementor<E> addTreatedElementSubgraph(
			PluralAttribute<? super J, ?, ? super E> attribute,
			Class<E> type) {
		return null;
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
