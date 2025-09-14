/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Sub-system logging related to JDBC interactions
 *
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = JdbcLogging.NAME,
		description = "Logging related to JDBC interactions"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 100001, max = 100500)
@Internal
public interface JdbcLogging extends BasicLogger {
	String NAME = SubSystemLogging.BASE + ".jdbc";

	Logger JDBC_LOGGER = Logger.getLogger( NAME );
	JdbcLogging JDBC_MESSAGE_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), JdbcLogging.class, NAME );

	@LogMessage(level = WARN)
	@Message(
			id=100001,
			value = "JDBC driver did not return the expected number of row counts (%s) - expected %s, but received %s"
	)
	void unexpectedRowCounts(String tableName, int expected, int actual);

	@LogMessage(level = TRACE)
	@Message(value = "Created JdbcCoordinator @%s", id = 100002)
	void createdJdbcCoordinator(int hashCode);

	@LogMessage(level = TRACE)
	@Message(value = "Closing JdbcCoordinator @%s", id = 100003)
	void closingJdbcCoordinator(int hashCode);

	@LogMessage(level = TRACE)
	@Message(value = "Statement execution complete (connection release mode %s) in JdbcCoordinator @%s", id = 100004)
	void statementExecutionComplete(ConnectionReleaseMode connectionReleaseMode, int hashCode);

	@LogMessage(level = TRACE)
	@Message(value = "Transaction after begin in JdbcCoordinator @%s", id = 100005)
	void transactionAfterBegin(int hashCode);

	@LogMessage(level = TRACE)
	@Message(value = "Transaction before completion in JdbcCoordinator @%s", id = 100006)
	void transactionBeforeCompletion(int hashCode);

	@LogMessage(level = TRACE)
	@Message(value = "Transaction after %s completion in JdbcCoordinator @%s", id = 100007)
	void transactionAfterCompletion(String completionStatus, int hashCode);

	@LogMessage(level = DEBUG)
	@Message(value = "Closing unreleased batch in JdbcCoordinator @%s", id = 100008)
	void closingUnreleasedBatch(int hashCode);

	@LogMessage(level = DEBUG)
	@Message(value = """
			Database:
				name: %s
				version: %s
				major: %s
				minor: %s""",
			id = 100017)
	void logDatabaseInfo(String name, String version, int major, int minor);

	@LogMessage(level = DEBUG)
	@Message(value = """
			Driver:
				name: %s
				version: %s
				major: %s
				minor: %s
				JDBC version: %s.%s""",
			id = 100018)
	void logDriverInfo(String name, String version, int major, int minor, int jdbcMajor, int jdbcMinor);

	@LogMessage(level = INFO)
	@Message(value = "Unable to release isolated connection", id = 100020)
	void unableToReleaseIsolatedConnection(@Cause Exception ignored);

	@LogMessage(level = DEBUG)
	@Message(value = "Unable to release connection", id = 100021)
	void unableToReleaseConnection(@Cause Exception ignored);

	@LogMessage(level = INFO)
	@Message(value = "Unable to roll back isolated connection on exception ", id = 100022)
	void unableToRollBackIsolatedConnection(@Cause Exception ignored);

	@LogMessage(level = TRACE)
	@Message(value = "Unable to reset connection back to auto-commit enabled", id = 100040)
	void unableToResetAutoCommitEnabled(@Cause Exception ignored);

	@LogMessage(level = TRACE)
	@Message(value = "Unable to reset connection back to auto-commit disabled", id = 100041)
	void unableToResetAutoCommitDisabled(@Cause Exception ignored);

	@LogMessage(level = DEBUG)
	@Message(value = "Using default JDBC fetch size: %s", id = 100122)
	void usingFetchSize(int fetchSize);

	@LogMessage(level = WARN)
	@Message(value = "Low default JDBC fetch size: %s (consider setting 'hibernate.jdbc.fetch_size')", id = 100123)
	void warnLowFetchSize(int fetchSize);

	@LogMessage(level = TRACE)
	@Message(value = "JDBC fetch size: %s", id = 100124)
	void fetchSize(int fetchSize);

	@LogMessage(level = DEBUG)
	@Message(value = "Low JDBC fetch size: %s (consider setting 'hibernate.jdbc.fetch_size')", id = 100125)
	void lowFetchSize(int fetchSize);

	@LogMessage(level = TRACE)
	@Message(value = "Setting JDBC fetch size: %s", id = 100126)
	void settingFetchSize(int fetchSize);

	@LogMessage(level = TRACE)
	@Message(value = "Setting JDBC query timeout: %s", id = 100127)
	void settingQueryTimeout(int timeout);

	@LogMessage(level = WARN)
	@Message(value = "Called joinTransaction() on a non-JTA EntityManager (ignoring)", id = 100030)
	void callingJoinTransactionOnNonJtaEntityManager();

	@LogMessage(level = TRACE)
	@Message(value = "Current timestamp retrieved from database: %s (nanos=%s, time=%s)", id = 100031)
	void currentTimestampRetrievedFromDatabase(Object timestamp, int nanos, long time);

	@LogMessage(level = TRACE)
	@Message(value = "Sequence value retrieved from database: %s", id = 100032)
	void sequenceValueRetrievedFromDatabase(Number sequenceValue);

	@LogMessage(level = TRACE)
	@Message(value = "Notifying resource-local transaction observers after begin", id = 100033)
	void notifyingResourceLocalObserversAfterBegin();

	@LogMessage(level = TRACE)
	@Message(value = "Notifying resource-local transaction observers before completion", id = 100034)
	void notifyingResourceLocalObserversBeforeCompletion();

	@LogMessage(level = TRACE)
	@Message(value = "Notifying resource-local transaction observers after completion", id = 100035)
	void notifyingResourceLocalObserversAfterCompletion();

	@LogMessage(level = TRACE)
	@Message(value = "On commit, transaction was marked for rollback only, rolling back", id = 100036)
	void onCommitMarkedRollbackOnlyRollingBack();

	@LogMessage(level = TRACE)
	@Message(value = "JDBC transaction marked for rollback only (exception provided for stack trace)", id = 100037)
	void jdbcTransactionMarkedForRollbackOnly(@Cause Throwable stackTraceHolder);

	@LogMessage(level = DEBUG)
	@Message(value = "Encountered failure rolling back failed commit", id = 100038)
	void encounteredFailureRollingBackFailedCommit(@Cause Throwable cause);

	@LogMessage(level = DEBUG)
	@Message(value = "Could not access DatabaseMetaData while building the JdbcEnvironment", id = 100039)
	void noDatabaseMetaData(@Cause SQLException sqle);

	@LogMessage(level = WARN)
	@Message(value = "Could not obtain connection metadata: %s", id = 100045)
	void unableToObtainConnectionMetadata(SQLException error);

	@LogMessage(level = WARN)
	@Message(value = "Could not obtain connection to query JDBC database metadata", id = 100046)
	void unableToObtainConnectionToQueryMetadata(@Cause Exception e);

}
