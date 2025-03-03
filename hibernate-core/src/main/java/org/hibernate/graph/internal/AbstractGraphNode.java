/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal;

import org.hibernate.graph.spi.GraphNodeImplementor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractGraphNode<J> implements GraphNodeImplementor<J> {

	private final boolean mutable;

	public AbstractGraphNode(boolean mutable) {
		this.mutable = mutable;
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	protected void verifyMutability() {
		if ( !isMutable() ) {
			throw new IllegalStateException( "Cannot mutate immutable graph node" );
		}
	}
}
