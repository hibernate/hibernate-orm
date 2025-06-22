/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;

/**
 * Quite sad, really, when you need helpers for generating loggers...
 *
 * @author Steve Ebersole
 */
public class CoreLogging {

	/**
	 * Disallow instantiation
	 */
	private CoreLogging() {
	}

	public static CoreMessageLogger messageLogger(Class<?> classNeedingLogging) {
		return messageLogger( classNeedingLogging.getName() );
	}

	public static CoreMessageLogger messageLogger(String loggerName) {
		return Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, loggerName );
	}

	public static Logger logger(Class<?> classNeedingLogging) {
		return Logger.getLogger( classNeedingLogging );
	}

	public static Logger logger(String loggerName) {
		return Logger.getLogger( loggerName );
	}
}
