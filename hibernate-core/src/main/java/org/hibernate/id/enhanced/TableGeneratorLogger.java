/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import java.sql.SQLException;

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

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;

/**
 * Logging related to {@link TableGenerator} operations
 *
 * @author Gavin King
 */
@SubSystemLogging(
		name = TableGeneratorLogger.NAME,
		description = "Logging related to table-based identifier generation"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90101, max = 90200)
@Internal
public interface TableGeneratorLogger extends BasicLogger {
	String NAME = SubSystemLogging.BASE + ".id.table";

	Logger TABLE_GENERATOR_LOGGER = Logger.getLogger(NAME);
	TableGeneratorLogger TABLE_GENERATOR_MESSAGE_LOGGER = Logger.getMessageLogger(
			MethodHandles.lookup(),
			TableGeneratorLogger.class,
			NAME
	);

	@LogMessage(level = ERROR)
	@Message(value = "Could not read hi value in table: %s", id = 90101)
	void unableToReadHiValue(String tableName, @Cause SQLException e);

	@LogMessage(level = ERROR)
	@Message(value = "Could not read or initialize hi value in table: %s", id = 90102)
	void unableToReadOrInitializeHiValue(String tableName, @Cause SQLException e);

	@LogMessage(level = ERROR)
	@Message(value = "Could not update hi value in table: %s", id = 90103)
	void unableToUpdateHiValue(String tableName, @Cause SQLException e);

	@LogMessage(level = INFO)
	@Message(value = "Forcing table use for sequence-style generator due to pooled optimizer selection where db does not support pooled sequences",
			id = 90107)
	void forcingTableUse();

	@LogMessage(level = INFO)
	@Message(value = "Explicit segment value for id generator [%s.%s] suggested; using default [%s]", id = 90110)
	void usingDefaultIdGeneratorSegmentValue(String tableName, String segmentColumnName, String defaultToUse);

	@LogMessage(level = TRACE)
	@Message(value = "Retrieving current value for table generator segment '%s'", id = 90111)
	void retrievingCurrentValueForSegment(String segmentValue);

	@LogMessage(level = TRACE)
	@Message(value = "Inserting initial value '%s' for table generator segment '%s'", id = 90112)
	void insertingInitialValueForSegment(Object value, String segmentValue);

	@LogMessage(level = TRACE)
	@Message(value = "Updating current value to '%s' for table generator segment '%s'", id = 90113)
	void updatingCurrentValueForSegment(Object updateValue, String segmentValue);
}
