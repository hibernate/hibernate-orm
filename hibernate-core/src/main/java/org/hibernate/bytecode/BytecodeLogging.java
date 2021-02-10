/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.bytecode;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public interface BytecodeLogging {
	String NAME = "org.hibernate.orm.bytecode";

	Logger LOGGER = Logger.getLogger( NAME );

	static String subLoggerName(String subName) {
		return NAME + "." + subName;
	}

	boolean TRACE_ENABLED = LOGGER.isTraceEnabled();
	boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();
}
