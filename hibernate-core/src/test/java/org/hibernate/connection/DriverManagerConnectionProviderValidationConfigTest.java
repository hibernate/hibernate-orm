/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.connection;

import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class DriverManagerConnectionProviderValidationConfigTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Event.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( DriverManagerConnectionProviderImpl.VALIDATION_INTERVAL, 1L );
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
		} );
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		@GeneratedValue
		private Long id;

		private String name;
	}
}