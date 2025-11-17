/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.spi;

import org.hibernate.graph.CannotBecomeEntityGraphException;
import org.hibernate.graph.SubGraph;

/**
 * Integration version of the {@link SubGraph} contract.
 *
 * @author Steve Ebersole
 *
 * @see RootGraphImplementor
 */
public interface SubGraphImplementor<J> extends SubGraph<J>, GraphImplementor<J> {

	@Override
	SubGraphImplementor<J> makeCopy(boolean mutable);

	@Override @Deprecated(forRemoval = true)
	SubGraphImplementor<J> makeSubGraph(boolean mutable);

	@Override @Deprecated(forRemoval = true)
	RootGraphImplementor<J> makeRootGraph(String name, boolean mutable)
			throws CannotBecomeEntityGraphException;
}
