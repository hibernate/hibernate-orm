/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql;

import org.hibernate.query.QueryLogging;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class NativeQueryLogging {
	public static final Logger LOGGER = QueryLogging.subLogger( "native" );

	public static final boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();
	public static final boolean TRACE_ENABLED = LOGGER.isTraceEnabled();
}
