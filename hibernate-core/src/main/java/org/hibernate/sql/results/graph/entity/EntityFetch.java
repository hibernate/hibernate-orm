/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity;

import java.util.BitSet;

import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;

/**
 * Specialization of Fetch for entity-valued fetches
 *
 * @author Steve Ebersole
 */
public interface EntityFetch extends EntityResultGraphNode, Fetch {
	@Override
	default boolean containsAnyNonScalarResults() {
		return true;
	}

	@Override
	default FetchParent asFetchParent() {
		return this;
	}

	@Override
	default void collectValueIndexesToCache(BitSet valueIndexes) {
		EntityResultGraphNode.super.collectValueIndexesToCache( valueIndexes );
	}
}
