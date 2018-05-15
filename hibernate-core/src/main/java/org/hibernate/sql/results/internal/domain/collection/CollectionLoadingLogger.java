/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import org.hibernate.sql.results.SqlResultsLogger;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90005101, max = 90005200 )
public interface CollectionLoadingLogger extends BasicLogger {
	String LOGGER_NAME = SqlResultsLogger.LOGGER_NAME + "loading.collection";

	/**
	 * Static access to the logging instance
	 */
	CollectionLoadingLogger INSTANCE = Logger.getMessageLogger(
			CollectionLoadingLogger.class,
			LOGGER_NAME
	);

	boolean TRACE_ENABLED = INSTANCE.isTraceEnabled();
	boolean DEBUG_ENABLED = INSTANCE.isDebugEnabled();
	boolean INFO_ENABLED = INSTANCE.isInfoEnabled();
}
