/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.sql.Connection;

import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.jdbc.spi.JdbcObserver;

/**
 * @author Steve Ebersole
 */
public final class JdbcObserverImpl implements JdbcObserver {

	private final ConnectionObserverStatsBridge observer;
	private final SessionEventListenerManager eventListenerManager;
	private final SharedSessionContractImplementor session;

	public JdbcObserverImpl(SharedSessionContractImplementor session, FastSessionServices fastSessionServices) {
		this.session = session;
		this.observer = fastSessionServices.getDefaultJdbcObserver();
		this.eventListenerManager = session.getEventListenerManager();
	}

	@Override
	public void jdbcConnectionAcquisitionStart() {
	}

	@Override
	public void jdbcConnectionAcquisitionEnd(Connection connection) {
		observer.physicalConnectionObtained( connection );
	}

	@Override
	public void jdbcConnectionReleaseStart() {
	}

	@Override
	public void jdbcConnectionReleaseEnd() {
		observer.physicalConnectionReleased();
	}

	@Override
	public void jdbcPrepareStatementStart() {
		eventListenerManager.jdbcPrepareStatementStart();
	}

	@Override
	public void jdbcPrepareStatementEnd() {
		observer.statementPrepared();
		eventListenerManager.jdbcPrepareStatementEnd();
	}

	@Override
	public void jdbcExecuteStatementStart() {
		eventListenerManager.jdbcExecuteStatementStart();
	}

	@Override
	public void jdbcExecuteStatementEnd() {
		eventListenerManager.jdbcExecuteStatementEnd();
	}

	@Override
	public void jdbcExecuteBatchStart() {
		eventListenerManager.jdbcExecuteBatchStart();
	}

	@Override
	public void jdbcExecuteBatchEnd() {
		eventListenerManager.jdbcExecuteBatchEnd();
	}

	@Override
	public void jdbcReleaseRegistryResourcesStart() {
		session.getJdbcCoordinator().abortBatch();
	}

	@Override
	public void jdbcReleaseRegistryResourcesEnd() {
	}

}
