/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import org.hibernate.HibernateException;

/**
 * Indicates a problem with requested typed-Query result-type (e.g., JPA's {@link javax.persistence.TypedQuery})
 *
 * @author Steve Ebersole
 */
public class QueryTypeMismatchException extends HibernateException {
	public QueryTypeMismatchException(String message) {
		super( message );
	}

	public QueryTypeMismatchException(String message, Throwable cause) {
		super( message, cause );
	}
}
