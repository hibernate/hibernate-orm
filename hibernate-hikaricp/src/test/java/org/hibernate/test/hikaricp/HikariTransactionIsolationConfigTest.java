/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.hikaricp;

import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.community.dialect.GaussDBDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.test.hikaricp.util.GradleParallelTestingHikariCPConnectionProvider;
import org.hibernate.testing.orm.common.BaseTransactionIsolationConfigTest;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;

/**
 * @author Steve Ebersole
 */
@SkipForDialect(dialectClass = SybaseDialect.class,
		matchSubTypes = true,
		reason = "The jTDS driver doesn't implement Connection#getNetworkTimeout() so this fails")
@SkipForDialect(dialectClass = TiDBDialect.class,
		reason = "Doesn't support SERIALIZABLE isolation")
@SkipForDialect(dialectClass = AltibaseDialect.class,
		reason = "Altibase cannot change isolation level in autocommit mode")
@SkipForDialect(dialectClass = GaussDBDialect.class,
		reason = "GaussDB does not support SERIALIZABLE isolation")
public class HikariTransactionIsolationConfigTest extends BaseTransactionIsolationConfigTest {
	@Override
	protected ConnectionProvider getConnectionProviderUnderTest(ServiceRegistryScope registryScope) {
		return new GradleParallelTestingHikariCPConnectionProvider();
	}
}
