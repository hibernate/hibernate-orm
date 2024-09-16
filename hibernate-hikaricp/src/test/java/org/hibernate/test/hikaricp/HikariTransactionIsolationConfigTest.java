/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.hikaricp;

import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.TiDBDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.common.connections.BaseTransactionIsolationConfigTest;

/**
 * @author Steve Ebersole
 */
@SkipForDialect(value = SybaseDialect.class, comment = "The jTDS driver doesn't implement Connection#getNetworkTimeout() so this fails")
@SkipForDialect(value = TiDBDialect.class, comment = "Doesn't support SERIALIZABLE isolation")
@SkipForDialect(value = AltibaseDialect.class, comment = "Altibase cannot change isolation level in autocommit mode")
public class HikariTransactionIsolationConfigTest extends BaseTransactionIsolationConfigTest {
	@Override
	protected ConnectionProvider getConnectionProviderUnderTest() {
		return new HikariCPConnectionProvider();
	}
}
