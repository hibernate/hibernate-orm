/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import java.sql.Connection;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.JdbcMetadataOnBoot;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;

/// Determines the transaction concurrency behavior that Hibernate should
/// assume for a session factory's connections. Called during bootstrap to
/// produce an immutable [TransactionConcurrency] descriptor from dialect
/// knowledge, available JDBC metadata, and an optional configuration declaration.
///
/// Resolution also takes place when bootstrap has no JDBC connection. In
/// that case, the resolver uses the supplied metadata, established dialect
/// facts, and the declaration. Missing information must remain unknown;
/// it must not be interpreted as a guarantee.
///
/// When bootstrap supplies a connection and JDBC access is permitted, the
/// resolver may query it for additional facts, such as database settings
/// that affect the behavior of the configured isolation level. The connection
/// is borrowed only for the duration of [#resolve]. Implementations must not
/// acquire another connection, retain or close the supplied connection, or
/// change its isolation or other settings. Statements and result sets created
/// by the resolver must be closed before returning.
///
/// The resulting descriptor describes behavior; it does not configure
/// connections or track transactions in individual sessions. A named declaration
/// may supply facts unavailable to bootstrap, but must not override known
/// contradictory observations or dialect facts. A supplied descriptor is
/// authoritative: bootstrap uses it directly without invoking this resolver.
/// Built-in resolvers also return descriptor instances unchanged if called directly.
///
/// @see org.hibernate.cfg.TransactionSettings#TRANSACTION_CONCURRENCY
///
/// @since 8.0
/// @author Steve Ebersole
@FunctionalInterface
public interface TransactionConcurrencyResolver {
	/// Builds the factory's concurrency descriptor from the available facts.
	/// JDBC queries are permitted only when `connection` is non-null and
	/// `access` is not [JdbcMetadataOnBoot#DISALLOW].
	///
	/// A fact for which no probe is supported may remain unknown. If a supported
	/// probe fails under [JdbcMetadataOnBoot#ALLOW], resolution may continue
	/// using the other available facts. Under [JdbcMetadataOnBoot#REQUIRE],
	/// such a failure must fail resolution. Invalid names and known
	/// contradictions must fail resolution regardless of the access policy.
	///
	/// @param dialect the resolved database dialect
	/// @param metadata bootstrap metadata, which may contain no JDBC observations
	/// @param connection the connection already obtained by bootstrap, or `null`
	/// when resolving without a connection
	/// @param declaration the value of
	/// [org.hibernate.cfg.TransactionSettings#TRANSACTION_CONCURRENCY], either
	/// a name recognized by the resolver
	/// (the standard names are listed on that setting), or `null` if no baseline
	/// was declared. Direct calls supplying a [TransactionConcurrency] instance
	/// must return that instance without probing or validation
	/// @param access the JDBC access policy for this resolution
	/// @return an immutable descriptor, independent of the supplied connection
	///
	/// @throws TransactionConcurrencyResolutionException if a declaration is
	/// invalid, contradicts known facts, or a supported probe fails when JDBC
	/// access is required; these failures must not be hidden by bootstrap fallback
	TransactionConcurrency resolve(
			Dialect dialect,
			JdbcMetadata metadata,
			Connection connection,
			Object declaration,
			JdbcMetadataOnBoot access);
}
