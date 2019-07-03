/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.io.Serializable;
import java.sql.Connection;

import org.hibernate.engine.jdbc.spi.ConnectionObserver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * @author Steve Ebersole
 */
public class ConnectionObserverStatsBridge implements ConnectionObserver, Serializable {
	private final SessionFactoryImplementor sessionFactory;

	public ConnectionObserverStatsBridge(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public void physicalConnectionObtained(Connection connection) {
		final StatisticsImplementor statistics = sessionFactory.getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.connect();
		}
	}

	@Override
	public void physicalConnectionReleased() {
	}

	@Override
	public void logicalConnectionClosed() {
	}

	@Override
	public void statementPrepared() {
		final StatisticsImplementor statistics = sessionFactory.getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.prepareStatement();
		}
	}
}
