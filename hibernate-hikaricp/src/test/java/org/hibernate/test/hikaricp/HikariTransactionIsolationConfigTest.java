/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hikaricp;

import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.common.connections.BaseTransactionIsolationConfigTest;

/**
 * @author Steve Ebersole
 */
@SkipForDialect(value = SybaseDialect.class, comment = "The jTDS driver doesn't implement Connection#isValid so this fails")
public class HikariTransactionIsolationConfigTest extends BaseTransactionIsolationConfigTest {
	@Override
	protected ConnectionProvider getConnectionProviderUnderTest() {
		return new HikariCPConnectionProvider();
	}
}
