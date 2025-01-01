/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal;

import org.hibernate.graph.spi.GraphHelper;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;

import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;

/**
 * Implementation of the JPA-defined {@link jakarta.persistence.EntityGraph} interface.
 *
 * @author Steve Ebersole
 */
public class RootGraphImpl<J> extends AbstractGraph<J> implements RootGraphImplementor<J> {

	private final String name;

	public RootGraphImpl(String name, EntityDomainType<J> entityType, boolean mutable) {
		super( entityType, mutable );
		this.name = name;
	}

	public RootGraphImpl(String name, EntityDomainType<J> entityType) {
		this( name, entityType, true );
	}

	public RootGraphImpl(String name, GraphImplementor<J> original, boolean mutable) {
		super(original, mutable);
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean appliesTo(EntityDomainType<?> entityType) {
		return GraphHelper.appliesTo( this, entityType );
	}

	@Override
	public RootGraphImplementor<J> makeCopy(boolean mutable) {
		return new RootGraphImpl<>( null, this, mutable);
	}

	@Override
	public SubGraphImplementor<J> makeSubGraph(boolean mutable) {
		return new SubGraphImpl<>(this, mutable);
	}

	@Override
	public RootGraphImplementor<J> makeRootGraph(String name, boolean mutable) {
		return !mutable && !isMutable() ? this : super.makeRootGraph( name, mutable );
	}

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
	public <T1> SubGraphImplementor<? extends T1> addSubclassSubgraph(Class<? extends T1> type) {
		return addTreatedSubgraph( (Class) type );
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
