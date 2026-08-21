/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.jdbc.internal;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 10002501, max = 10003000 )
@SubSystemLogging(
		name = ResourceRegistryLogger.LOGGER_NAME,
		description = "Logging related to use of ResourceRegistry"
)
@Internal
public interface ResourceRegistryLogger extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".resource.registry";

	/**
	 * Static access to the logging instance
	 */
	ResourceRegistryLogger RESOURCE_REGISTRY_LOGGER = Logger.getMessageLogger(
			MethodHandles.lookup(),
			ResourceRegistryLogger.class,
			LOGGER_NAME
	);

	@LogMessage(level = TRACE)
	@Message(value = "Releasing registered JDBC resources", id = 10002501)
	void releasingResources();

	@LogMessage(level = TRACE)
	@Message(value = "Registering statement [%s]", id = 10002502)
	void registeringStatement(Statement statement);

	@LogMessage(level = TRACE)
	@Message(value = "Releasing statement [%s]", id = 10002503)
	void releasingStatement(Statement statement);

	@LogMessage(level = TRACE)
	@Message(value = "Releasing result set [%s]", id = 10002504)
	void releasingResultSet(ResultSet resultSet);

	@LogMessage(level = TRACE)
	@Message(value = "Closing result set [%s]", id = 10002505)
	void closingResultSet(ResultSet resultSet);

	@LogMessage(level = TRACE)
	@Message(value = "Closing prepared statement [%s]", id = 10002506)
	void closingPreparedStatement(Statement statement);

	@LogMessage(level = TRACE)
	@Message(value = "Registering result set [%s]", id = 10002507)
	void registeringResultSet(ResultSet resultSet);

	@LogMessage(level = DEBUG)
	@Message(value = "Unable to release JDBC statement [%s]", id = 10002508)
	void unableToReleaseStatement(String message);

	@LogMessage(level = DEBUG)
	@Message(value = "Unable to release JDBC result set [%s]", id = 10002509)
	void unableToReleaseResultSet(String message);

	@LogMessage(level = DEBUG)
	@Message(value = "Exception clearing maxRows or queryTimeout for JDBC Statement [%s]", id = 10002510)
	void exceptionClearingMaxRowsOrQueryTimeout(String message);

	@LogMessage(level = DEBUG)
	@Message(value = "Unable to free '%s' reference [%s]", id = 10002511)
	void unableToFreeLob(String lobType, String message);

	// Keep this at DEBUG level, rather than WARN. Many connection pool implementations return
	// a proxy/wrapper for the JDBC Statement, causing excessive logging here. See HHH-8210.
	@LogMessage(level = DEBUG)
	@Message(value = "Statement associated with ResultSet was not registered", id = 10002514)
	void unregisteredStatement();

	@LogMessage(level = WARN)
	@Message(value = "ResultSet had no statement associated with it, but was not yet registered", id = 10002515)
	void unregisteredResultSetWithoutStatement();

	@LogMessage(level = DEBUG)
	@Message(value = "Request to release '%s', but none have ever been registered", id = 10002516)
	void noRegisteredLobs(String lobType);

}
