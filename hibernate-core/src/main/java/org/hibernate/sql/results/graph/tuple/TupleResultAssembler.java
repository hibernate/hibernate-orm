/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.tuple;

import org.hibernate.sql.results.ResultsLogger;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Christian Beikov
 */
public class TupleResultAssembler<J> implements DomainResultAssembler<J> {

	private final int[] valuesArrayPositions;
	private final JavaType<J> assembledJavaType;

	public TupleResultAssembler(
			int[] valuesArrayPositions,
			JavaType<J> assembledJavaType) {
		this.valuesArrayPositions = valuesArrayPositions;
		this.assembledJavaType = assembledJavaType;
	}

	public int[] getValuesArrayPositions() {
		return valuesArrayPositions;
	}

	/**
	 * Access to the raw value (unconverted, if a converter applied)
	 */
	public Object[] extractRawValue(RowProcessingState rowProcessingState) {
		final Object[] values = new Object[valuesArrayPositions.length];
		for ( int i = 0; i < valuesArrayPositions.length; i++ ) {
			values[i] = rowProcessingState.getJdbcValue( valuesArrayPositions[i] );
		}
		return values;
	}

	@Override
	public J assemble(
			RowProcessingState rowProcessingState) {
		final Object[] jdbcValues = extractRawValue( rowProcessingState );

		if ( ResultsLogger.RESULTS_MESSAGE_LOGGER.isDebugEnabled() ) {
			for ( int i = 0; i < valuesArrayPositions.length; i++ ) {
				ResultsLogger.RESULTS_MESSAGE_LOGGER.debugf(
						"Extracted JDBC value [%d] - [%s]",
						valuesArrayPositions[i],
						jdbcValues[i]
				);
			}
		}

		//noinspection unchecked
		return (J) jdbcValues;
	}

	@Override
	public JavaType<J> getAssembledJavaType() {
		return assembledJavaType;
	}

}
