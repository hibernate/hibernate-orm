/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.jdbc.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

import java.sql.Connection;
import java.sql.SQLException;

import static java.lang.reflect.InvocationHandler.invokeDefault;
import static java.lang.reflect.Proxy.newProxyInstance;

/**
 * Contract for something that controls a {@link JdbcSessionContext}.
 * <p>
 * The term <em>JDBC session</em> is taken from the SQL specification which
 * calls a connection and its associated transaction context a "session".
 *
 * @apiNote The name comes from the design idea of a {@code JdbcSession}
 *           which encapsulates this information, which we will hopefully
 *           get back to later.
 *
 * @author Steve Ebersole
 */
public interface JdbcSessionOwner {

	JdbcSessionContext getJdbcSessionContext();

	JdbcConnectionAccess getJdbcConnectionAccess();

	/**
	 * Obtain the {@link TransactionCoordinator}.
	 *
	 * @return The {@code TransactionCoordinator}
	 */
	TransactionCoordinator getTransactionCoordinator();

	/**
	 * Callback indicating recognition of entering into a transactional
	 * context whether that is explicitly via the Hibernate
	 * {@link org.hibernate.Transaction} API or via registration
	 * of Hibernate's JTA Synchronization impl with a JTA Transaction
	 */
	void startTransactionBoundary();

	/**
	 * An after-begin callback from the coordinator to its owner.
	 */
	void afterTransactionBegin();

	/**
	 * A before-completion callback to the owner.
	 */
	void beforeTransactionCompletion();

	/**
	 * An after-completion callback to the owner.
	 *
	 * @param successful Was the transaction successful?
	 * @param delayed Is this a delayed after transaction completion call (aka after a timeout)?
	 */
	void afterTransactionCompletion(boolean successful, boolean delayed);

	void flushBeforeTransactionCompletion();

	/**
	 * Get the session-level JDBC batch size.
	 * @return session-level JDBC batch size
	 *
	 * @since 5.2
	 */
	Integer getJdbcBatchSize();

	default SqlExceptionHelper getSqlExceptionHelper() {
		return getJdbcSessionContext().getJdbcServices().getSqlExceptionHelper();
	}

	/**
	 * Called after a managed JDBC connection is obtained.
	 * <p>
	 * Sets the schema to the schema belonging to the current tenant if:
	 * <ol>
	 * <li>{@value org.hibernate.cfg.MultiTenancySettings#MULTI_TENANT_SCHEMA_MAPPER} is enabled, and
	 * <li>this session has an active tenant id.
	 * </ol>
	 *
	 * @param connection A JDBC connection which was just acquired
	 *
	 * @since 7.1
	 */
	@Incubating
	void afterObtainConnection(Connection connection) throws SQLException;

	/**
	 * Called right before a managed JDBC connection is released.
	 * <p>
	 * Unset the schema which was set by {@link #afterObtainConnection},
	 * if any.
	 * .
	 * @param connection The JDBC connection which is about to be released
	 *
	 * @since 7.1
	 */
	@Incubating
	void beforeReleaseConnection(Connection connection) throws SQLException;

	/**
	 * Obtain a reference to the {@link EventMonitor}.
	 *
	 * @since 7.0
	 */
	EventMonitor getEventMonitor();

	/**
	 * Obtain a reference to the {@link EventMonitor}
	 * dressed up as an instance of {@link EventManager}.
	 *
	 * @since 6.4
	 *
	 * @deprecated Use {@link #getEventMonitor()}.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	default EventManager getEventManager() {
		final EventMonitor eventMonitor = getEventMonitor();
		return (EventManager)
				newProxyInstance( EventManager.class.getClassLoader(), new Class[]{ EventManager.class },
						(instance, method, arguments)
								-> invokeDefault( eventMonitor, method, arguments) );
	}
}
