/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal;

import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = GraphParserLogging.LOGGER_NAME,
		description = "Logging related to Hibernate's `GraphParser` for parsing entity-graphs from String representations"
)
public interface GraphParserLogging {
	String LOGGER_NAME = SubSystemLogging.BASE + ".graph.parsing";

	Logger PARSING_LOGGER = Logger.getLogger( LOGGER_NAME );
}
