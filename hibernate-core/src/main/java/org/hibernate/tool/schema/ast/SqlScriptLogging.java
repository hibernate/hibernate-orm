/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.ast;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * @author Steve Ebersole
 */
@ValidIdRange( )
public class SqlScriptLogging {
	public static final String SCRIPT_LOGGER_NAME = "org.hibernate.orm.tooling.schema.script";
	public static final Logger SCRIPT_LOGGER = Logger.getLogger( SCRIPT_LOGGER_NAME );

	public static final boolean TRACE_ENABLED = SCRIPT_LOGGER.isTraceEnabled();
	public static final boolean DEBUG_ENABLED = SCRIPT_LOGGER.isDebugEnabled();

	public static final String AST_LOGGER_NAME = SCRIPT_LOGGER_NAME + ".graph";
	public static final Logger AST_LOGGER = Logger.getLogger( AST_LOGGER_NAME );

	public static final boolean AST_TRACE_ENABLED = AST_LOGGER.isTraceEnabled();

}
