/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume;

import org.hibernate.HibernateException;

/**
 * A semantic error in the SQL AST
 *
 * @author Steve Ebersole
 */
public class SemanticException extends HibernateException {
	public SemanticException(String message) {
		super( message );
	}

	public SemanticException(String message, Throwable cause) {
		super( message, cause );
	}
}
