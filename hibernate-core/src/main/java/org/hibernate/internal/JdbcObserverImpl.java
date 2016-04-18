package org.hibernate.internal;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.jdbc.spi.ConnectionObserver;
import org.hibernate.resource.jdbc.spi.JdbcObserver;

/**
 * @author Steve Ebersole
 */
public class JdbcObserverImpl implements JdbcObserver {

	private final transient List<ConnectionObserver> observers;

	public JdbcObserverImpl() {
		this.observers = new ArrayList<>();
		this.observers.add( new ConnectionObserverStatsBridge( factory ) );
	}

	@Override
	public void jdbcConnectionAcquisitionStart() {

	}

	@Override
	public void jdbcConnectionAcquisitionEnd(Connection connection) {
		for ( ConnectionObserver observer : observers ) {
			observer.physicalConnectionObtained( connection );
		}
	}

	@Override
	public void jdbcConnectionReleaseStart() {

	}

	@Override
	public void jdbcConnectionReleaseEnd() {
		for ( ConnectionObserver observer : observers ) {
			observer.physicalConnectionReleased();
		}
	}

	@Override
	public void jdbcPrepareStatementStart() {
		getEventListenerManager().jdbcPrepareStatementStart();
	}

	@Override
	public void jdbcPrepareStatementEnd() {
		for ( ConnectionObserver observer : observers ) {
			observer.statementPrepared();
		}
		getEventListenerManager().jdbcPrepareStatementEnd();
	}

	@Override
	public void jdbcExecuteStatementStart() {
		getEventListenerManager().jdbcExecuteStatementStart();
	}

	@Override
	public void jdbcExecuteStatementEnd() {
		getEventListenerManager().jdbcExecuteStatementEnd();
	}

	@Override
	public void jdbcExecuteBatchStart() {
		getEventListenerManager().jdbcExecuteBatchStart();
	}

	@Override
	public void jdbcExecuteBatchEnd() {
		getEventListenerManager().jdbcExecuteBatchEnd();
	}
}
