/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

/**
 * Enumerates the possible flush modes for execution of a
 * {@link org.hibernate.query.Query}. An explicitly-specified
 * {@linkplain Query#setQueryFlushMode(QueryFlushMode)
 * query-level flush mode} overrides the current
 * {@linkplain org.hibernate.Session#getHibernateFlushMode()
 * flush mode of the session}.
 *
 * @since 7.0
 *
 * @see CommonQueryContract#setQueryFlushMode(QueryFlushMode)
 * @see org.hibernate.annotations.NamedQuery#flush
 * @see org.hibernate.annotations.NamedNativeQuery#flush
 *
 * @author Gavin King
 */
public enum QueryFlushMode {
	/**
	 * Flush before executing the query.
	 */
	FLUSH,
	/**
	 * Do not flush before executing the query.
	 */
	NO_FLUSH,
	/**
	 * Let the owning {@link org.hibernate.Session session}
	 * decide whether to flush, depending on its current
	 * {@link org.hibernate.FlushMode}.
	 *
	 * @see org.hibernate.Session#getFlushMode()
	 */
	DEFAULT
}
