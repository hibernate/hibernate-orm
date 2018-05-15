/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.session;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.Session;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.proxy.AbstractLazyInitializer;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class AssociateEntityWithTwoSessionsTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			Location.class,
			Event.class
		};
	}

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( CoreMessageLogger.class, AbstractLazyInitializer.class.getName() ) );

	@Test
	@TestForIssue( jiraKey = "HHH-12216" )
	public void test() {

		final Location location = new Location();
		location.setCity( "Cluj" );

		final Event event = new Event();
		event.setLocation( location );

		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( location );
			entityManager.persist( event );
		} );

		final Triggerable triggerable = logInspection.watchForLogMessages( "HHH000485" );
		triggerable.reset();

		doInJPA( this::entityManagerFactory, entityManager -> {
			Event event1 = entityManager.find( Event.class, event.id );
			Location location1 = event1.getLocation();

			try {
				doInJPA( this::entityManagerFactory, _entityManager -> {
					_entityManager.unwrap( Session.class ).update( location1 );
				} );

				fail("Should have thrown a HibernateException");
			}
			catch (Exception expected) {
			}
		} );

		assertEquals(
			"HHH000485: Illegally attempted to associate a proxy for entity [org.hibernate.session.AssociateEntityWithTwoSessionsTest$Location] with id [1] with two open sessions.",
			triggerable.triggerMessage()
		);

	}

	@Entity(name = "Location")
	public static class Location {

		@Id
		@GeneratedValue
		public Long id;

		public String city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		@GeneratedValue
		public Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Location location;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Location getLocation() {
			return location;
		}

		public void setLocation(Location location) {
			this.location = location;
		}
	}
}
