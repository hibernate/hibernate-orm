/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.spi;

import org.hibernate.graph.GraphNode;

/**
 * Integration version of the {@link GraphNode} contract
 *
 * @author Steve Ebersole
 * @author Strong Liu
 */
public interface GraphNodeImplementor<J> extends GraphNode<J> {
	@Override
	GraphNodeImplementor<J> makeCopy(boolean mutable);
}
