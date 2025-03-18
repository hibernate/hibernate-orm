/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal;

import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;

/**
 * Sad when you need helpers for generating loggers...
 *
 * @author Steve Ebersole
 */
public class EnversLogging {
	/**
	 * Disallow instantiation
	 */
	private EnversLogging() {
	}

	public static EnversMessageLogger messageLogger(Class classNeedingLogging) {
		return messageLogger( classNeedingLogging.getName() );
	}

	public static EnversMessageLogger messageLogger(String loggerName) {
		return Logger.getMessageLogger( MethodHandles.lookup(), EnversMessageLogger .class, loggerName );
	}

	public static Logger logger(Class classNeedingLogging) {
		return Logger.getLogger( classNeedingLogging );
	}

	public static Logger logger(String loggerName) {
		return Logger.getLogger( loggerName );
	}
}
