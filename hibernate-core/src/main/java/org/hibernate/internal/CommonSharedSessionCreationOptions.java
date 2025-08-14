/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.Interceptor;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

/**
 * Creation options for shared {@linkplain org.hibernate.engine.spi.SessionImplementor stateful}
 * and {@linkplain org.hibernate.engine.spi.StatelessSessionImplementor stateless} sessions.
 *
 * @author Steve Ebersole
 */
public interface CommonSharedSessionCreationOptions {

	Interceptor getInterceptor();

	StatementInspector getStatementInspector();

	Object getTenantIdentifierValue();

	boolean isTransactionCoordinatorShared();
	TransactionCoordinator getTransactionCoordinator();
	JdbcCoordinator getJdbcCoordinator();
}
