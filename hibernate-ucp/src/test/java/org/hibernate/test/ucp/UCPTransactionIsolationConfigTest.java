package org.hibernate.test.ucp;

import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.TiDBDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.oracleucp.internal.UCPConnectionProvider;
import org.hibernate.testing.common.connections.BaseTransactionIsolationConfigTest;
import org.hibernate.testing.orm.junit.SkipForDialect;

@SkipForDialect(dialectClass = SybaseDialect.class, reason = "The jTDS driver doesn't implement Connection#getNetworkTimeout() so this fails")
@SkipForDialect(dialectClass = TiDBDialect.class, reason = "Doesn't support SERIALIZABLE isolation")
public class UCPTransactionIsolationConfigTest extends BaseTransactionIsolationConfigTest{
	@Override
	protected ConnectionProvider getConnectionProviderUnderTest() {
		return new UCPConnectionProvider();
	}
}
