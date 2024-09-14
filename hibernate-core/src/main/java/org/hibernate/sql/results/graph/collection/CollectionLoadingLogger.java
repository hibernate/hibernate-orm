/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.results.graph.collection;

import org.hibernate.internal.log.SubSystemLogging;
import org.hibernate.sql.results.LoadingLogger;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = CollectionLoadingLogger.LOGGER_NAME,
		description = "Logging related to collection loading"
)
public interface CollectionLoadingLogger extends BasicLogger {
	String LOGGER_NAME = LoadingLogger.LOGGER_NAME + ".collection";

	/**
	 * Static access to the logging instance
	 */
	Logger COLL_LOAD_LOGGER = LoadingLogger.subLogger( LOGGER_NAME );
}
