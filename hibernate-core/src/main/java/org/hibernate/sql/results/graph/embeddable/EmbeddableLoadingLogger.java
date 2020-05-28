/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable;

import org.hibernate.sql.results.LoadingLogger;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90005301, max = 90005400 )
public interface EmbeddableLoadingLogger extends BasicLogger {
	String LOCAL_NAME = "composite";

	String LOGGER_NAME = LoadingLogger.subLoggerName( LOCAL_NAME );

	/**
	 * Static access to the logging instance
	 */
	Logger INSTANCE = LoadingLogger.subLogger( LOCAL_NAME );


	boolean TRACE_ENABLED = INSTANCE.isTraceEnabled();
	boolean DEBUG_ENABLED = INSTANCE.isDebugEnabled();
}
