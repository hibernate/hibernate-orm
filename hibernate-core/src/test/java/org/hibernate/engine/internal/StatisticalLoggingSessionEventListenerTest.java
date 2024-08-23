/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.engine.internal;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		assertThat( acquiringJdbcConnectionsMetric.getCount() ).isEqualTo( 1 );

		// releasing JDBC connections
		SessionMetric releasingJdbcConnectionsMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent releasing ([0-9]+) JDBC connections"
		);
		assertThat( releasingJdbcConnectionsMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );
		assertThat( releasingJdbcConnectionsMetric.getCount() ).isEqualTo( 1 );

		// preparing JDBC statements
		SessionMetric preparingJdbcStatmentsMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent preparing ([0-9]+) JDBC statements"
		);
		assertThat( preparingJdbcStatmentsMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );
		assertThat( preparingJdbcStatmentsMetric.getCount() ).isEqualTo( 3 );

		// executing JDBC statements
		SessionMetric executingJdbcStatmentsMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent executing ([0-9]+) JDBC statements"
		);
		assertThat( executingJdbcStatmentsMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );
		assertThat( executingJdbcStatmentsMetric.getCount() ).isEqualTo( 2 );

		// executing JDBC batches
		SessionMetric executingJdbcBatchesMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent executing ([0-9]+) JDBC batches"
		);
		assertThat( executingJdbcBatchesMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );
		assertThat( executingJdbcBatchesMetric.getCount() ).isEqualTo( 1 );

		// performing L2C puts
		SessionMetric performingL2CPutsMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent performing ([0-9]+) L2C puts"
		);
		assertThat( performingL2CPutsMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );
		assertThat( performingL2CPutsMetric.getCount() ).isEqualTo( 4 );

		// performing L2C hits
		SessionMetric performingL2CHitsMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent performing ([0-9]+) L2C hits"
		);
		assertThat( performingL2CHitsMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );
		assertThat( performingL2CHitsMetric.getCount() ).isEqualTo( 2 );

		// performing L2C misses
		SessionMetric performingL2CMissesMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent performing ([0-9]+) L2C misses"
		);
		assertThat( performingL2CMissesMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );
		assertThat( performingL2CMissesMetric.getCount() ).isEqualTo( 1 );

		// executing flushes
		SessionMetric executingFlushesMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent executing ([0-9]+) flushes"
		);
		assertThat( executingFlushesMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );
		assertThat( executingFlushesMetric.getCount() ).isEqualTo( 1 );

		// executing pre-partial-flushes
		SessionMetric executingPrePartialFlushesMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent executing ([0-9]+) pre-partial-flushes"
		);
		assertThat( executingPrePartialFlushesMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );
		assertThat( executingPrePartialFlushesMetric.getCount() ).isEqualTo( 2 );

		// executing partial-flushes
		SessionMetric executingPartialFlushesMetric = extractMetric(
				sessionMetricsLog,
				"([0-9]+) nanoseconds spent executing ([0-9]+) partial-flushes"
		);
		assertThat( executingPartialFlushesMetric.getDuration() ).isGreaterThan( 0L )
				.isLessThan( sessionNanoDuration );
		assertThat( executingPartialFlushesMetric.getCount() ).isEqualTo( 2 );

		// Number of metrics
		final int numberOfMetrics = 11;
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
		assertThat( metricList.size() ).isEqualTo( numberOfMetrics );
		// Number of lines
		assertThat( sessionMetricsLog.lines().count() ).isEqualTo( numberOfMetrics + 2 );
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
