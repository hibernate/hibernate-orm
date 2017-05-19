/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume;

import org.hibernate.HibernateException;

/**
 * A problem with the syntax of the SQL AST
 *
 * @author Steve Ebersole
 */
public class SyntaxException extends HibernateException {
	public SyntaxException(String message) {
		super( message );
	}

	public SyntaxException(String message, Throwable cause) {
		super( message, cause );
	}
}
