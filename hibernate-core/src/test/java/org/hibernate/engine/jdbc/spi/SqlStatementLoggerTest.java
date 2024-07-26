package org.hibernate.engine.jdbc.spi;

import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Kyuhee Cho
 */
@JiraKey(value = "HHH-15300")
class SqlStatementLoggerTest {

	@Test
	public void testLogSlowQueryFromStatementWhenLoggingDisabled() {
		SqlStatementLogger sqlStatementLogger = new SqlStatementLogger( false, false, false, 0L );
		AtomicInteger callCounterToString = new AtomicInteger();
		Statement statement = mockStatementForCountingToString( callCounterToString );

		sqlStatementLogger.logSlowQuery( statement, System.nanoTime(), null );
		assertEquals( 0, callCounterToString.get() );
	}

	@Test
	public void testLogSlowQueryFromStatementWhenLoggingEnabled() {
		long logSlowQueryThresholdMillis = 300L;
		SqlStatementLogger sqlStatementLogger = new SqlStatementLogger(
				false,
				false,
				false,
				logSlowQueryThresholdMillis
		);
		AtomicInteger callCounterToString = new AtomicInteger();
		Statement statement = mockStatementForCountingToString( callCounterToString );

		long startTimeNanos = System.nanoTime() - TimeUnit.MILLISECONDS.toNanos( logSlowQueryThresholdMillis + 1 );
		sqlStatementLogger.logSlowQuery( statement, startTimeNanos, null );
		assertEquals( 1, callCounterToString.get() );
	}

	private Statement mockStatementForCountingToString(AtomicInteger callCounter) {
		Statement statement = mock( Statement.class );
		when( statement.toString() ).then( (Answer<String>) invocation -> {
			callCounter.incrementAndGet();
			return (String) invocation.callRealMethod();
		} );
		return statement;
	}

}
