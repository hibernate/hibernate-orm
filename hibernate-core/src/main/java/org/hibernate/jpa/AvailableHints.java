/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa;

import java.util.Map;
import java.util.Set;

import org.hibernate.jpa.internal.HintsCollector;

import jakarta.persistence.LockModeType;

/**
 * Combined set of Hibernate and Jakarta Persistence hints.
 *
 * @see jakarta.persistence.EntityManager#setProperty(String, Object)
 * @see jakarta.persistence.EntityManager#find(Class, Object, Map)
 * @see jakarta.persistence.EntityManager#find(Class, Object, LockModeType, Map)
 * @see jakarta.persistence.EntityManager#lock(Object, LockModeType, Map)
 * @see jakarta.persistence.Query#setHint
 *
 * @author Steve Ebersole
 */
public interface AvailableHints extends HibernateHints, SpecHints {
	static Set<String> getDefinedHints() {
		return HintsCollector.getDefinedHints();
	}
}
