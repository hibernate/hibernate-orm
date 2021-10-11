/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.tuple;

import org.hibernate.sql.results.ResultsLogger;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Christian Beikov
 */
public class TupleResultAssembler<J> implements DomainResultAssembler<J> {

	private final int[] valuesArrayPositions;
	private final JavaType<J> assembledJavaTypeDescriptor;

	public TupleResultAssembler(
			int[] valuesArrayPositions,
			JavaType<J> assembledJavaTypeDescriptor) {
		this.valuesArrayPositions = valuesArrayPositions;
		this.assembledJavaTypeDescriptor = assembledJavaTypeDescriptor;
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
			RowProcessingState rowProcessingState,
			JdbcValuesSourceProcessingOptions options) {
		final Object[] jdbcValues = extractRawValue( rowProcessingState );

		if ( ResultsLogger.LOGGER.isDebugEnabled() ) {
			for ( int i = 0; i < valuesArrayPositions.length; i++ ) {
				ResultsLogger.LOGGER.debugf(
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
	public JavaType<J> getAssembledJavaTypeDescriptor() {
		return assembledJavaTypeDescriptor;
	}

}
