/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.CacheMode;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

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

	boolean isTransactionCoordinatorShared();
	TransactionCoordinator getTransactionCoordinator();
	JdbcCoordinator getJdbcCoordinator();
	Transaction getTransaction();
}
