/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import java.sql.Connection;
import java.util.List;
import java.util.TimeZone;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionEventListener;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.ExceptionMapper;

/**
 * @author Steve Ebersole
 */
public interface SessionCreationOptions {
	// todo : (5.2) review this. intended as a consolidation of the options needed to create a Session
	//		comes from building a Session and a EntityManager

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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// deprecations

	ExceptionMapper getExceptionMapper();
}
