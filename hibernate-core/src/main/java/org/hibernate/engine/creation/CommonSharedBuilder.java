/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import org.hibernate.CacheMode;
import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Incubating;
import org.hibernate.Interceptor;

import java.sql.Connection;
import java.time.Instant;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

/// Common options for builders of [stateful][org.hibernate.Session] and
/// [stateless][org.hibernate.StatelessSession] sessions which share state
/// from an underlying stateful/stateless session.
///
/// @since 7.2
///
/// @author Steve Ebersole
@Incubating
public interface CommonSharedBuilder extends CommonBuilder {

	/// Signifies that the connection from the original session should be used to create the new session.
	/// Implies that the overall "transaction context" should be shared as well.
	///
	/// @return `this`, for method chaining
	@Nonnull
	CommonSharedBuilder connection();

	/// Signifies the interceptor from the original session should be used to create the new session.
	///
	/// @return `this`, for method chaining
	@Nonnull
	CommonSharedBuilder interceptor();

	/// Signifies that the SQL statement inspector from the original session should be used to create the new session.
	///
	/// @return `this`, for method chaining
	@Nonnull
	CommonSharedBuilder statementInspector();

	@Override
	@Nonnull
	CommonSharedBuilder noStatementInspector();

	@Override
	@Nonnull
	CommonSharedBuilder interceptor(@Nullable Interceptor interceptor);

	@Override
	@Nonnull
	CommonSharedBuilder noInterceptor();

	@Override
	@Nonnull
	CommonSharedBuilder noSessionInterceptorCreation();

	@Override
	@Nonnull
	CommonSharedBuilder statementInspector(@Nullable UnaryOperator<String> operator);

	@Override
	@Nonnull
	CommonSharedBuilder readOnly(boolean readOnly);

	@Override
	@Nonnull
	CommonSharedBuilder initialCacheMode(@Nonnull CacheMode cacheMode);

	@Override
	@Nonnull
	CommonSharedBuilder tenantIdentifier(Object tenantIdentifier);

	@Override
	@Nonnull
	CommonSharedBuilder connection(@Nonnull Connection connection);

	@Override
	@Nonnull
	CommonSharedBuilder connectionHandling(@Nonnull ConnectionAcquisitionMode acquisitionMode, @Nonnull ConnectionReleaseMode releaseMode);

	@Override
	@Nonnull
	CommonSharedBuilder jdbcBatchSize(int batchSize);

	@Override
	@Nonnull
	CommonSharedBuilder cacheStoreMode(@Nullable CacheStoreMode cacheStoreMode);

	@Override
	@Nonnull
	CommonSharedBuilder cacheRetrieveMode(@Nullable CacheRetrieveMode cacheRetrieveMode);

	@Override
	@Nonnull
	CommonSharedBuilder jdbcTimeZone(@Nullable TimeZone timeZone);

	@Override
	@Nonnull
	CommonSharedBuilder asOf(@Nullable Instant instant);

	@Override
	@Nonnull
	CommonSharedBuilder atChangeset(@Nullable Object changesetId);
}
