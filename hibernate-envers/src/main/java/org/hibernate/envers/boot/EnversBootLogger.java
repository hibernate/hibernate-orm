/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot;

import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.INFO;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90005601, max = 90005700 )
@SubSystemLogging(
		name = EnversBootLogger.LOGGER_NAME,
		description = "Logging related to bootstrapping an Envers (currently just its in-flight generation " +
				"of `hbm.xml` mappings for Hibernate ORM to process)"
)
public interface EnversBootLogger extends BasicLogger {
	String LOGGER_NAME = "org.hibernate.envers.boot";

	EnversBootLogger BOOT_LOGGER = Logger.getMessageLogger(
			MethodHandles.lookup(),
			EnversBootLogger.class,
			LOGGER_NAME
	);

	boolean TRACE_ENABLED = BOOT_LOGGER.isTraceEnabled();
	boolean DEBUG_ENABLED = BOOT_LOGGER.isDebugEnabled();

	static String subLoggerName(String subName) {
		return LOGGER_NAME + '.' + subName;
	}

	static Logger subLogger(String subName) {
		return Logger.getLogger( subLoggerName( subName ) );
	}

	/**
	 * Log about usage of deprecated Scanner setting
	 */
	@LogMessage( level = INFO )
	@Message(
			value = "Envers-generated HBM mapping...%n%s",
			id = 90005601
	)
	void jaxbContribution(String hbm);
}
