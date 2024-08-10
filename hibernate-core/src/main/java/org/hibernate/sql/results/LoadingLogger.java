/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results;

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
@ValidIdRange( min = 90005801, max = 90005900 )
@SubSystemLogging(
		name = LoadingLogger.LOGGER_NAME,
		description = "Logging related to building parts of the domain model from JDBC or from cache"
)
public interface LoadingLogger extends BasicLogger {
	String LOGGER_NAME = ResultsLogger.LOGGER_NAME + ".loading";

	Logger LOGGER = Logger.getLogger( LOGGER_NAME );
	LoadingLogger MESSAGE_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), LoadingLogger.class, LOGGER_NAME );

	static String subLoggerName(String subName) {
		return LOGGER_NAME + "." + subName;
	}

	static Logger subLogger(String subName) {
		return Logger.getLogger( subLoggerName( subName ) );
	}

	boolean TRACE_ENABLED = LOGGER.isTraceEnabled();
	boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();
}
