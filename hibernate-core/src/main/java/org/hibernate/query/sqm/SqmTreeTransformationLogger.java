/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import org.hibernate.query.QueryLogging;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public interface SqmTreeTransformationLogger {
	String LOGGER_NAME = QueryLogging.subLoggerName( "sqm.transform" );

	Logger LOGGER = Logger.getLogger( LOGGER_NAME );

	boolean TRACE_ENABLED = LOGGER.isTraceEnabled();
	boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();
}
