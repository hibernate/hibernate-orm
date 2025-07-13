/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.jdbc.spi;

import java.sql.Connection;
import java.util.function.Supplier;

import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.jboss.logging.Logger;

/**
 * Acts as an observer for various events regarding JDBC interactions and doing one or more of -<ol>
 *     <li>delegating to {@linkplain org.hibernate.stat.spi.StatisticsImplementor}</li>
 *     <li>delegating to {@linkplain SessionEventListenerManager}</li>
 *     <li>logging</li>
 * </ol>
 *
 * @since 7.0
 *
 * @author Steve Ebersole
 */
public class JdbcEventHandler {
	private static final Logger log = Logger.getLogger( JdbcEventHandler.class );

	private final StatisticsImplementor statistics;
	private final SessionEventListenerManager sessionListener;
	private final Supplier<JdbcCoordinator> jdbcCoordinatorSupplier;

	public JdbcEventHandler(
			StatisticsImplementor statistics,
			SessionEventListenerManager sessionListener,
			Supplier<JdbcCoordinator> jdbcCoordinatorSupplier) {
		this.statistics = statistics;
		this.sessionListener = sessionListener;
		this.jdbcCoordinatorSupplier = jdbcCoordinatorSupplier;
	}

	/**
	 * Creates a specialized JdbcEventHandler for use with "temporary Sessions"
	 */
	public JdbcEventHandler() {
		this( null, null, null );

	}

	public void jdbcConnectionAcquisitionStart() {
		// NOTE : Connection acquisition and release events are propagated to
		// SessionEventListenerManager via the JdbcConnectionAccess contracts
		// which is the more proper place, so here we do nothing
	}

	public void jdbcConnectionAcquisitionEnd(Connection connection) {
		// NOTE : Connection acquisition and release events are propagated to
		// SessionEventListenerManager via the JdbcConnectionAccess contracts
		// which is the more proper place, so here we do nothing

		if ( statistics != null && statistics.isStatisticsEnabled() ) {
			statistics.connect();
		}
	}

	public void jdbcConnectionReleaseStart() {
		// NOTE : Connection acquisition and release events are propagated to
		// SessionEventListenerManager via the JdbcConnectionAccess contracts
		// which is the more proper place, so here we do nothing
	}

	public void jdbcConnectionReleaseEnd() {
		// NOTE : Connection acquisition and release events are propagated to
		// SessionEventListenerManager via the JdbcConnectionAccess contracts
		// which is the more proper place, so here we do nothing
	}

	public void jdbcPrepareStatementStart() {
		if ( sessionListener != null ) {
			sessionListener.jdbcPrepareStatementStart();
		}

		if ( statistics != null && statistics.isStatisticsEnabled() ) {
			statistics.prepareStatement();
		}
	}

	public void jdbcPrepareStatementEnd() {
		if ( sessionListener != null ) {
			sessionListener.jdbcPrepareStatementEnd();
		}

		if ( statistics != null && statistics.isStatisticsEnabled() ) {
			statistics.closeStatement();
		}
	}

	public void jdbcExecuteStatementStart() {
		if ( sessionListener != null ) {
			sessionListener.jdbcExecuteStatementStart();
		}
	}

	public void jdbcExecuteStatementEnd() {
		if ( sessionListener != null ) {
			sessionListener.jdbcExecuteStatementEnd();
		}
	}

	public void jdbcExecuteBatchStart() {
		if ( sessionListener != null ) {
			sessionListener.jdbcExecuteBatchStart();
		}
	}

	public void jdbcExecuteBatchEnd() {
		if ( sessionListener != null ) {
			sessionListener.jdbcExecuteBatchEnd();
		}
	}

	public void jdbcReleaseRegistryResourcesStart() {
		if ( jdbcCoordinatorSupplier != null ) {
			final JdbcCoordinator jdbcCoordinator = jdbcCoordinatorSupplier.get();
			if ( jdbcCoordinator != null ) {
				jdbcCoordinator.abortBatch();
			}
		}
	}

	public void jdbcReleaseRegistryResourcesEnd() {
	}

}
