/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.internal;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = DatabaseOperationLogging.DB_OP_LOGGER_NAME,
		description = "Stuff"
)
@Internal
public interface DatabaseOperationLogging {
	String DB_OP_LOGGER_NAME = SubSystemLogging.BASE + ".sql.op";

	Logger DB_OP_LOGGER = Logger.getLogger( DB_OP_LOGGER_NAME );

	boolean DB_OP_DEBUG_ENABLED = DB_OP_LOGGER.isDebugEnabled();
	boolean DB_OP_TRACE_ENABLED = DB_OP_LOGGER.isTraceEnabled();
}
