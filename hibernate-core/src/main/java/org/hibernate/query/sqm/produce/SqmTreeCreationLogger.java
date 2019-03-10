/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce;

import org.hibernate.Internal;
import org.hibernate.query.QueryLogger;

import org.jboss.logging.Logger;

/**
 * Logger used to log information about the creation of an SQM tree.
 *
 * @author Steve Ebersole
 */
@Internal
public interface SqmTreeCreationLogger {
	String LOGGER_NAME = QueryLogger.LOGGER_NAME + ".sqm.creation";

	Logger LOGGER = Logger.getLogger( LOGGER_NAME );

	boolean TRACE_ENABLED = LOGGER.isTraceEnabled();
	boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();

}
