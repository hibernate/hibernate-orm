/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation;

import org.hibernate.CacheMode;
import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Incubating;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;

import java.sql.Connection;
import java.util.function.UnaryOperator;

/**
 * Common options for builders of {@linkplain Session stateful}
 * and {@linkplain StatelessSession stateless} sessions.
 *
 * @since 7.2
 *
 * @author Steve Ebersole
 */
@Incubating
public interface CommonBuilder {

	/**
	 * Adds a specific connection to the session options.
	 *
	 * @param connection The connection to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonBuilder connection(Connection connection);

	/**
	 * Specifies the connection handling modes for the session.
	 * <p>
	 * Note that if {@link ConnectionAcquisitionMode#IMMEDIATELY} is specified,
	 * then the release mode must be {@link ConnectionReleaseMode#ON_CLOSE}.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @since 7.0
	 */
	CommonBuilder connectionHandling(ConnectionAcquisitionMode acquisitionMode, ConnectionReleaseMode releaseMode);

	/**
	 * Adds a specific interceptor to the session options.
	 *
	 * @param interceptor The interceptor to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonBuilder interceptor(Interceptor interceptor);

	/**
	 * Signifies that no {@link Interceptor} should be used.
	 * <p>
	 * By default, if no {@code Interceptor} is explicitly specified, the
	 * {@code Interceptor} associated with the {@link SessionFactory} is
	 * inherited by the new session.
	 * <p>
	 * Calling {@link #interceptor(Interceptor)} with null has the same effect.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonBuilder noInterceptor();

	/**
	 * Applies the given statement inspection function to the session.
	 *
	 * @param operator An operator which accepts a SQL string, returning
	 * a processed SQL string to be used by Hibernate instead of the given
	 * original SQL. The operator may simply return the original SQL.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonBuilder statementInspector(UnaryOperator<String> operator);

	/**
	 * Signifies that no SQL statement inspector should be used.
	 * <p>
	 * By default, if no inspector is explicitly specified, the
	 * inspector associated with the {@link SessionFactory} is
	 * inherited by the new session.
	 * <p>
	 * Calling {@link #interceptor(Interceptor)} with null has the same effect.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonBuilder noStatementInspector();

	/**
	 * Define the tenant identifier to be associated with the opened session.
	 *
	 * @param tenantIdentifier The tenant identifier.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonBuilder tenantIdentifier(Object tenantIdentifier);

	/**
	 * Specify a {@linkplain Session#isDefaultReadOnly read-only mode}
	 * for the session. If a session is created in read-only mode, then
	 * {@link Connection#setReadOnly} is called when a JDBC connection
	 * is obtained.
	 * <p>
	 * Furthermore, if read/write replication is in use, then:
	 * <ul>
	 * <li>a read-only session will connect to a read-only replica, but
	 * <li>a non-read-only session will connect to a writable replica.
	 * </ul>
	 * <p>
	 * When read/write replication is in use, it's strongly recommended
	 * that the session be created with the {@linkplain #initialCacheMode
	 * initial cache mode} set to {@link CacheMode#GET}, to avoid writing
	 * stale data read from a read-only replica to the second-level cache.
	 * Hibernate cannot possibly guarantee that data read from a read-only
	 * replica is up to date.
	 * <p>
	 * When read/write replication is in use, it's possible that an item
	 * read from the second-level cache might refer to data which does not
	 * yet exist in the read-only replica. In this situation, an exception
	 * occurs when the association is fetched. To completely avoid this
	 * possibility, the {@linkplain #initialCacheMode initial cache mode}
	 * must be set to {@link CacheMode#IGNORE}. However, it's also usually
	 * possible to structure data access code in a way which eliminates
	 * this possibility.
	 * <p>
	 * If a session is created in read-only mode, then it cannot be
	 * changed to read-write mode, and any call to
	 * {@link Session#setDefaultReadOnly(boolean)} with fail. On the
	 * other hand, if a session is created in read-write mode, then it
	 * may later be switched to read-only mode, but all database access
	 * is directed to the writable replica.
	 *
	 * @return {@code this}, for method chaining
	 * @since 7.2
	 *
	 * @see org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider#getReadOnlyConnection(Object)
	 * @see org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider#releaseReadOnlyConnection(Object, Connection)
	 */
	@Incubating
	CommonBuilder readOnly(boolean readOnly);

	/**
	 * Specify the initial {@link CacheMode} for the session.
	 *
	 * @return {@code this}, for method chaining
	 * @since 7.2
	 *
	 * @see SharedSessionContract#getCacheMode()
	 */
	CommonBuilder initialCacheMode(CacheMode cacheMode);
}
