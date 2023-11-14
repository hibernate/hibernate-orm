/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
