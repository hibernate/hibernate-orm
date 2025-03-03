/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal;

import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

/**
 * Implementation of the JPA-defined {@link jakarta.persistence.Subgraph} interface.
 *
 * @author Steve Ebersole
 */
public class SubGraphImpl<J> extends GraphImpl<J> implements SubGraphImplementor<J> {

	public SubGraphImpl(ManagedDomainType<J> managedType, boolean mutable) {
		super( managedType, mutable );
	}

	public SubGraphImpl(GraphImpl<J> original, boolean mutable) {
		super( original, mutable );
	}

	@Override
	public SubGraphImplementor<J> makeCopy(boolean mutable) {
		return !mutable && !isMutable() ? this : new SubGraphImpl<>( this, mutable );
	}

	@Override @Deprecated(forRemoval = true)
	public SubGraphImplementor<J> makeSubGraph(boolean mutable) {
		return makeCopy( mutable );
	}
}
