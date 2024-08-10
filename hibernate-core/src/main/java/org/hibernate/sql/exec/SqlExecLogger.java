/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec;

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
public interface SqlExecLogger extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".sql.exec";

	SqlExecLogger SQL_EXEC_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), SqlExecLogger.class, LOGGER_NAME );
}
