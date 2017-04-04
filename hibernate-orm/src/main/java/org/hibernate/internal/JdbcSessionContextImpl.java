/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.jdbc.spi.JdbcObserver;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Steve Ebersole
 */
public class JdbcSessionContextImpl implements JdbcSessionContext {
	private final SessionFactoryImplementor sessionFactory;
	private final StatementInspector statementInspector;
	private final PhysicalConnectionHandlingMode connectionHandlingMode;

	private final transient ServiceRegistry serviceRegistry;
	private final transient JdbcObserver jdbcObserver;

	public JdbcSessionContextImpl(SharedSessionContractImplementor session, StatementInspector statementInspector) {
		this.sessionFactory = session.getFactory();
		this.statementInspector = statementInspector;
		this.connectionHandlingMode = settings().getPhysicalConnectionHandlingMode();
		this.serviceRegistry = sessionFactory.getServiceRegistry();
		this.jdbcObserver = new JdbcObserverImpl( session );

		if ( this.statementInspector == null ) {
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
		return statementInspector;
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
