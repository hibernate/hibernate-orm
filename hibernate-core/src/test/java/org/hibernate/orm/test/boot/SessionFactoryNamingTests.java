/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.PersistenceSettings;

import org.hibernate.internal.SessionFactoryRegistryMessageLogger;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@MessageKeyInspection(
		messageKey = "HHH020277",
		logger = @Logger( loggerName = SessionFactoryRegistryMessageLogger.LOGGER_NAME )
)
public class SessionFactoryNamingTests {
	@Test
	@DomainModel
	@ServiceRegistry( settings = {
			@Setting( name = AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, value = "true" ),
			@Setting( name = AvailableSettings.SESSION_FACTORY_JNDI_NAME, value = "jndi-named" )
	} )
	@SessionFactory()
	void testExplicitJndiName(SessionFactoryScope scope, MessageKeyWatcher logWatcher) {
		scope.getSessionFactory();
		assertThat( logWatcher.wasTriggered() ).isTrue();
	}

	@Test
	@DomainModel
	@ServiceRegistry( settings = @Setting( name = AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, value = "true" ) )
	@SessionFactory( sessionFactoryName = "named" )
	void testSessionFactoryName(SessionFactoryScope scope, MessageKeyWatcher logWatcher) {
		scope.getSessionFactory();
		assertThat( logWatcher.wasTriggered() ).isTrue();
	}

	@Test
	@DomainModel
	@ServiceRegistry( settings = @Setting( name = AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, value = "false" ) )
	@SessionFactory( sessionFactoryName = "named" )
	void testNonJndiSessionFactoryName(SessionFactoryScope scope, MessageKeyWatcher logWatcher) {
		scope.getSessionFactory();
		assertThat( logWatcher.wasTriggered() ).isFalse();
	}

	@Test
	@DomainModel
	@ServiceRegistry( settings = {
			@Setting( name = AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, value = "true" ),
			// mimics the persistence.xml persistence-unit name
			@Setting( name = PersistenceSettings.PERSISTENCE_UNIT_NAME, value = "named-pu" ),
	} )
	@SessionFactory
	void testPuName(SessionFactoryScope scope, MessageKeyWatcher logWatcher) {
		scope.getSessionFactory();
		assertThat( logWatcher.wasTriggered() ).isFalse();
	}

	@Test
	@DomainModel
	@ServiceRegistry( settings = {
			@Setting( name = AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, value = "false" ),
			// mimics the persistence.xml persistence-unit name
			@Setting( name = PersistenceSettings.PERSISTENCE_UNIT_NAME, value = "named-pu" ),
	} )
	@SessionFactory
	void testNonJndiPuName(SessionFactoryScope scope, MessageKeyWatcher logWatcher) {
		scope.getSessionFactory();
		assertThat( logWatcher.wasTriggered() ).isFalse();
	}
}
