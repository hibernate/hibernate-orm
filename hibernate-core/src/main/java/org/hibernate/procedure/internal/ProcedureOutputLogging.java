/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

/// Logging related to procedure output handling.
///
/// @author Steve Ebersole
@SubSystemLogging(
		name = "ProcedureOutputLogging.PROC_OUTPUT_LOGGER_NAME",
		description = "Logging related to ProcedureCall output handling"
)
@Internal
public interface ProcedureOutputLogging {
	String PROC_OUTPUT_LOGGER_NAME = SubSystemLogging.BASE + ".procedure.output";

	Logger PROC_OUTPUT_LOGGER = Logger.getLogger( PROC_OUTPUT_LOGGER_NAME );

	boolean something_DEBUG_ENABLED = PROC_OUTPUT_LOGGER.isDebugEnabled();
	boolean something_TRACE_ENABLED = PROC_OUTPUT_LOGGER.isTraceEnabled();
}
