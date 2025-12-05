/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;

import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;


/**
 * Implementation of BasicConnectionProviderTest.
 *
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = Environment.CONNECTION_HANDLING,
				provider = BasicConnectionProviderTest.ConnectionmHandlingProvider.class
		)
)
public class BasicConnectionProviderTest extends ConnectionManagementTestCase {

	public static class ConnectionmHandlingProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT.toString();
		}
	}

	@Override
	protected Session getSessionUnderTest(SessionFactoryScope scope) {
		Session session = scope.getSessionFactory().openSession();
		session.beginTransaction();
		return session;
	}

	@Override
	protected void reconnect(Session session) {
	}
}
