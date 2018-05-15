/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal;

import org.jboss.logging.Logger;

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
		return Logger.getMessageLogger( EnversMessageLogger .class, loggerName );
	}

	public static Logger logger(Class classNeedingLogging) {
		return Logger.getLogger( classNeedingLogging );
	}

	public static Logger logger(String loggerName) {
		return Logger.getLogger( loggerName );
	}
}
