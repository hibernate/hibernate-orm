/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import org.hibernate.QueryException;

/**
 * Occurs when a named query is requested, and there is no known
 * HQL or native SQL query registered under the given name.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.query.named.NamedObjectRepository
 * @see jakarta.persistence.NamedQuery
 * @see jakarta.persistence.NamedNativeQuery
 * @see org.hibernate.annotations.NamedQuery
 * @see org.hibernate.annotations.NamedNativeQuery
 */
public class UnknownNamedQueryException extends QueryException {
	public UnknownNamedQueryException(String queryName) {
		super( "No query named '" + queryName + "'" );
	}
}
