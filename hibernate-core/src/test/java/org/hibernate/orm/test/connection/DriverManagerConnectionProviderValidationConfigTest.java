/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.connection;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;

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
		integrationSettings = @Setting(name = DriverManagerConnectionProviderImpl.VALIDATION_INTERVAL, value = "1")
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