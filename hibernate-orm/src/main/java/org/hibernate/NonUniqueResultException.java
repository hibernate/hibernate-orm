/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Thrown when the application calls <tt>Query.uniqueResult()</tt> and
 * the query returned more than one result.  Unlike all other Hibernate
 * exceptions, this one is recoverable!
 *
 * @author Gavin King
 */
public class NonUniqueResultException extends HibernateException {
	/**
	 * Constructs a NonUniqueResultException.
	 *
	 * @param resultCount The number of actual results.
	 */
	public NonUniqueResultException(int resultCount) {
		super( "query did not return a unique result: " + resultCount );
	}

}
