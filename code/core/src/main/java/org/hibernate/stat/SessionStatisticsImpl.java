//$Id: SessionStatisticsImpl.java 7688 2005-07-29 21:43:18Z epbernard $
package org.hibernate.stat;

import java.util.Collections;
import java.util.Set;

import org.hibernate.engine.SessionImplementor;

/**
 * @author Gavin King
 */
public class SessionStatisticsImpl implements SessionStatistics {

	private final SessionImplementor session;
	
	public SessionStatisticsImpl(SessionImplementor session) {
		this.session = session;
	}

	public int getEntityCount() {
		return session.getPersistenceContext().getEntityEntries().size();
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
		return new StringBuffer()
			.append("SessionStatistics[")
			.append("entity count=").append( getEntityCount() )
			.append("collection count=").append( getCollectionCount() )
			.append(']')
			.toString();
	}

}
