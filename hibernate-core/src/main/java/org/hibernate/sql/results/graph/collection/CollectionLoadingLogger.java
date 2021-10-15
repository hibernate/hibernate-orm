/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection;

import org.hibernate.sql.results.LoadingLogger;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public interface CollectionLoadingLogger extends BasicLogger {
	String LOCAL_NAME = "collection";

	String LOGGER_NAME = LoadingLogger.subLoggerName( LOCAL_NAME );

	/**
	 * Static access to the logging instance
	 */
	Logger COLL_LOAD_LOGGER = LoadingLogger.subLogger( LOCAL_NAME );

	/**
	 * @deprecated Use {@link #COLL_LOAD_LOGGER}
	 */
	@Deprecated
	Logger INSTANCE = COLL_LOAD_LOGGER;

	boolean TRACE_ENABLED = INSTANCE.isTraceEnabled();
	boolean DEBUG_ENABLED = INSTANCE.isDebugEnabled();
}
