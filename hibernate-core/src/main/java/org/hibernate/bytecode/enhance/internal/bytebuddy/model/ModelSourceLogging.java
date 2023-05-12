/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = "ModelSourceLogging.MODEL_SOURCE_LOGGER_NAME",
		description = "Logging related to interpreting the domain model sources"
)
@Internal
public interface ModelSourceLogging {
	String MODEL_SOURCE_LOGGER_NAME = SubSystemLogging.BASE + ".boot.model.source";

	Logger MODEL_SOURCE_LOGGER = Logger.getLogger( MODEL_SOURCE_LOGGER_NAME );

	boolean MODEL_SOURCE_DEBUG_ENABLED = MODEL_SOURCE_LOGGER.isDebugEnabled();
	boolean MODEL_SOURCE_TRACE_ENABLED = MODEL_SOURCE_LOGGER.isTraceEnabled();
}
