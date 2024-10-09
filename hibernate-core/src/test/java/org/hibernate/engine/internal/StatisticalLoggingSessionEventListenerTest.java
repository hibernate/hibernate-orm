/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.CoreMessageLogger;

import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.Rule;
import org.junit.jupiter.api.Test;

import org.jboss.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Christian Bürgi
 */
@DomainModel(annotatedClasses = {
		BasicEntity.class
})
@SessionFactory(generateStatistics = true)
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "10"),
		@Setting(name = AvailableSettings.ORDER_INSERTS, value = "true"),
		@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
		@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true")
})
@Jira("https://hibernate.atlassian.net/browse/HHH-18513")
public class StatisticalLoggingSessionEventListenerTest {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger(
					CoreMessageLogger.class,
					StatisticalLoggingSessionEventListener.class.getName()
			) );

	@Test
	void testSessionMetricsLog(SessionFactoryScope scope) {
		Triggerable triggerable = logInspection.watchForLogMessages( "Session Metrics {" );
		long startTime = System.nanoTime();

		scope.inTransaction( session -> {
			session.persist( new BasicEntity( 1, "fooData" ) );
			session.persist( new BasicEntity( 2, "fooData2" ) );
			session.flush();
			session.clear();
			for ( int i = 0; i < 2; i++ ) {
				assertThat( session.createQuery( "select data from BasicEntity e where e.id=1", String.class )
									.setCacheable( true )
									.getSingleResult() ).isEqualTo( "fooData" );
			}
		} );
		long sessionNanoDuration = System.nanoTime() - startTime;

		List<String> messages = triggerable.triggerMessages();
		assertThat( messages ).hasSize( 1 );
		String sessionMetricsLog = messages.get( 0 );

		// acquiring JDBC connections
		SessionMetric acquiringJdbcConnectionsMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent acquiring ([0-9]+) JDBC connections"
		);
		assertThat( acquiringJdbcConnectionsMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );

		// releasing JDBC connections
		SessionMetric releasingJdbcConnectionsMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent releasing ([0-9]+) JDBC connections"
		);
		assertThat( releasingJdbcConnectionsMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );

		// preparing JDBC statements
		SessionMetric preparingJdbcStatmentsMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent preparing ([0-9]+) JDBC statements"
		);
		assertThat( preparingJdbcStatmentsMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );

		// executing JDBC statements
		SessionMetric executingJdbcStatmentsMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent executing ([0-9]+) JDBC statements"
		);
		assertThat( executingJdbcStatmentsMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );

		// executing JDBC batches
		SessionMetric executingJdbcBatchesMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent executing ([0-9]+) JDBC batches"
		);
		assertThat( executingJdbcBatchesMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );

		// performing L2C puts
		SessionMetric performingL2CPutsMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent performing ([0-9]+) L2C puts"
		);
		assertThat( performingL2CPutsMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );

		// performing L2C hits
		SessionMetric performingL2CHitsMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent performing ([0-9]+) L2C hits"
		);
		assertThat( performingL2CHitsMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );

		// performing L2C misses
		SessionMetric performingL2CMissesMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent performing ([0-9]+) L2C misses"
		);
		assertThat( performingL2CMissesMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );

		// executing flushes
		SessionMetric executingFlushesMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent executing ([0-9]+) flushes"
		);
		assertThat( executingFlushesMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );

		// executing pre-partial-flushes
		SessionMetric executingPrePartialFlushesMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent executing ([0-9]+) pre-partial-flushes"
		);
		assertThat( executingPrePartialFlushesMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );

		// executing partial-flushes
		SessionMetric executingPartialFlushesMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent executing ([0-9]+) partial-flushes"
		);
		assertThat( executingPartialFlushesMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );

		// Number of metrics
		List<SessionMetric> metricList = List.of(
				acquiringJdbcConnectionsMetric,
				releasingJdbcConnectionsMetric,
				preparingJdbcStatmentsMetric,
				executingJdbcStatmentsMetric,
				executingJdbcBatchesMetric,
				performingL2CPutsMetric,
				performingL2CHitsMetric,
				performingL2CMissesMetric,
				executingFlushesMetric,
				executingPrePartialFlushesMetric,
				executingPartialFlushesMetric
		);
		int numberOfMetrics = metricList.size();
		// Number of lines
		assertThat( sessionMetricsLog.lines().count() )
				.as( "The StatisticalLoggingSessionEventListener should write a line per metric ("
					+ numberOfMetrics + " lines) plus a header and a footer (2 lines)" )
				.isEqualTo( numberOfMetrics + 2 );
		// Total time
		long sumDuration = metricList.stream().map( SessionMetric::getDuration ).mapToLong( Long::longValue ).sum();
		assertThat( sumDuration ).isLessThanOrEqualTo( sessionNanoDuration );
	}

	private SessionMetric extractMetric(String logMessage, String regex) {
		Pattern pattern = Pattern.compile( regex );
		Matcher matcher = pattern.matcher( logMessage );
		assertTrue( matcher.find() );
		return new SessionMetric( Long.parseLong( matcher.group( 1 ) ), Integer.parseInt( matcher.group( 2 ) ) );
	}

	private static class SessionMetric {
		private final long duration;
		private final int count;

		public SessionMetric(long duration, int count) {
			this.duration = duration;
			this.count = count;
		}

		public long getDuration() {
			return duration;
		}

		public int getCount() {
			return count;
		}
	}
}
