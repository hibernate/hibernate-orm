/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

/**
 * Indicates a problem with a path expression in HQL/JPQL.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.query.sqm.UnknownPathException
 */
public class PathException extends SemanticException {
	public PathException(String message) {
		super( message );
	}

	public PathException(String message, Exception cause) {
		super( message, cause );
	}

	public PathException(String message, String hql, Exception cause) {
		super(message, hql, cause);
	}
}
