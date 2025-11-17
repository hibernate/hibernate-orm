/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.jdbc.spi;

import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * Provides the "JDBC session" with contextual information it needs during its lifecycle.
 *
 * @author Steve Ebersole
 */
public interface JdbcSessionContext {
	/**
	 * @see org.hibernate.cfg.JdbcSettings#USE_SCROLLABLE_RESULTSET
	 */
	boolean isScrollableResultSetsEnabled();

	/**
	 * @see org.hibernate.cfg.JdbcSettings#USE_GET_GENERATED_KEYS
	 */
	boolean isGetGeneratedKeysEnabled();

	/**
	 * @see org.hibernate.cfg.JdbcSettings#STATEMENT_FETCH_SIZE
	 */
	Integer getFetchSizeOrNull();

	/**
	 * @see org.hibernate.cfg.JdbcSettings#CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT
	 */
	boolean doesConnectionProviderDisableAutoCommit();

	/**
	 * @see org.hibernate.cfg.TransactionSettings#PREFER_USER_TRANSACTION
	 */
	boolean isPreferUserTransaction();

	/**
	 * @see org.hibernate.cfg.TransactionSettings#JTA_TRACK_BY_THREAD
	 */
	boolean isJtaTrackByThread();

	PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode();

	StatementInspector getStatementInspector();

	JpaCompliance getJpaCompliance();

	StatisticsImplementor getStatistics();

	JdbcEventHandler getEventHandler();

	JdbcServices getJdbcServices();

	BatchBuilder getBatchBuilder();

	/**
	 * @see org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner#isActive()
	 *
	 * @return {@code false} if the session factory was already destroyed
	 */
	boolean isActive();
}
