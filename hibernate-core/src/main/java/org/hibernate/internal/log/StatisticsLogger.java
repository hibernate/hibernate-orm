/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.log;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;

@MessageLogger( projectCode = "HHH" )
@SubSystemLogging(
		name = StatisticsLogger.LOGGER_NAME,
		description = "Logging related to statistics"
)
public interface StatisticsLogger extends BasicLogger {
	String LOGGER_NAME = "org.hibernate.statistics";

	StatisticsLogger STATISTICS_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), StatisticsLogger.class, LOGGER_NAME );

	@LogMessage(level = TRACE)
	@Message(value = "Statistics initialized", id = 460)
	void statisticsInitialized();

	@LogMessage(level = TRACE)
	@Message(value = "Statistics collection enabled", id = 461)
	void statisticsEnabled();

	@LogMessage(level = TRACE)
	@Message(value = "Statistics collection disabled", id = 462)
	void statisticsDisabled();

	@LogMessage(level = TRACE)
	@Message(value = "Statistics reset", id = 463)
	void statisticsReset();

	@LogMessage(level = DEBUG)
	@Message(value = "Query: %s, time: %sms, rows: %s", id = 117)
	void queryExecuted(String query, Long time, Long rows);

	@LogMessage(level = INFO)
	@Message(
			id = 400,
			value = """
					Logging statistics:
						Start time: %s
						Sessions opened (closed): %s (%s)
						Transactions started (successful): %s (%s)
						Optimistic lock failures: %s
						Flushes: %s
						Connections obtained: %s
						Statements prepared (closed): %s (%s)
						Second-level cache puts: %s
						Second-level cache hits (misses): %s (%s)
						Entities loaded: %s
						Entities fetched: %s (minimize this)
						Entities updated, upserted, inserted, deleted: %s, %s, %s, %s
						Collections loaded: %s
						Collections fetched: %s (minimize this)
						Collections updated, removed, recreated: %s, %s, %s
						Natural id queries executed on database: %s
						Natural id cache puts: %s
						Natural id cache hits (misses): %s (%s)
						Max natural id query execution time: %s ms
						Queries executed on database: %s
						Query cache puts: %s
						Query cache hits (misses): %s (%s)
						Max query execution time: %s ms
						Update timestamps cache puts: %s
						Update timestamps cache hits (misses): %s (%s)
						Query plan cache hits (misses): %s (%s)
					"""
	)
	void logStatistics(
			long startTime,
			long sessionOpenCount,
			long sessionCloseCount,
			long transactionCount,
			long committedTransactionCount,
			long optimisticFailureCount,
			long flushCount,
			long connectCount,
			long prepareStatementCount,
			long closeStatementCount,
			long secondLevelCachePutCount,
			long secondLevelCacheHitCount,
			long secondLevelCacheMissCount,
			long entityLoadCount,
			long entityFetchCount,
			long entityUpdateCount,
			long entityUpsertCount,
			long entityInsertCount,
			long entityDeleteCount,
			long collectionLoadCount,
			long collectionFetchCount,
			long collectionUpdateCount,
			long collectionRemoveCount,
			long collectionRecreateCount,
			long naturalIdQueryExecutionCount,
			long naturalIdCachePutCount,
			long naturalIdCacheHitCount,
			long naturalIdCacheMissCount,
			long naturalIdQueryExecutionMaxTime,
			long queryExecutionCount,
			long queryCachePutCount,
			long queryCacheHitCount,
			long queryCacheMissCount,
			long queryExecutionMaxTime,
			long updateTimestampsCachePutCount,
			long updateTimestampsCacheHitCount,
			long updateTimestampsCacheMissCount,
			long queryPlanCacheHitCount,
			long queryPlanCacheMissCount);
}
