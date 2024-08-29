/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.jpa.AvailableHints;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.jpa.LegacySpecHints;
import org.hibernate.jpa.SpecHints;
import org.hibernate.query.Query;

/**
 * List of hints that may be passed to {@link jakarta.persistence.Query#setHint(String, Object)}
 * to control execution of a query. Each of these hints corresponds to a typesafe operation of
 * the {@link Query} interface, and so hints are only necessary for programs
 * working with the JPA APIs.
 *
 * @see AvailableHints
 *
 * @deprecated Use {@link AvailableHints} instead
 */
@SuppressWarnings("unused")
@Deprecated(since = "6.0")
public final class QueryHints {
	/**
	 * Disallow instantiation.
	 */
	private QueryHints() {
	}

	/**
	 * @see HibernateHints#HINT_READ_ONLY
	 */
	public static final String READ_ONLY = HibernateHints.HINT_READ_ONLY;

	/**
	 * @see HibernateHints#HINT_CACHEABLE
	 */
	public static final String CACHEABLE = HibernateHints.HINT_CACHEABLE;

	/**
	 * @see HibernateHints#HINT_CACHE_MODE
	 */
	public static final String CACHE_MODE = HibernateHints.HINT_CACHE_MODE;

	/**
	 * @see HibernateHints#HINT_CACHE_REGION
	 */
	public static final String CACHE_REGION = HibernateHints.HINT_CACHE_REGION;

	/**
	 * @see HibernateHints#HINT_COMMENT
	 */
	public static final String COMMENT = HibernateHints.HINT_COMMENT;

	/**
	 * @see HibernateHints#HINT_FETCH_SIZE
	 */
	public static final String FETCH_SIZE = HibernateHints.HINT_FETCH_SIZE;

	/**
	 * @see HibernateHints#HINT_FLUSH_MODE
	 */
	public static final String FLUSH_MODE = HibernateHints.HINT_FLUSH_MODE;

	/**
	 * @see HibernateHints#HINT_TIMEOUT
	 */
	public static final String TIMEOUT_HIBERNATE = HibernateHints.HINT_TIMEOUT;

	/**
	 * @see org.hibernate.jpa.SpecHints#HINT_SPEC_QUERY_TIMEOUT
	 */
	public static final String TIMEOUT_JAKARTA_JPA = SpecHints.HINT_SPEC_QUERY_TIMEOUT;

	/**
	 * @see HibernateHints#HINT_NATIVE_LOCK_MODE
	 */
	public static final String NATIVE_LOCKMODE = HibernateHints.HINT_NATIVE_LOCK_MODE;

	/**
	 * @see HibernateHints#HINT_FOLLOW_ON_LOCKING
	 */
	public static final String FOLLOW_ON_LOCKING = HibernateHints.HINT_FOLLOW_ON_LOCKING;

	/**
	 * @see HibernateHints#HINT_NATIVE_SPACES
	 */
	public static final String NATIVE_SPACES = HibernateHints.HINT_NATIVE_SPACES;

	/**
	 * @see HibernateHints#HINT_CALLABLE_FUNCTION
	 *
	 * @deprecated Calling stored-procedures and functions via
	 * {@link org.hibernate.query.NativeQuery} is no longer supported.
	 * Use {@link org.hibernate.procedure.ProcedureCall} or
	 * {@link jakarta.persistence.StoredProcedureQuery} instead.
	 */
	@Deprecated(since="6")
	public static final String CALLABLE_FUNCTION = HibernateHints.HINT_CALLABLE_FUNCTION;

	/**
	 * @see org.hibernate.jpa.SpecHints#HINT_SPEC_QUERY_TIMEOUT
	 */
	public static final String TIMEOUT_JPA = LegacySpecHints.HINT_JAVAEE_QUERY_TIMEOUT;
}
