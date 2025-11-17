/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
