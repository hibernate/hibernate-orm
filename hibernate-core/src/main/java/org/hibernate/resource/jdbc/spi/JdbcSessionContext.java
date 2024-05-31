/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.jdbc.spi;

import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * Provides the "JDBC session" with contextual information it needs during its lifecycle.
 *
 * @author Steve Ebersole
 */
public interface JdbcSessionContext {
	/**
	 * @see org.hibernate.cfg.AvailableSettings#USE_SCROLLABLE_RESULTSET
	 */
	boolean isScrollableResultSetsEnabled();

	/**
	 * @see org.hibernate.cfg.AvailableSettings#USE_GET_GENERATED_KEYS
	 */
	boolean isGetGeneratedKeysEnabled();

	/**
	 * @see org.hibernate.cfg.AvailableSettings#STATEMENT_FETCH_SIZE
	 */
	Integer getFetchSizeOrNull();

	/**
	 * @see org.hibernate.cfg.AvailableSettings#CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT
	 */
	boolean doesConnectionProviderDisableAutoCommit();

	/**
	 * @see org.hibernate.cfg.AvailableSettings#PREFER_USER_TRANSACTION
	 */
	boolean isPreferUserTransaction();

	/**
	 * @see org.hibernate.cfg.AvailableSettings#JTA_TRACK_BY_THREAD
	 */
	boolean isJtaTrackByThread();

	PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode();

	StatementInspector getStatementInspector();

	JpaCompliance getJpaCompliance();

	StatisticsImplementor getStatistics();

	/**
	 * @deprecated since {@link JdbcObserver} is deprecated
	 */
	@Deprecated(forRemoval = true)
	JdbcObserver getObserver();

	/**
	* Retrieve the session factory for this environment.
	 *
	 * @deprecated exposing this here seems to kinda defeat the purpose of this SPI
	*/
	@Deprecated(since = "6.2")
	SessionFactoryImplementor getSessionFactory();

	/**
	 * Retrieve the service registry.
	 *
	 * @deprecated this is no longer called, and unnecessary, since the needed
	 *             services are now available via {@link #getJdbcServices()}
	 */
	@Deprecated(since = "6.2")
	ServiceRegistry getServiceRegistry();

	JdbcServices getJdbcServices();

	BatchBuilder getBatchBuilder();

	/**
	 * @see org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner#isActive()
	 *
	 * @return {@code false} if the session factory was already destroyed
	 */
	boolean isActive();
}
