/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import org.hibernate.QueryException;

/**
 * Indicates a named-query has specified options that are not legal
 *
 * @author Steve Ebersole
 */
public class IllegalNamedQueryOptionsException extends QueryException {
	public IllegalNamedQueryOptionsException(String message) {
		super( message );
	}
}
