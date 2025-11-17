/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.DEBUG;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90005801, max = 90005900 )
@SubSystemLogging(
		name = LoadingLogger.LOGGER_NAME,
		description = "Logging related to building parts of the domain model from JDBC or from cache"
)
@Internal
public interface LoadingLogger extends BasicLogger {
	String LOGGER_NAME = ResultsLogger.LOGGER_NAME + ".loading";

	LoadingLogger LOADING_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), LoadingLogger.class, LOGGER_NAME );

	static String subLoggerName(String subName) {
		return LOGGER_NAME + "." + subName;
	}

	static Logger subLogger(String subName) {
		return Logger.getLogger( subLoggerName( subName ) );
	}

	@LogMessage(level = DEBUG)
	@Message(id = 90005801,
			value = "Found matching entity in context, but it is scheduled for removal (returning null)")
	void foundEntityScheduledForRemoval();

	@LogMessage(level = DEBUG)
	@Message(id = 90005802,
			value = "Found matching entity in context, but the matched entity had an inconsistent type")
	void foundEntityWrongType();
}
