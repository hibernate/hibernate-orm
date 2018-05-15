/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * Dedicated logger for rendering a SQL AST
 *
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90005401, max = 90005500 )
public interface SqlAstTreeLogger extends BasicLogger {
	String LOGGER_NAME = "org.hibernate.orm.sql.ast.tree";

	/**
	 * Static access to the logging instance
	 */
	SqlAstTreeLogger INSTANCE = Logger.getMessageLogger( SqlAstTreeLogger.class, LOGGER_NAME );
}
