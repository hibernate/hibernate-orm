/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionEventListener;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.sql.Connection;
import java.time.Instant;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

/**
 * @author Steve Ebersole
 *
 * @since 7.2
 */
public interface SharedSessionBuilderImplementor extends SharedSessionBuilder, SessionBuilderImplementor {
	@Override
	@Nonnull
	SharedSessionBuilderImplementor interceptor(@Nullable Interceptor interceptor);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor noInterceptor();

	@Override
	@Nonnull
	SharedSessionBuilderImplementor noSessionInterceptorCreation();

	@Override
	@Nonnull
	SharedSessionBuilderImplementor statementInspector(@Nullable UnaryOperator<String> operator);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor statementInspector(@Nonnull StatementInspector statementInspector);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor tenantIdentifier(Object tenantIdentifier);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor readOnly(boolean readOnly);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor initialCacheMode(@Nonnull CacheMode cacheMode);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor connection();

	@Override
	@Nonnull
	SharedSessionBuilderImplementor interceptor();

	@Override
	@Nonnull
	SharedSessionBuilderImplementor connectionHandlingMode();

	@Override
	@Nonnull
	SharedSessionBuilderImplementor autoJoinTransactions();

	@Override
	@Nonnull
	SharedSessionBuilderImplementor flushMode();

	@Override
	@Nonnull
	SharedSessionBuilderImplementor autoClose();

	@Override
	@Deprecated
	@Nonnull
	SharedSessionBuilderImplementor connectionHandlingMode(@Nonnull PhysicalConnectionHandlingMode mode);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor connectionHandling(@Nonnull ConnectionAcquisitionMode acquisitionMode, @Nonnull ConnectionReleaseMode releaseMode);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor autoClear(boolean autoClear);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor flushMode(@Nonnull FlushMode flushMode);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor eventListeners(@Nonnull SessionEventListener... listeners);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor clearEventListeners();

	@Override
	@Nonnull
	SharedSessionBuilderImplementor jdbcTimeZone(@Nullable TimeZone timeZone);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor connection(@Nonnull Connection connection);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor autoJoinTransactions(boolean autoJoinTransactions);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor autoClose(boolean autoClose);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor identifierRollback(boolean identifierRollback);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor statementInspector();

	@Override
	@Nonnull
	SharedSessionBuilderImplementor noStatementInspector();

	@Override
	@Nonnull
	SharedSessionBuilderImplementor defaultBatchFetchSize(int defaultBatchFetchSize);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor subselectFetchEnabled(boolean subselectFetchEnabled);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor asOf(@Nullable Instant instant);

	@Override
	@Nonnull
	SharedSessionBuilderImplementor atChangeset(@Nullable Object changesetId);
}
