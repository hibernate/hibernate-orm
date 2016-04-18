package org.hibernate.internal;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.jdbc.spi.JdbcObserver;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Steve Ebersole
 */
public class JdbcSessionContextImpl implements JdbcSessionContext {
	private final SessionFactoryImpl sessionFactory;
	private final StatementInspector inspector;
	private final PhysicalConnectionHandlingMode connectionHandlingMode;

	private final transient ServiceRegistry serviceRegistry;
	private final transient JdbcObserver jdbcObserver;

	public JdbcSessionContextImpl(SessionFactoryImpl sessionFactory, StatementInspector inspector) {
		this.sessionFactory = sessionFactory;
		this.inspector = inspector;
		this.connectionHandlingMode = settings().getPhysicalConnectionHandlingMode();
		this.serviceRegistry = sessionFactory.getServiceRegistry();
		this.jdbcObserver = new AbstractSessionImpl.JdbcObserverImpl();

		if ( inspector == null ) {
			throw new IllegalArgumentException( "StatementInspector cannot be null" );
		}
	}

	@Override
	public boolean isScrollableResultSetsEnabled() {
		return settings().isScrollableResultSetsEnabled();
	}

	@Override
	public boolean isGetGeneratedKeysEnabled() {
		return settings().isGetGeneratedKeysEnabled();
	}

	@Override
	public int getFetchSize() {
		return settings().getJdbcFetchSize();
	}

	@Override
	public PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode() {
		return connectionHandlingMode;
	}

	@Override
	public ConnectionReleaseMode getConnectionReleaseMode() {
		return connectionHandlingMode.getReleaseMode();
	}

	@Override
	public ConnectionAcquisitionMode getConnectionAcquisitionMode() {
		return connectionHandlingMode.getAcquisitionMode();
	}

	@Override
	public StatementInspector getStatementInspector() {
		return inspector;
	}

	@Override
	public JdbcObserver getObserver() {
		return this.jdbcObserver;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return this.sessionFactory;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return this.serviceRegistry;
	}

	private SessionFactoryOptions settings() {
		return this.sessionFactory.getSessionFactoryOptions();
	}
}
