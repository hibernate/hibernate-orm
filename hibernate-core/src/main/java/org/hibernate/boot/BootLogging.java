/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot;

import org.jboss.logging.Logger;

import static org.hibernate.internal.CoreLogging.subsystemLoggerName;

/**
 * Logging related to Hibernate bootstrapping
 */
public class BootLogging {
	public static String NAME = subsystemLoggerName( "boot" );

	public static final Logger LOGGER = Logger.getLogger( NAME );

	public static String subLoggerName(String subPath) {
		return NAME + "." + subPath;
	}

	public static Logger subLogger(String subPath) {
		return Logger.getLogger( subLoggerName( subPath ) );
	}

	public static final boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();
	public static final boolean TRACE_ENABLED = LOGGER.isTraceEnabled();
}
