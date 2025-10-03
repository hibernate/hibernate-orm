/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.c3p0;

import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.community.dialect.GaussDBDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.test.c3p0.util.GradleParallelTestingC3P0ConnectionProvider;
import org.hibernate.testing.orm.common.BaseTransactionIsolationConfigTest;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;

/**
 * @author Steve Ebersole
 */
@SkipForDialect(dialectClass = TiDBDialect.class, reason = "Doesn't support SERIALIZABLE isolation")
@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase cannot change isolation level in autocommit mode")
@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "JtdsConnection.isValid not implemented")
@SkipForDialect(dialectClass = GaussDBDialect.class, reason = "GaussDB does not support SERIALIZABLE isolation")
@ServiceRegistry
public class C3p0TransactionIsolationConfigTest
		extends BaseTransactionIsolationConfigTest {

	@Override
	protected ConnectionProvider getConnectionProviderUnderTest(ServiceRegistryScope registryScope) {
		C3P0ConnectionProvider provider = new GradleParallelTestingC3P0ConnectionProvider();
		provider.injectServices( (ServiceRegistryImplementor) registryScope.getRegistry() );
		return provider;
	}
}
