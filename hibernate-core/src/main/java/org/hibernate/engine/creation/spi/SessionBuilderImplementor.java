/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.spi;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.CacheMode;
import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionEventListener;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.sql.Connection;
import java.time.Instant;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

/**
 * Defines the internal contract between the {@link SessionBuilder} and
 * other parts of Hibernate.
 *
 * @see SessionBuilder
 *
 * @author Gail Badner
 */
public interface SessionBuilderImplementor extends SessionBuilder {
	@Override
	@Nonnull
	SessionImplementor openSession();

	@Override
	@Nonnull
	SessionBuilderImplementor interceptor(@Nullable Interceptor interceptor);

	@Override
	@Nonnull
	SessionBuilderImplementor noInterceptor();

	@Override
	@Nonnull
	SessionBuilderImplementor noSessionInterceptorCreation();

	@Override
	@Nonnull
	SessionBuilderImplementor statementInspector(@Nullable UnaryOperator<String> operator);

	@Override
	@Nonnull
	SessionBuilderImplementor statementInspector(@Nonnull StatementInspector statementInspector);

	@Override
	@Nonnull
	SessionBuilderImplementor connection(@Nonnull Connection connection);

	@Override
	@Nonnull
	SessionBuilderImplementor connectionHandling(@Nonnull ConnectionAcquisitionMode acquisitionMode, @Nonnull ConnectionReleaseMode releaseMode);

	@Override
	@Nonnull
	SessionBuilderImplementor connectionHandlingMode(@Nonnull PhysicalConnectionHandlingMode mode);

	@Override
	@Nonnull
	SessionBuilderImplementor autoJoinTransactions(boolean autoJoinTransactions);

	@Override
	@Nonnull
	SessionBuilderImplementor autoClear(boolean autoClear);

	@Override
	@Nonnull
	SessionBuilderImplementor flushMode(@Nonnull FlushMode flushMode);

	@Override
	@Nonnull
	SessionBuilderImplementor tenantIdentifier(@Nullable Object tenantIdentifier);

	@Override
	@Nonnull
	SessionBuilderImplementor readOnly(boolean readOnly);

	@Override
	@Nonnull
	SessionBuilderImplementor initialCacheMode(@Nonnull CacheMode cacheMode);

	@Override
	@Nonnull
	SessionBuilderImplementor eventListeners(@Nonnull SessionEventListener... listeners);

	@Override
	@Nonnull
	SessionBuilderImplementor clearEventListeners();

	@Override
	@Nonnull
	SessionBuilderImplementor jdbcTimeZone(@Nullable TimeZone timeZone);

	@Override
	@Nonnull
	SessionBuilderImplementor autoClose(boolean autoClose);

	@Override
	@Nonnull
	SessionBuilderImplementor identifierRollback(boolean identifierRollback);

	@Override
	@Nonnull
	SessionBuilderImplementor noStatementInspector();

	@Override
	@Nonnull
	SessionBuilderImplementor defaultBatchFetchSize(int defaultBatchFetchSize);

	@Override
	@Nonnull
	SessionBuilderImplementor subselectFetchEnabled(boolean subselectFetchEnabled);

	@Override
	@Nonnull
	SessionBuilderImplementor asOf(@Nullable Instant instant);

	@Override
	@Nonnull
	SessionBuilderImplementor atChangeset(@Nullable Object changesetId);
}
