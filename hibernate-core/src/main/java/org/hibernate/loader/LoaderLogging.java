/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader;

import org.jboss.logging.Logger;

/**
 * Logging for loaders
 */
public interface LoaderLogging {
	String LOGGER_NAME = "org.hibernate.orm.loader";

	Logger LOADER_LOGGER = Logger.getLogger( LOGGER_NAME );

	boolean DEBUG_ENABLED = LOADER_LOGGER.isDebugEnabled();
	boolean TRACE_ENABLED = LOADER_LOGGER.isTraceEnabled();
}
