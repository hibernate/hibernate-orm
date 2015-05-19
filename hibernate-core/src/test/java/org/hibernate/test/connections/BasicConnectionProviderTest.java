/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.connections;

import java.util.Map;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;

/**
 * Implementation of BasicConnectionProviderTest.
 *
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
public class BasicConnectionProviderTest extends ConnectionManagementTestCase {
	@Override
	protected Session getSessionUnderTest() {
		return openSession();
	}

	@Override
	protected void reconnect(Session session) {
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( Environment.RELEASE_CONNECTIONS, ConnectionReleaseMode.ON_CLOSE.toString() );
	}
}
