/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec;

import org.hibernate.HibernateError;

/**
 * Indicates an exception performing execution
 *
 * @author Steve Ebersole
 */
public class ExecutionException extends HibernateError {
	public ExecutionException(String message) {
		this( message, null );
	}

	public ExecutionException(String message, Throwable cause) {
		super( "A problem occurred in the SQL executor : " + message, cause );
	}
}
