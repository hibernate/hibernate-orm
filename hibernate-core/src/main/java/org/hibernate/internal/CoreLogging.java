/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
