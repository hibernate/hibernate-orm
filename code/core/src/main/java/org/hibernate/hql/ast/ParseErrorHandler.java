// $Id: ParseErrorHandler.java 4941 2004-12-11 16:31:18Z pgmjsd $

package org.hibernate.hql.ast;

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
