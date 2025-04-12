/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90004001, max = 90005000 )
@SubSystemLogging(
		name = SqlExecLogger.LOGGER_NAME,
		description = "Logging related to the execution of SQL statements"
)
@Internal
public interface SqlExecLogger extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".sql.exec";

	SqlExecLogger SQL_EXEC_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), SqlExecLogger.class, LOGGER_NAME );
}
