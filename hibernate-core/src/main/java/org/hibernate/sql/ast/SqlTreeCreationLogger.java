/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast;

import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = SqlTreeCreationLogger.LOGGER_NAME,
		description = "Logging related to the creation of SQL AST trees"
)
public interface SqlTreeCreationLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".sql.ast.create";

	Logger LOGGER = Logger.getLogger( LOGGER_NAME );
}
