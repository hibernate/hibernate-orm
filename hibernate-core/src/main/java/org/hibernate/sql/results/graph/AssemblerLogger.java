/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import org.hibernate.sql.results.ResultsLogger;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public interface AssemblerLogger {
	Logger LOGGER = ResultsLogger.subLogger( "assembler" );

	boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();
	boolean TRACE_ENABLED = LOGGER.isTraceEnabled();
}
