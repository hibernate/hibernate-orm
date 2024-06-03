/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;


import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

public interface AggregateEmbeddableInitializer extends EmbeddableInitializer {

	int[] getAggregateValuesArrayPositions();

	default Object[] getJdbcValues(RowProcessingState processingState) {
		final int[] aggregateValuesArrayPositions = getAggregateValuesArrayPositions();
		Object[] jdbcValue = (Object[]) processingState.getJdbcValue( aggregateValuesArrayPositions[0] );
		for ( int i = 1; i < aggregateValuesArrayPositions.length; i++ ) {
			if ( jdbcValue == null ) {
				break;
			}
			jdbcValue = (Object[]) jdbcValue[aggregateValuesArrayPositions[i]];
		}
		return jdbcValue;
	}

	static int[] determineAggregateValuesArrayPositions(
			InitializerParent parent,
			SqlSelection structSelection) {
		if ( parent instanceof AggregateEmbeddableInitializer ) {
			final int[] parentAggregateValuesArrayPositions = ( (AggregateEmbeddableInitializer) parent ).getAggregateValuesArrayPositions();
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
		else if ( parent instanceof EmbeddableInitializer ) {
			return determineAggregateValuesArrayPositions( parent.getParent(), structSelection );
		}
		return new int[] { structSelection.getValuesArrayPosition() };
	}
}
