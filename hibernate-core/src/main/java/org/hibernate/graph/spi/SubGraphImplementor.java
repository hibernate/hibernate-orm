/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.spi;

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
}
