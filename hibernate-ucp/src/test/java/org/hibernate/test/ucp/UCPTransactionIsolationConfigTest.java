/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ucp;

import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.TiDBDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.oracleucp.internal.UCPConnectionProvider;
import org.hibernate.testing.common.connections.BaseTransactionIsolationConfigTest;
import org.hibernate.testing.orm.junit.SkipForDialect;

@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "The jTDS driver doesn't implement Connection#getNetworkTimeout() so this fails")
@SkipForDialect(dialectClass = TiDBDialect.class, matchSubTypes = true, reason = "Doesn't support SERIALIZABLE isolation")
@SkipForDialect(dialectClass = AltibaseDialect.class, matchSubTypes = true, reason = "Altibase cannot change isolation level in autocommit mode")
public class UCPTransactionIsolationConfigTest extends BaseTransactionIsolationConfigTest{
	@Override
	protected ConnectionProvider getConnectionProviderUnderTest() {
		return new UCPConnectionProvider();
	}
}
