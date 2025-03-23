/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.embeddable.AggregateEmbeddableResultGraphNode;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * An initializer for an embeddable that is mapped as aggregate e.g. STRUCT, JSON or XML.
 * The aggregate selection reads an Object[] from JDBC which serves as data for the nested {@link DomainResultAssembler}.
 * This class exposes the Object[] of the aggregate to the nested assemblers through a wrapping {@link RowProcessingState}.
 */
public class AggregateEmbeddableInitializerImpl extends EmbeddableInitializerImpl {

	private final int[] aggregateValuesArrayPositions;

	public AggregateEmbeddableInitializerImpl(
			AggregateEmbeddableResultGraphNode resultDescriptor,
			BasicFetch<?> discriminatorFetch,
			InitializerParent<?> parent,
			AssemblerCreationState creationState,
			boolean isResultInitializer) {
		super( resultDescriptor, discriminatorFetch, parent, creationState, isResultInitializer );
		this.aggregateValuesArrayPositions = resultDescriptor.getAggregateValuesArrayPositions();
	}

	@Override
	public void startLoading(RowProcessingState rowProcessingState) {
		super.startLoading( NestedRowProcessingState.wrap( this, rowProcessingState ) );
	}

	public int[] getAggregateValuesArrayPositions() {
		return aggregateValuesArrayPositions;
	}

	public Object[] getJdbcValues(RowProcessingState processingState) {
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

}
