/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.jdbc.internal;

import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.sql.results.ResultsLogger;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * An {@link AbstractJdbcValues} implementation for cases where we had a cache hit.
 *
 * @author Steve Ebersole
 */
public class JdbcValuesCacheHit extends AbstractJdbcValues {
	private static final Object[][] NO_DATA = new Object[0][];

	private Object[][] cachedData;
	private final int numberOfRows;
	private final JdbcValuesMapping resolvedMapping;
	private int position = -1;

	public JdbcValuesCacheHit(Object[][] cachedData, JdbcValuesMapping resolvedMapping) {
		this.cachedData = cachedData;
		this.numberOfRows = cachedData.length;
		this.resolvedMapping = resolvedMapping;
	}

	public JdbcValuesCacheHit(List<?> cachedResults, JdbcValuesMapping resolvedMapping) {
		this( extractData( cachedResults ), resolvedMapping );
	}

	private static Object[][] extractData(List<?> cachedResults) {
		if ( CollectionHelper.isEmpty( cachedResults ) ) {
			return NO_DATA;
		}

		final Object[][] data;
		if ( cachedResults.get( 0 ) instanceof JdbcValuesMetadata ) {
			final int end = cachedResults.size() - 1;
			data = new Object[end][];
			for ( int i = 0; i < end; i++ ) {
				final Object[] row = (Object[]) cachedResults.get( i + 1 );
				data[i] = row;
			}
		}
		else {
			data = new Object[cachedResults.size()][];
			for ( int i = 0; i < cachedResults.size(); i++ ) {
				final Object[] row = (Object[]) cachedResults.get( i );
				data[i] = row;
			}
		}

		return data;
	}

	@Override
	protected boolean processNext(RowProcessingState rowProcessingState) {
		ResultsLogger.RESULTS_MESSAGE_LOGGER.tracef(
				"JdbcValuesCacheHit#processNext : position = %i; numberOfRows = %i",
				position,
				numberOfRows
		);

		// NOTE : explicitly skipping limit handling because the cached state ought
		// 		already be the limited size since the cache key includes limits

		position++;

		if ( position >= numberOfRows ) {
			position = numberOfRows;
			return false;
		}

		return true;
	}

	@Override
	protected boolean processPrevious(RowProcessingState rowProcessingState) {
		ResultsLogger.RESULTS_MESSAGE_LOGGER.tracef(
				"JdbcValuesCacheHit#processPrevious : position = %i; numberOfRows = %i",
				position, numberOfRows
		);

		// NOTE : explicitly skipping limit handling because the cached state ought
		// 		already be the limited size since the cache key includes limits

		position--;

		if ( position >= numberOfRows ) {
			position = numberOfRows;
			return false;
		}

		return true;
	}

	@Override
	protected boolean processScroll(int numberOfRows, RowProcessingState rowProcessingState) {
		ResultsLogger.RESULTS_MESSAGE_LOGGER.tracef(
				"JdbcValuesCacheHit#processScroll(%i) : position = %i; numberOfRows = %i",
				numberOfRows, position, this.numberOfRows
		);

		// NOTE : explicitly skipping limit handling because the cached state should
		// 		already be the limited size since the cache key includes limits

		position += numberOfRows;

		if ( position > this.numberOfRows ) {
			position = this.numberOfRows;
			return false;
		}

		return true;
	}

	@Override
	public int getPosition() {
		return position;
	}

	@Override
	protected boolean processPosition(int position, RowProcessingState rowProcessingState) {
		ResultsLogger.RESULTS_MESSAGE_LOGGER.tracef(
				"JdbcValuesCacheHit#processPosition(%i) : position = %i; numberOfRows = %i",
				position, this.position, this.numberOfRows
		);

		// NOTE : explicitly skipping limit handling because the cached state should
		// 		already be the limited size since the cache key includes limits

		if ( position < 0 ) {
			// we need to subtract it from `numberOfRows`
			final int newPosition = numberOfRows + position;
			ResultsLogger.RESULTS_MESSAGE_LOGGER.debugf(
					"Translated negative absolute position `%i` into `%i` based on `%i` number of rows",
					position,
					newPosition,
					numberOfRows
			);
			position = newPosition;
		}

		if ( position > numberOfRows ) {
			ResultsLogger.RESULTS_MESSAGE_LOGGER.debugf(
					"Absolute position `%i` exceeded number of rows `%i`",
					position,
					numberOfRows
			);
			this.position = numberOfRows;
			return false;
		}

		this.position = position;
		return true;
	}

	@Override
	public boolean isBeforeFirst(RowProcessingState rowProcessingState) {
		return position < 0;
	}

	@Override
	public void beforeFirst(RowProcessingState rowProcessingState) {
		position = -1;
	}

	@Override
	public boolean isFirst(RowProcessingState rowProcessingState) {
		return position == 0;
	}

	@Override
	public boolean first(RowProcessingState rowProcessingState) {
		position = 0;
		return numberOfRows > 0;
	}

	@Override
	public boolean isAfterLast(RowProcessingState rowProcessingState) {
		return position >= numberOfRows;
	}

	@Override
	public void afterLast(RowProcessingState rowProcessingState) {
		position = numberOfRows;
	}

	@Override
	public boolean isLast(RowProcessingState rowProcessingState) {
		if ( numberOfRows == 0 ) {
			return position == 0;
		}
		else {
			return position == numberOfRows - 1;
		}
	}

	@Override
	public boolean last(RowProcessingState rowProcessingState) {
		if ( numberOfRows == 0 ) {
			position = 0;
			return false;
		}

		position = numberOfRows - 1;
		return true;
	}

	@Override
	public JdbcValuesMapping getValuesMapping() {
		return resolvedMapping;
	}

	@Override
	public Object[] getCurrentRowValuesArray() {
		if ( position >= numberOfRows ) {
			return null;
		}
		return cachedData[position];
	}

	@Override
	public Object getCurrentRowValue(int valueIndex) {
		if ( position >= numberOfRows ) {
			return null;
		}
		return cachedData[position][valueIndex];
	}

	@Override
	public void finishRowProcessing(RowProcessingState rowProcessingState) {
	}

	@Override
	public void finishUp(SharedSessionContractImplementor session) {
		cachedData = null;
	}

	@Override
	public void setFetchSize(int fetchSize) {}
}
