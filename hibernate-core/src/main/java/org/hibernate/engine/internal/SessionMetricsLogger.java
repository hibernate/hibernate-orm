/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.internal.log.SubSystemLogging;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.DEBUG;

@MessageLogger( projectCode = "HHH" )
@SubSystemLogging(
		name = SessionMetricsLogger.LOGGER_NAME,
		description = "Logging related to session metrics"
)
public interface SessionMetricsLogger extends BasicLogger {
	String LOGGER_NAME = "org.hibernate.session.metrics";

	SessionMetricsLogger SESSION_METRICS_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), SessionMetricsLogger.class, LOGGER_NAME );

	@LogMessage(level = DEBUG)
	@Message(
			id = 401,
			value = """
					Logging session metrics:
						%s ns acquiring %s JDBC connections
						%s ns releasing %s JDBC connections
						%s ns preparing %s JDBC statements
						%s ns executing %s JDBC statements
						%s ns executing %s JDBC batches
						%s ns performing %s second-level cache puts
						%s ns performing %s second-level cache hits
						%s ns performing %s second-level cache misses
						%s ns executing %s flushes (flushing a total of %s entities and %s collections)
						%s ns executing %s pre-partial-flushes
						%s ns executing %s partial-flushes (flushing a total of %s entities and %s collections)
					"""
	)
	void sessionMetrics(
			long jdbcConnectionAcquisitionTime,
			int jdbcConnectionAcquisitionCount,
			long jdbcConnectionReleaseTime,
			int jdbcConnectionReleaseCount,
			long jdbcPrepareStatementTime,
			int jdbcPrepareStatementCount,
			long jdbcExecuteStatementTime,
			int jdbcExecuteStatementCount,
			long jdbcExecuteBatchTime,
			int jdbcExecuteBatchCount,
			long cachePutTime,
			int cachePutCount,
			long cacheHitTime,
			int cacheHitCount,
			long cacheMissTime,
			int cacheMissCount,
			long flushTime,
			int flushCount,
			long flushEntityCount,
			long flushCollectionCount,
			long prePartialFlushTime,
			int prePartialFlushCount,
			long partialFlushTime,
			int partialFlushCount,
			long partialFlushEntityCount,
			long partialFlushCollectionCount);
}
