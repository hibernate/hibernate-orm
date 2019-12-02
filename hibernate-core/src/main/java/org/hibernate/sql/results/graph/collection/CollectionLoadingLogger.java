/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection;

import org.hibernate.sql.results.ResultsLogger;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public interface CollectionLoadingLogger extends BasicLogger {
	String LOGGER_NAME = ResultsLogger.LOGGER_NAME + "loading.collection";

	/**
	 * Static access to the logging instance
	 */
	Logger INSTANCE = Logger.getLogger( LOGGER_NAME );

	boolean TRACE_ENABLED = INSTANCE.isTraceEnabled();
	boolean DEBUG_ENABLED = INSTANCE.isDebugEnabled();
}
