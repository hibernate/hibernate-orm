/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.embeddable;

import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Specialized EmbeddableResultGraphNode for cases where we have an actual embeddable class.
 */
@org.hibernate.Incubating
public interface AggregateEmbeddableResultGraphNode extends EmbeddableResultGraphNode {
	/**
	 * Returns the positions within the values array of the respective nesting level
	 * at which the data for this aggregate can be found.
	 */
	int[] getAggregateValuesArrayPositions();

	static int[] determineAggregateValuesArrayPositions(@Nullable FetchParent parent, SqlSelection structSelection) {
		if ( parent instanceof AggregateEmbeddableResultGraphNode ) {
			final int[] parentAggregateValuesArrayPositions = ( (AggregateEmbeddableResultGraphNode) parent ).getAggregateValuesArrayPositions();
			final int[] aggregateValuesArrayPositions = new int[parentAggregateValuesArrayPositions.length + 1];
			System.arraycopy(
					parentAggregateValuesArrayPositions,
					0,
					aggregateValuesArrayPositions,
					0,
					parentAggregateValuesArrayPositions.length
			);
			aggregateValuesArrayPositions[aggregateValuesArrayPositions.length - 1] = structSelection.getValuesArrayPosition();
			return aggregateValuesArrayPositions;
		}
		else if ( parent instanceof Fetch && parent instanceof EmbeddableResultGraphNode ) {
			return determineAggregateValuesArrayPositions( ( (Fetch) parent ).getFetchParent(), structSelection );
		}
		return new int[] { structSelection.getValuesArrayPosition() };
	}
}
