/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.internal;

import org.hibernate.graph.spi.GraphNodeImplementor;
import org.hibernate.metamodel.model.domain.JpaMetamodel;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractGraphNode<J> implements GraphNodeImplementor<J> {
	private final JpaMetamodel jpaMetamodel;
	private final boolean mutable;

	public AbstractGraphNode(boolean mutable, JpaMetamodel jpaMetamodel) {
		this.jpaMetamodel = jpaMetamodel;
		this.mutable = mutable;
	}

	protected JpaMetamodel jpaMetamodel() {
		return jpaMetamodel;
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	protected void verifyMutability() {
		if ( !isMutable() ) {
			throw new IllegalStateException( "Cannot mutable immutable graph node" );
		}
	}
}
