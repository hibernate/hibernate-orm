/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.stat.internal;

import java.util.Collections;
import java.util.Set;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.stat.SessionStatistics;

/**
 * @author Gavin King
 */
public class SessionStatisticsImpl implements SessionStatistics {

	private final SessionImplementor session;

	public SessionStatisticsImpl(SessionImplementor session) {
		this.session = session;
	}

	public int getEntityCount() {
		return session.getPersistenceContextInternal().getNumberOfManagedEntities();
	}

	public int getCollectionCount() {
		return session.getPersistenceContextInternal().getCollectionEntriesSize();
	}

	public Set<?> getEntityKeys() {
		return Collections.unmodifiableSet( session.getPersistenceContextInternal().getEntitiesByKey().keySet() );
	}

	public Set<?> getCollectionKeys() {
		return Collections.unmodifiableSet( session.getPersistenceContextInternal().getCollectionsByKey().keySet() );
	}

	public String toString() {
		return "SessionStatistics[" +
				"entity count=" + getEntityCount() +
				",collection count=" + getCollectionCount() +
				']';
	}

}
