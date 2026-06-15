/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.sql.Connection;
import java.time.Instant;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.engine.creation.CommonBuilder;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Allows creation of a new {@link StatelessSession} with specific options
 * overriding the defaults from the {@link SessionFactory}.
 *
 * @author Steve Ebersole
 *
 * @see SessionFactory#withStatelessOptions()
 * @see SharedStatelessSessionBuilder
 */
public interface StatelessSessionBuilder extends CommonBuilder {
	/**
	 * Opens a session with the specified options.
	 * @see #open()
	 */
	@Nonnull
	StatelessSession openStatelessSession();

	@Override
	@Nonnull
	StatelessSession open();

	@Override
	@Nonnull
	StatelessSessionBuilder connection(@Nonnull Connection connection);

	@Override
	@Nonnull
	StatelessSessionBuilder connectionHandling(@Nonnull ConnectionAcquisitionMode acquisitionMode, @Nonnull ConnectionReleaseMode releaseMode);

	@Override
	@Nonnull
	StatelessSessionBuilder interceptor(@Nullable Interceptor interceptor);

	@Override
	@Nonnull
	StatelessSessionBuilder noInterceptor();

	@Override
	@Nonnull
	StatelessSessionBuilder noSessionInterceptorCreation();

	@Override
	@Nonnull
	StatelessSessionBuilder tenantIdentifier(Object tenantIdentifier);

	@Incubating
	@Override
	@Nonnull
	StatelessSessionBuilder readOnly(boolean readOnly);

	@Incubating
	@Override
	@Nonnull
	StatelessSessionBuilder jdbcBatchSize(int batchSize);

	@Incubating
	@Override
	@Nonnull
	StatelessSessionBuilder initialCacheMode(@Nonnull CacheMode cacheMode);

	@Override
	@Nonnull
	StatelessSessionBuilder cacheStoreMode(@Nullable CacheStoreMode cacheStoreMode);

	@Override
	@Nonnull
	StatelessSessionBuilder cacheRetrieveMode(@Nullable CacheRetrieveMode cacheRetrieveMode);

	@Override
	@Nonnull
	StatelessSessionBuilder statementInspector(@Nullable UnaryOperator<String> operator);

	@Override
	@Nonnull
	StatelessSessionBuilder noStatementInspector();

	@Override
	@Nonnull
	StatelessSessionBuilder jdbcTimeZone(@Nullable TimeZone timeZone);

	/**
	 * Applies the given {@link StatementInspector} to the session.
	 *
	 * @param statementInspector The {@code StatementInspector} to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated This operation exposes the SPI type{@link StatementInspector}
	 * and is therefore a layer-breaker. Use {@link #statementInspector(UnaryOperator)}
	 * instead.
	 */
	@Deprecated(since = "7.0")
	@Nonnull
	StatelessSessionBuilder statementInspector(@Nonnull StatementInspector statementInspector);

	/**
	 * Specify the instant for reading
	 * {@linkplain org.hibernate.annotations.Temporal temporal} entity data.
	 * Instances of temporal entities retrieved in the session will represent
	 * the revisions effective at the given instant.
	 */
	@Nonnull
	StatelessSessionBuilder asOf(@Nullable Instant instant);

	/**
	 * Specify the
	 * {@linkplain StateManagementSettings#CHANGESET_ID_SUPPLIER
	 * changeset id} for reading {@linkplain org.hibernate.annotations.Temporal
	 * temporal} or {@linkplain org.hibernate.annotations.Audited audited}
	 * entity data. Instances of temporal or audited entities retrieved in
	 * the session represent the state effective at the given changeset.
	 * The given value should match the type returned by the configured
	 * changeset id supplier.
	 */
	@Nonnull
	StatelessSessionBuilder atChangeset(@Nullable Object changesetId);
}
