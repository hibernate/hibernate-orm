/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal;

import org.hibernate.graph.spi.GraphHelper;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;


/**
 * Implementation of the JPA-defined {@link jakarta.persistence.EntityGraph} interface.
 *
 * @author Steve Ebersole
 */
public class RootGraphImpl<J> extends GraphImpl<J> implements RootGraphImplementor<J> {

	private final String name;

	public RootGraphImpl(String name, EntityDomainType<J> entityType, boolean mutable) {
		super( entityType, mutable );
		this.name = name;
	}

	public RootGraphImpl(String name, EntityDomainType<J> entityType) {
		this( name, entityType, true );
	}

	public RootGraphImpl(String name, GraphImplementor<J> original, boolean mutable) {
		super( original, mutable );
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
		return new RootGraphImpl<>( null, this, mutable );
	}

	@Override @Deprecated(forRemoval = true)
	public SubGraphImplementor<J> makeSubGraph(boolean mutable) {
		return new SubGraphImpl<>( this, mutable );
	}

	@Override @Deprecated(forRemoval = true)
	public RootGraphImplementor<J> makeRootGraph(String name, boolean mutable) {
		return !mutable && !isMutable() ? this : super.makeRootGraph( name, mutable );
	}

	@Override
	public RootGraphImplementor<J> makeImmutableCopy(String name) {
		return makeRootGraph( name, false );
	}
}
