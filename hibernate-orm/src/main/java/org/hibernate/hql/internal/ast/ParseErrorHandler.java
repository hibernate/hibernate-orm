/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast;
import org.hibernate.QueryException;


/**
 * Defines the behavior of an error handler for the HQL parsers.
 * User: josh
 * Date: Dec 6, 2003
 * Time: 12:20:43 PM
 */
public interface ParseErrorHandler extends ErrorReporter {

	int getErrorCount();

	// --Commented out by Inspection (12/11/04 10:56 AM): int getWarningCount();

	void throwQueryException() throws QueryException;
}
