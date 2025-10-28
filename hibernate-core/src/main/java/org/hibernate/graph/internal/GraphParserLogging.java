/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = GraphParserLogging.LOGGER_NAME,
		description = "Logging related to GraphParser for parsing entity graphs from String representations"
)
@Internal
public interface GraphParserLogging {
	String LOGGER_NAME = SubSystemLogging.BASE + ".graph.parsing";

	Logger PARSING_LOGGER = Logger.getLogger( LOGGER_NAME );
}
