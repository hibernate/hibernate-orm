/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

/**
 * Indicates an attempt to call {@link QueryProducer#createSelectionQuery(String)}
 * with a non-selection query (generally a mutation query)
 *
 * @author Steve Ebersole
 */
public class IllegalSelectQueryException extends IllegalQueryOperationException {
	public IllegalSelectQueryException(String message) {
		super( message );
	}

	public IllegalSelectQueryException(String message, String queryString) {
		super( message, queryString, null );
	}
}
