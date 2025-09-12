/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import org.hibernate.CacheMode;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.TransactionCompletionCallbacksImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

import java.util.TimeZone;


/**
 * Creation options for shared {@linkplain org.hibernate.engine.spi.SessionImplementor stateful}
 * and {@linkplain org.hibernate.engine.spi.StatelessSessionImplementor stateless} sessions.
 *
 * @implNote At the moment this is only used in the creation of
 *           {@linkplain org.hibernate.engine.spi.StatelessSessionImplementor stateless sessions}.
 *
 * @author Steve Ebersole
 *
 * @since 7.2
 */
public interface CommonSharedSessionCreationOptions {

	Interceptor getInterceptor();

	StatementInspector getStatementInspector();

	Object getTenantIdentifierValue();

	boolean isReadOnly();

	CacheMode getInitialCacheMode();

	PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode();

	boolean isTransactionCoordinatorShared();
	TransactionCoordinator getTransactionCoordinator();
	JdbcCoordinator getJdbcCoordinator();
	TransactionCompletionCallbacksImplementor getTransactionCompletionCallbacksImplementor();
	Transaction getTransaction();

	TimeZone getJdbcTimeZone();
}
