/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ejb3configuration;

import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.orchestration.SessionFactoryBootstrap;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;

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

		try {
			final EntityManagerFactory entityManagerFactory = SessionFactoryBootstrap.build(
					new PersistenceUnitInfoDescriptor( new PersistenceUnitInfoAdapter() ),
					settings
			);
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
