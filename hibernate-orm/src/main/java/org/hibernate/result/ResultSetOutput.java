/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.result;

import java.util.List;

/**
 * Models a return that is a result set.
 *
 * @author Steve Ebersole
 */
public interface ResultSetOutput extends Output {
	/**
	 * Consume the underlying {@link java.sql.ResultSet} and return the resulting List.
	 *
	 * @return The consumed ResultSet values.
	 */
	public List getResultList();

	/**
	 * Consume the underlying {@link java.sql.ResultSet} with the expectation that there is just a single level of
	 * root returns.
	 *
	 * @return The single result.
	 */
	public Object getSingleResult();
}
