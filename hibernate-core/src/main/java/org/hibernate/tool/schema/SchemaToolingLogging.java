/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SchemaToolingLogging {
	public static final String LOGGER_NAME = "org.hibernate.orm.tooling.schema";
	public static final Logger LOGGER = Logger.getLogger( LOGGER_NAME );

	public static final String AST_LOGGER_NAME = LOGGER_NAME + ".AST";
	public static final Logger AST_LOGGER = Logger.getLogger( AST_LOGGER_NAME );

	public static final boolean TRACE_ENABLED = LOGGER.isTraceEnabled();
	public static final boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();

	public static final boolean AST_TRACE_ENABLED = AST_LOGGER.isTraceEnabled();
}
