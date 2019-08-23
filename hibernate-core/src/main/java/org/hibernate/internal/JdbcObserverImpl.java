/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.sql.Connection;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.jdbc.spi.JdbcObserver;

/**
 * @author Steve Ebersole
 */
public class JdbcObserverImpl implements JdbcObserver {

	private final SharedSessionContractImplementor session;
	private final ConnectionObserverStatsBridge observer;

	public JdbcObserverImpl(SharedSessionContractImplementor session, FastSessionServices fastSessionServices) {
		this.session = session;
		this.observer = fastSessionServices.getDefaultJdbcObserver();
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
		session.getEventListenerManager().jdbcPrepareStatementStart();
	}

	@Override
	public void jdbcPrepareStatementEnd() {
		observer.statementPrepared();
		session.getEventListenerManager().jdbcPrepareStatementEnd();
	}

	@Override
	public void jdbcExecuteStatementStart() {
		session.getEventListenerManager().jdbcExecuteStatementStart();
	}

	@Override
	public void jdbcExecuteStatementEnd() {
		session.getEventListenerManager().jdbcExecuteStatementEnd();
	}

	@Override
	public void jdbcExecuteBatchStart() {
		session.getEventListenerManager().jdbcExecuteBatchStart();
	}

	@Override
	public void jdbcExecuteBatchEnd() {
		session.getEventListenerManager().jdbcExecuteBatchEnd();
	}

	@Override
	public void jdbcReleaseRegistryResourcesStart() {
		session.getJdbcCoordinator().abortBatch();
	}

	@Override
	public void jdbcReleaseRegistryResourcesEnd() {
	}

}
