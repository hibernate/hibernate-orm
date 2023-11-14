/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.c3p0;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.TiDBDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.common.connections.BaseTransactionIsolationConfigTest;
import org.junit.Before;

/**
 * @author Steve Ebersole
 */
@SkipForDialect(value = TiDBDialect.class, comment = "Doesn't support SERIALIZABLE isolation")
@SkipForDialect(value = AltibaseDialect.class, comment = "Altibase cannot change isolation level in autocommit mode")
public class C3p0TransactionIsolationConfigTest extends BaseTransactionIsolationConfigTest {
	private StandardServiceRegistry ssr;

	@Before
	public void setUp() {
		ssr = new StandardServiceRegistryBuilder().build();
	}

	@Override
	protected ConnectionProvider getConnectionProviderUnderTest() {
		C3P0ConnectionProvider provider = new C3P0ConnectionProvider();
		provider.injectServices( (ServiceRegistryImplementor) ssr );
		return provider;
	}
}
