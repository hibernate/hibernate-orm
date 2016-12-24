/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec;

import org.hibernate.HibernateError;

/**
 * @author Steve Ebersole
 */
public class ExecutionException extends HibernateError {
	public ExecutionException(String message) {
		this( message, null );
	}

	public ExecutionException(Throwable cause) {
		this( "uncategorized", cause );
	}

	public ExecutionException(String message, Throwable cause) {
		super( "A problem occurred in the SQL-AST executor - this is likely a bug in Hibernate : " + message, cause );
	}
}
