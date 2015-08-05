/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.connection;

import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.common.connections.BaseTransactionIsolationConfigTest;
import org.junit.Ignore;

/**
 * NOTE : had to mark this as ignored because otherwise many other of the hibernate-core tests ran into problems.
 * It seems like H2 holds on to the isolation.  Anyway quite a few other tests fail when this test is run,
 * none do when it is not :(
 *
 * @author Steve Ebersole
 */
@Ignore
public class DriverManagerConnectionProviderTransactionIsolationConfigTest extends BaseTransactionIsolationConfigTest {
	@Override
	protected ConnectionProvider getConnectionProviderUnderTest() {
		return new DriverManagerConnectionProviderImpl();
	}
}
