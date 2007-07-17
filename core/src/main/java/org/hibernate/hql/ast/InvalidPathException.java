// $Id: InvalidPathException.java 4903 2004-12-07 07:53:10Z pgmjsd $
package org.hibernate.hql.ast;

import antlr.SemanticException;

/**
 * Exception thrown when an invalid path is found in a query.
 *
 * @author josh Dec 5, 2004 7:05:34 PM
 */
public class InvalidPathException extends SemanticException {
	public InvalidPathException(String s) {
		super( s );
	}
}
