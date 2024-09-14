/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
