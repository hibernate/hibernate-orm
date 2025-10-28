/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connection;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = { DriverManagerConnectionProviderValidationConfigTest.Event.class },
		integrationSettings = {
				// Force a non-shared connection provider to avoid re-creation of the shared pool
				@Setting(name = AvailableSettings.CONNECTION_PROVIDER, value = ""),
				@Setting(name = DriverManagerConnectionProvider.VALIDATION_INTERVAL, value = "1")
		}
)
public class DriverManagerConnectionProviderValidationConfigTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Event event = new Event();
					entityManager.persist( event );

					assertTrue( Thread.getAllStackTraces()
										.keySet()
										.stream()
										.filter( thread -> thread.getName()
												.equals( "Hibernate Connection Pool Validation Thread" ) && thread.isDaemon() )
										.map( Thread::isDaemon )
										.findAny()
										.isPresent() );
				}
		);
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		@GeneratedValue
		private Long id;

		private String name;
	}
}
