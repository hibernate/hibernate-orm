/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
