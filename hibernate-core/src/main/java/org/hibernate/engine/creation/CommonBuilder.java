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
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;

import java.sql.Connection;
import java.util.TimeZone;
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
	/// Open the session using the specified options.
	SharedSessionContract open();

	/// Adds a specific connection to be used to the session options.
	///
	/// @param connection The connection to use.
	/// @return {@code this}, for method chaining
	CommonBuilder connection(Connection connection);

	/// Specifies the connection handling modes for the session.
	///
	/// @apiNote If [ConnectionAcquisitionMode#IMMEDIATELY] is specified,
	/// then the release mode must be [ConnectionReleaseMode#ON_CLOSE].
	///
	/// @return `this`, for method chaining
	///
	/// @since 7.0
	CommonBuilder connectionHandling(ConnectionAcquisitionMode acquisitionMode, ConnectionReleaseMode releaseMode);

	/// Adds a specific interceptor to the session options.
	///
	/// @param interceptor The interceptor to use.
	/// @return `this`, for method chaining
	CommonBuilder interceptor(Interceptor interceptor);

	/// Specifies that no {@link Interceptor} should be used.  This indicates to
	/// ignore both (if either) the [interceptor][org.hibernate.cfg.SessionEventSettings#INTERCEPTOR]
	/// and [session-scoped interceptor][org.hibernate.cfg.SessionEventSettings#SESSION_SCOPED_INTERCEPTOR]
	/// associated with the `SessionFactory`
	///
	/// @return `this`, for method chaining
	///
	/// @apiNote Calling [#interceptor(Interceptor)] with `null` has the same effect
	CommonBuilder noInterceptor();

	/// Specifies that no [session-scoped interceptor][org.hibernate.cfg.SessionEventSettings#SESSION_SCOPED_INTERCEPTOR]
	/// should be used for the session.  If the `SessionFactory` has a configured
	/// [interceptor][org.hibernate.cfg.SessionEventSettings#INTERCEPTOR], it will still be used.
	///
	/// @return `this`, for method chaining
	///
	/// @see #noInterceptor
	///
	/// @apiNote Unlike [#noInterceptor], this operation does not disable use of an
	/// [interceptor][org.hibernate.cfg.SessionEventSettings#INTERCEPTOR] associated with the `SessionFactory`.
	///
	/// @since 7.2
	CommonBuilder noSessionInterceptorCreation();

	/// Applies the given statement inspection function to the session.
	///
	/// @param operator An operator which accepts a SQL string, returning
	/// a processed SQL string to be used by Hibernate instead.  The
	/// operator may simply (and usually) return the original SQL.
	///
	/// @return `this`, for method chaining
	CommonBuilder statementInspector(UnaryOperator<String> operator);

	/// Signifies that no SQL statement inspector should be used.
	///
	/// By default, if no inspector is explicitly specified, the
	/// inspector associated with the {@link org.hibernate.SessionFactory}, if one, is
	/// inherited by the new session.
	///
	/// @return `this`, for method chaining
	///
	/// @apiNote Calling [#statementInspector] with `null` has the same effect.
	CommonBuilder noStatementInspector();

	/// Specify the tenant identifier to be associated with the opened session.
	///
	/// ```java
	/// try (var session = sessionFactory.withOptions()
	///			.tenantIdentifier(tenantId)
	///			.openSession()) {
	/// 	...
	///	}
	/// ```
	/// @param tenantIdentifier The tenant identifier.
	///
	/// @return `this`, for method chaining
	CommonBuilder tenantIdentifier(Object tenantIdentifier);

	/// Specify a [read-only mode][Session#isDefaultReadOnly]
	/// for the session. If a session is created in read-only mode, then
	/// [Connection#setReadOnly] is called when a JDBC connection is obtained.
	///
	/// Furthermore, if read/write replication is in use, then:
	/// * a read-only session will connect to a read-only replica, but
	/// * a non-read-only session will connect to a writable replica.
	///
	/// When read/write replication is in use, it's strongly recommended
	/// that the session be created with the [initial cache-mode][#initialCacheMode]
	/// set to [CacheMode#GET], to avoid writing stale data read from a read-only
	/// replica to the second-level cache. Hibernate cannot possibly guarantee that
	/// data read from a read-only replica is up to date.
	///
	/// When read/write replication is in use, it's possible that an item
	/// read from the second-level cache might refer to data which does not
	/// yet exist in the read-only replica. In this situation, an exception
	/// occurs when the association is fetched. To completely avoid this
	/// possibility, the [initial cache-mode][#initialCacheMode] must be
	/// set to [CacheMode#IGNORE]. However, it's also usually possible to
	/// structure data access code in a way which eliminates this possibility.
	/// ```java
	/// try (var readOnlySession =
	/// 		sessionFactory.withOptions()
	/// 			.readOnly(true)
	/// 			.initialCacheMode(CacheMode.IGNORE)
	///				.openSession()) {
	/// 	...
	/// }
	/// ```
	///
	/// If a session is created in read-only mode, then it cannot be
	/// changed to read-write mode, and any call to [Session#setDefaultReadOnly(boolean)]
	/// with fail. On the other hand, if a session is created in read-write mode, then it
	/// may later be switched to read-only mode, but all database access is directed to
	/// the writable replica.
	///
	/// @return `this`, for method chaining
	///
	/// @see org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider#getReadOnlyConnection(Object)
	/// @see org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider#releaseReadOnlyConnection(Object, Connection)
	///
	/// @since 7.2
	@Incubating
	CommonBuilder readOnly(boolean readOnly);

	/// Specify the initial [CacheMode] for the session.
	///
	/// @return `this`, for method chaining
	///
	/// @see SharedSessionContract#getCacheMode()
	///
	/// @since 7.2
	CommonBuilder initialCacheMode(CacheMode cacheMode);

	/// Specify the [JDBC time zone][org.hibernate.cfg.JdbcSettings#JDBC_TIME_ZONE]
	/// to use for the session.
	///
	/// @return `this`, for method chaining
	CommonBuilder jdbcTimeZone(TimeZone timeZone);
}
