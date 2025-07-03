/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.agroal;

import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.dialect.GaussDBDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.agroal.internal.AgroalConnectionProvider;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.common.connections.BaseTransactionIsolationConfigTest;

/**
 * @author Steve Ebersole
 */
@SkipForDialect(value = TiDBDialect.class, comment = "Doesn't support SERIALIZABLE isolation")
@SkipForDialect(value = AltibaseDialect.class, comment = "Altibase cannot change isolation level in autocommit mode")
@SkipForDialect(value = GaussDBDialect.class, comment = "GaussDB query serialization level of SERIALIZABLE has some problem")
public class AgroalTransactionIsolationConfigTest extends BaseTransactionIsolationConfigTest {
	@Override
	protected ConnectionProvider getConnectionProviderUnderTest() {
		return new AgroalConnectionProvider();
	}
}
