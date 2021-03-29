/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor;

import org.jboss.logging.Logger;

public interface JdbcExtractingLogging {
	String NAME = "org.hibernate.orm.jdbc.extract";

	Logger LOGGER = Logger.getLogger( NAME );

	boolean TRACE_ENABLED = LOGGER.isTraceEnabled();
	boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();

}
