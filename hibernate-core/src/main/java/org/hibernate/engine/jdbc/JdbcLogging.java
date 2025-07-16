/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

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

	@LogMessage(level = TRACE)
	@Message(value = "Closing logical connection @%s", id = 100009)
	void closingLogicalConnection(int hashCode);

	@LogMessage(level = TRACE)
	@Message(value = "Closed logical connection @%s", id = 100010)
	void logicalConnectionClosed(int hashCode);

	@LogMessage(level = TRACE)
	@Message(value = "Skipping aggressive release of JDBC connection @%s from 'afterStatement' due to held resources", id = 100011)
	void skipConnectionReleaseAfterStatementDueToResources(int hashCode);

	@LogMessage(level = TRACE)
	@Message(value = "Initiating release of JDBC connection @%s from 'afterStatement'", id = 100012)
	void initiatingConnectionReleaseAfterStatement(int hashCode);

	@LogMessage(level = TRACE)
	@Message(value = "Initiating release of JDBC connection @%s from 'beforeTransactionCompletion'", id = 100013)
	void initiatingConnectionReleaseBeforeTransactionCompletion(int hashCode);

	@LogMessage(level = TRACE)
	@Message(value = "Initiating release of JDBC connection @%s from 'afterTransaction'", id = 100014)
	void initiatingConnectionReleaseAfterTransaction(int hashCode);

	@LogMessage(level = WARN)
	@Message(value = "Error before releasing JDBC connection @%s", id = 100015)
	void errorBeforeReleasingJdbcConnection(int hashCode, @Cause Throwable e);

	@LogMessage(level = DEBUG)
	@Message(
			id = 100016,
			value =
					"'" + CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT + "' " +
					"""
					was enabled. This setting should only be enabled when JDBC Connections obtained by Hibernate \
					from the ConnectionProvider have auto-commit disabled. Enabling this setting when connections \
					have auto-commit enabled leads to execution of SQL operations outside of any JDBC transaction.\
					"""
	)
	void connectionProviderDisablesAutoCommitEnabled();

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

	@LogMessage(level = TRACE)
	@Message(value = "Unable to reset connection back to auto-commit", id = 100019)
	void unableToResetAutoCommit(@Cause Exception ignored);

	@LogMessage(level = INFO)
	@Message(value = "Unable to release isolated connection", id = 100020)
	void unableToReleaseIsolatedConnection(@Cause Exception ignored);

	@LogMessage(level = INFO)
	@Message(value = "Unable to roll back isolated connection on exception ", id = 100021)
	void unableToRollBackIsolatedConnection(@Cause Exception ignored);

	@LogMessage(level = WARN)
	@Message(value = "Called joinTransaction() on a non-JTA EntityManager (ignoring)", id = 100025)
	void callingJoinTransactionOnNonJtaEntityManager();

	@LogMessage(level = TRACE)
	@Message(value = "Current timestamp retrieved from database: %s (nanos=%s, time=%s)", id = 100031)
	void currentTimestampRetrievedFromDatabase(Object timestamp, int nanos, long time);

	@LogMessage(level = TRACE)
	@Message(value = "Sequence value retrieved from database: %s", id = 100032)
	void sequenceValueRetrievedFromDatabase(Number sequenceValue);
}
