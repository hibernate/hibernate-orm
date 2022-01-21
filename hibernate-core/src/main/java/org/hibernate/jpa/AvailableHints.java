/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.LockModeType;

/**
 * Combined set of Hibernate and Jakarta Persistence hints
 *
 * @see jakarta.persistence.EntityManager#setProperty(String, Object)
 * @see jakarta.persistence.EntityManager#find(Class, Object, Map)
 * @see jakarta.persistence.EntityManager#find(Class, Object, LockModeType, Map)
 * @see jakarta.persistence.EntityManager#lock(Object, LockModeType, Map)
 * @see jakarta.persistence.Query#setHint
 *
 * @author Steve Ebersole
 */
public class AvailableHints implements HibernateHints, SpecHints {
	private static final Set<String> HINTS = buildHintsSet();

	public static Set<String> getDefinedHints() {
		return HINTS;
	}

	private static Set<String> buildHintsSet() {
		final HashSet<String> hints = new HashSet<>();

		hints.add( HibernateHints.HINT_TIMEOUT );
		hints.add( HibernateHints.HINT_READ_ONLY );
		hints.add( HibernateHints.HINT_FLUSH_MODE );
		hints.add( HibernateHints.HINT_CACHEABLE );
		hints.add( HibernateHints.HINT_CACHE_MODE );
		hints.add( HibernateHints.HINT_CACHE_REGION );
		hints.add( HibernateHints.HINT_FETCH_SIZE );
		hints.add( HibernateHints.HINT_COMMENT );
		hints.add( HibernateHints.HINT_NATIVE_SPACES );
		hints.add( HibernateHints.HINT_NATIVE_LOCK_MODE );

		hints.add( SpecHints.HINT_SPEC_QUERY_TIMEOUT );
		hints.add( SpecHints.HINT_SPEC_FETCH_GRAPH );
		hints.add( SpecHints.HINT_SPEC_LOAD_GRAPH );

		hints.add( LegacySpecHints.HINT_JAVAEE_QUERY_TIMEOUT );
		hints.add( LegacySpecHints.HINT_JAVAEE_FETCH_GRAPH );
		hints.add( LegacySpecHints.HINT_JAVAEE_LOAD_GRAPH );

		return java.util.Collections.unmodifiableSet( hints );
	}


}
