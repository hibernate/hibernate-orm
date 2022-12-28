/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Enumerates the possible flush modes for execution of a
 * {@link org.hibernate.query.Query}. A "forced" flush mode
 * overrides the {@linkplain Session#getHibernateFlushMode()
 * flush mode of the session}.
 *
 * @author Gavin King
 *
 * @since 6.2
 */
public enum ForcedFlushMode {
	/**
	 * Flush before executing the query.
	 */
	FORCE_FLUSH,
	/**
	 * Do not flush before executing the query.
	 */
	FORCE_NO_FLUSH,
	/**
	 * Let the owning {@link Session session} decide whether
	 * to flush, depending on its {@link FlushMode}.
	 */
	NO_FORCING
}
