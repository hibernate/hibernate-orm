/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import org.hibernate.Internal;
import org.hibernate.query.QueryLogging;

import org.jboss.logging.Logger;

/**
 * Logger used to log information about the creation of an SQM tree.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
@Internal
public interface SqmTreeCreationLogger {
	String LOGGER_NAME = QueryLogging.subLoggerName( "sqm.creation" );

	Logger LOGGER = Logger.getLogger( LOGGER_NAME );

	boolean TRACE_ENABLED = LOGGER.isTraceEnabled();
	boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();

}
