/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import java.sql.Connection;
import java.util.List;
import java.util.TimeZone;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionEventListener;
import org.hibernate.engine.creation.CommonBuilder;
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

	FlushMode getInitialSessionFlushMode();

	boolean isSubselectFetchEnabled();

	int getDefaultBatchFetchSize();

	boolean shouldAutoClose();

	boolean shouldAutoClear();

	Connection getConnection();

	Interceptor getInterceptor();

	StatementInspector getStatementInspector();

	PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode();

	String getTenantIdentifier();

	Object getTenantIdentifierValue();

	boolean isReadOnly();

	CacheMode getInitialCacheMode();

	boolean isIdentifierRollbackEnabled();

	TimeZone getJdbcTimeZone();

	/**
	 * @return the full list of SessionEventListener if this was customized,
	 * or null if this Session is being created with the default list.
	 */
	List<SessionEventListener> getCustomSessionEventListener();
}
