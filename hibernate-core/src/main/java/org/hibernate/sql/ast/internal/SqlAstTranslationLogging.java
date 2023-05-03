/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.ast.internal;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = "SqlAstTranslationLogging.SQL_AST_TRANSLATE_LOGGER_NAME",
		description = "Logging related to the translation of SQL AST trees"
)
@Internal
public interface SqlAstTranslationLogging {
	String SQL_AST_TRANSLATE_LOGGER_NAME = SubSystemLogging.BASE + ".sql.ast.translate";

	Logger SQL_AST_TRANSLATE_LOGGER = Logger.getLogger( SQL_AST_TRANSLATE_LOGGER_NAME );

	boolean SQL_AST_TRANSLATE_DEBUG_ENABLED = SQL_AST_TRANSLATE_LOGGER.isDebugEnabled();
	boolean SQL_AST_TRANSLATE_TRACE_ENABLED = SQL_AST_TRANSLATE_LOGGER.isTraceEnabled();
}
