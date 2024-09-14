/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.results;

import org.hibernate.HibernateException;

/**
 * Indicates that a column defined as part of a SQL ResultSet mapping was not part
 * of the query's ResultSet
 *
 * @see ResultSetMapping
 *
 * @author Steve Ebersole
 */
public class MissingSqlSelectionException extends HibernateException {
	public MissingSqlSelectionException(String message) {
		super( message );
	}

	public MissingSqlSelectionException(String message, Throwable cause) {
		super( message, cause );
	}
}
