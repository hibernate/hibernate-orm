/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import org.hibernate.QueryException;

/**
 * Indicates a request for a named-query when no query is
 * registered under that name
 *
 * @author Steve Ebersole
 */
public class UnknownNamedQueryException extends QueryException {
	public UnknownNamedQueryException(String queryName) {
		super( "No query is registered under the name '" + queryName + "'" );
	}
}
