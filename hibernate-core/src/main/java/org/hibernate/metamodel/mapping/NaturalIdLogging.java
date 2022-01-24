/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.internal.CoreLogging;

import org.jboss.logging.Logger;

/**
 * Logging related to natural-id operations
 *
 * @author Steve Ebersole
 */
public interface NaturalIdLogging {
	String LOGGER_NAME = CoreLogging.subsystemLoggerName( "mapping.natural_id" );
	Logger LOGGER = Logger.getLogger( LOGGER_NAME );

	boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();
	boolean TRACE_ENABLED = LOGGER.isTraceEnabled();
}
