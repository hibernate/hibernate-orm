/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.ejb3configuration;

import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;

import org.hibernate.testing.orm.jpa.PersistenceUnitInfoAdapter;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManagerFactory;

/**
 * @author Emmanuel Bernard
 */
public class SessionFactoryObserverTest {
    @Test
	public void testSessionFactoryObserverProperty() {

		Map<String, Object> settings = ServiceRegistryUtil.createBaseSettings();
		settings.put( AvailableSettings.SESSION_FACTORY_OBSERVER, GoofySessionFactoryObserver.class.getName() );
		EntityManagerFactoryBuilder builder = Bootstrap.getEntityManagerFactoryBuilder(
				new PersistenceUnitInfoAdapter(),
				settings
		);

		try {
			final EntityManagerFactory entityManagerFactory = builder.build();
			entityManagerFactory.close();
			Assertions.fail( "GoofyException should have been thrown" );
		}
		catch ( GoofyException e ) {
			//success
		}
	}

	public static class GoofySessionFactoryObserver implements SessionFactoryObserver {
		public void sessionFactoryCreated(SessionFactory factory) {
		}

		public void sessionFactoryClosed(SessionFactory factory) {
			throw new GoofyException();
		}
	}

	public static class GoofyException extends RuntimeException {
	}
}
