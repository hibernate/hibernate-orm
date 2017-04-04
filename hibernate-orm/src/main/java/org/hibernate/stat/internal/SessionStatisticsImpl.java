/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		return session.getPersistenceContext().getNumberOfManagedEntities();
	}
	
	public int getCollectionCount() {
		return session.getPersistenceContext().getCollectionEntries().size();
	}
	
	public Set getEntityKeys() {
		return Collections.unmodifiableSet( session.getPersistenceContext().getEntitiesByKey().keySet() );
	}
	
	public Set getCollectionKeys() {
		return Collections.unmodifiableSet( session.getPersistenceContext().getCollectionsByKey().keySet() );
	}
	
	public String toString() {
		return new StringBuilder()
			.append("SessionStatistics[")
			.append("entity count=").append( getEntityCount() )
			.append(",collection count=").append( getCollectionCount() )
			.append(']')
			.toString();
	}

}
