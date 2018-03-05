/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.hint;

/**
 * Contract defining how query hints get applied.
 *
 * @author Vlad Mihalcea
 */
public interface QueryHintHandler {

	/**
	 * Add query hints to the given query.
	 *
	 * @param query original query
	 * @param hints hints to be applied
	 * @return query with hints
	 */
	String addQueryHints(String query, String hints);
}
