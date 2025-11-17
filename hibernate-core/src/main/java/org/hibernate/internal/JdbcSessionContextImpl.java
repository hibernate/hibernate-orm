/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.resource.jdbc.spi.JdbcEventHandler;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * @author Steve Ebersole
 */
class JdbcSessionContextImpl implements JdbcSessionContext {
	private final SessionFactoryImplementor sessionFactory;
	private final StatementInspector statementInspector;
	private final PhysicalConnectionHandlingMode connectionHandlingMode;
	private final JdbcServices jdbcServices;
	private final BatchBuilder batchBuilder;

	private final transient JdbcEventHandler jdbcEventHandler;

	JdbcSessionContextImpl(
			SessionFactoryImplementor sessionFactory,
			StatementInspector statementInspector,
			PhysicalConnectionHandlingMode connectionHandlingMode,
			JdbcServices jdbcServices,
			BatchBuilder batchBuilder,
			JdbcEventHandler jdbcEventHandler) {
		this.sessionFactory = sessionFactory;
		this.statementInspector = statementInspector;
		this.connectionHandlingMode = connectionHandlingMode;
		this.jdbcServices = jdbcServices;
		this.batchBuilder = batchBuilder;
		this.jdbcEventHandler = jdbcEventHandler;

		if ( statementInspector == null ) {
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
	public Integer getFetchSizeOrNull() {
		return settings().getJdbcFetchSize();
	}

	@Override
	public JpaCompliance getJpaCompliance() {
		return settings().getJpaCompliance();
	}

	@Override
	public boolean isPreferUserTransaction() {
		return settings().isPreferUserTransaction();
	}

	@Override
	public boolean isJtaTrackByThread() {
		return settings().isJtaTrackByThread();
	}

	@Override
	public PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode() {
		return connectionHandlingMode;
	}

	@Override
	public boolean doesConnectionProviderDisableAutoCommit() {
		return settings().doesConnectionProviderDisableAutoCommit();
	}

	@Override
	public StatementInspector getStatementInspector() {
		return statementInspector;
	}

	@Override
	public JdbcEventHandler getEventHandler() {
		return jdbcEventHandler;
	}

	private SessionFactoryOptions settings() {
		return sessionFactory.getSessionFactoryOptions();
	}

	@Override
	public JdbcServices getJdbcServices() {
		return jdbcServices;
	}

	@Override
	public BatchBuilder getBatchBuilder() {
		return batchBuilder;
	}

	@Override
	public boolean isActive() {
		return !sessionFactory.isClosed();
	}

	@Override
	public StatisticsImplementor getStatistics() {
		return sessionFactory.getStatistics();
	}
}
