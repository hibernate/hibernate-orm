/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.jdbc.internal;

import java.util.BitSet;
import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.ResultsLogger;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * An {@link AbstractJdbcValues} implementation for cases where we had a cache hit.
 *
 * @author Steve Ebersole
 */
public class JdbcValuesCacheHit extends AbstractJdbcValues {
	private List<?> cachedResults;
	private final int numberOfRows;
	private final JdbcValuesMapping resolvedMapping;
	private final int[] valueIndexesToCacheIndexes;
	private final int offset;
	private final int resultCount;
	private int position = -1;

	public JdbcValuesCacheHit(List<?> cachedResults, JdbcValuesMapping resolvedMapping) {
		// See QueryCachePutManagerEnabledImpl for what is being put into the cached results
		this.cachedResults = cachedResults;
		this.offset = !cachedResults.isEmpty() && cachedResults.get( 0 ) instanceof JdbcValuesMetadata ? 1 : 0;
		this.numberOfRows = cachedResults.size() - offset - 1;
		this.resultCount = cachedResults.isEmpty() ? 0 : (int) cachedResults.get( cachedResults.size() - 1 );
		this.resolvedMapping = resolvedMapping;

		final BitSet valueIndexesToCache = new BitSet();
		for ( DomainResult<?> domainResult : resolvedMapping.getDomainResults() ) {
			domainResult.collectValueIndexesToCache( valueIndexesToCache );
		}
		if ( valueIndexesToCache.nextClearBit( 0 ) == -1 ) {
			this.valueIndexesToCacheIndexes = null;
		}
		else {
			final int[] valueIndexesToCacheIndexes = new int[valueIndexesToCache.length()];
			int cacheIndex = 0;
			for ( int i = 0; i < valueIndexesToCacheIndexes.length; i++ ) {
				if ( valueIndexesToCache.get( i ) ) {
					valueIndexesToCacheIndexes[i] = cacheIndex++;
				}
				else {
					valueIndexesToCacheIndexes[i] = -1;
				}
			}

			this.valueIndexesToCacheIndexes = valueIndexesToCacheIndexes;
		}
	}

	@Override
	protected boolean processNext(RowProcessingState rowProcessingState) {
		ResultsLogger.RESULTS_MESSAGE_LOGGER.tracef(
				"JdbcValuesCacheHit#processNext : position = %d; numberOfRows = %d",
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
				"JdbcValuesCacheHit#processPrevious : position = %d; numberOfRows = %d",
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
				"JdbcValuesCacheHit#processScroll(%d) : position = %d; numberOfRows = %d",
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
				"JdbcValuesCacheHit#processPosition(%d) : position = %d; numberOfRows = %d",
				position, this.position, this.numberOfRows
		);

		// NOTE : explicitly skipping limit handling because the cached state should
		// 		already be the limited size since the cache key includes limits

		if ( position < 0 ) {
			// we need to subtract it from `numberOfRows`
			final int newPosition = numberOfRows + position;
			ResultsLogger.RESULTS_MESSAGE_LOGGER.debugf(
					"Translated negative absolute position `%d` into `%d` based on `%d` number of rows",
					position,
					newPosition,
					numberOfRows
			);
			position = newPosition;
		}

		if ( position > numberOfRows ) {
			ResultsLogger.RESULTS_MESSAGE_LOGGER.debugf(
					"Absolute position `%d` exceeded number of rows `%d`",
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
	public boolean usesFollowOnLocking() {
		return true;
	}

	@Override
	public Object getCurrentRowValue(int valueIndex) {
		if ( position >= numberOfRows ) {
			return null;
		}
		final Object row = cachedResults.get( position + offset );
		if ( valueIndexesToCacheIndexes == null ) {
			return ( (Object[]) row )[valueIndex];
		}
		else if ( row.getClass() != Object[].class ) {
			assert valueIndexesToCacheIndexes[valueIndex] == 0;
			return row;
		}
		else {
			return ( (Object[]) row )[valueIndexesToCacheIndexes[valueIndex]];
		}
	}

	@Override
	public void finishUp(SharedSessionContractImplementor session) {
		cachedResults = null;
	}

	@Override
	public void finishRowProcessing(RowProcessingState rowProcessingState, boolean wasAdded) {
		// No-op
	}

	@Override
	public void setFetchSize(int fetchSize) {}

	@Override
	public int getResultCountEstimate() {
		return resultCount;
	}
}
