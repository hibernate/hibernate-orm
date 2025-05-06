/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

/**
 * Dedicated logger for rendering a SQL AST
 *
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90005401, max = 90005500 )
@SubSystemLogging(
		name = SqlAstTreeLogger.LOGGER_NAME,
		description = "Logging related to the processing of SQL AST trees"
)
@Internal
public interface SqlAstTreeLogger extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".sql.ast.tree";

	/**
	 * Static access to the logging instance
	 */
	SqlAstTreeLogger INSTANCE = Logger.getMessageLogger( MethodHandles.lookup(), SqlAstTreeLogger.class, LOGGER_NAME );

}
