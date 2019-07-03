/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public interface SqlTreeCreationLogger {
	String LOGGER_NAME = "org.hibernate.orm.sql.ast.create";

	Logger LOGGER = Logger.getLogger( LOGGER_NAME );

	boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();
	boolean TRACE_ENABLED = LOGGER.isTraceEnabled();
}
