/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.spi;

import org.hibernate.graph.GraphSemantic;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Contract for anything a fetch/load graph can be applied
 *
 * @author Steve Ebersole
 */
public interface AppliedGraph {
	/**
	 * The applied graph
	 */
	@Nullable RootGraphImplementor<?> getGraph();

	/**
	 * The semantic (fetch/load) under which the graph should be applied
	 */
	@Nullable GraphSemantic getSemantic();
}
