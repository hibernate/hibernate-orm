/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query;

import org.hibernate.HibernateException;

/**
 * Indicates an attempt to perform some operation on a Query that is illegal
 * based on its state, e.g., attempt to call {@link Query#executeUpdate} on a
 * SELECT query.
 *
 * @author Steve Ebersole
 */
public class IllegalQueryOperationException extends HibernateException {
	public IllegalQueryOperationException(String message) {
		super( message );
	}

	public IllegalQueryOperationException(String message, Throwable cause) {
		super( message, cause );
	}
}
