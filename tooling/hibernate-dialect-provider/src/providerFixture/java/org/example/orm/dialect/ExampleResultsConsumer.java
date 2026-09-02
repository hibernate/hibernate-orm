/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.sql.results.spi.ResultsConsumer;
import org.hibernate.sql.results.spi.RowReader;

/// External result consumer which navigates rows through the supported state
/// contract and never names Hibernate's standard state implementation.
///
/// @author Steve Ebersole
/// @since 8.0
public final class ExampleResultsConsumer implements ResultsConsumer<Integer, Object> {
	@Override
	public Integer consume(
			JdbcValues jdbcValues,
			SharedSessionContractImplementor session,
			JdbcValuesSourceProcessingOptions processingOptions,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			RowProcessingState rowProcessingState,
			RowReader<Object> rowReader) {
		int rowCount = 0;
		while ( rowProcessingState.next() ) {
			rowReader.readRow( rowProcessingState );
			rowProcessingState.finishRowProcessing( true );
			rowCount++;
		}
		return rowCount;
	}

	@Override
	public boolean canResultsBeCached() {
		return false;
	}
}
