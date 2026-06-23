/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.sql.Connection;
import java.util.List;
import java.util.TimeZone;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionCreationOption;
import org.hibernate.SessionEventListener;
import org.hibernate.StatementObserver;
import org.hibernate.engine.creation.CommonBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Options, specified through various {@linkplain CommonBuilder builders},
 * used when creating sessions.
 *
 * @author Steve Ebersole
 *
 * @since 7.2
 */
public interface SessionCreationOptions {

	boolean shouldAutoJoinTransactions();

	@Nullable
	FlushMode getInitialSessionFlushMode();

	boolean isSubselectFetchEnabled();

	int getDefaultBatchFetchSize();

	boolean shouldAutoClose();

	boolean shouldAutoClear();

	@Nullable
	Connection getConnection();

	@Nullable
	Interceptor resolveInterceptor(@Nonnull SessionFactoryImplementor sessionFactory);

	@Nullable
	StatementObserver getStatementObserver();

	@Nullable
	StatementInspector getStatementInspector();

	@Nonnull
	PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode();

	@Nullable
	Object getTenantIdentifierValue();

	boolean isReadOnly();

	@Nullable
	Integer getJdbcBatchSize();

	@Nonnull
	CacheMode getInitialCacheMode();

	boolean isIdentifierRollbackEnabled();

	@Nullable
	TimeZone getJdbcTimeZone();

	@Nullable
	Object getTemporalIdentifier();

	@Nonnull
	List<SessionCreationOption.EnabledFilter> getEnabledFilterOptions();

	/**
	 * @return the full list of SessionEventListener if this was customized,
	 * or null if this Session is being created with the default list.
	 */
	@Nullable
	List<SessionEventListener> getCustomSessionEventListeners();
}
