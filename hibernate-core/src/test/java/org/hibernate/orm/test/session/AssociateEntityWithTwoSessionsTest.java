/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.session;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.proxy.AbstractLazyInitializer;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.hibernate.testing.orm.junit.Setting;
import org.junit.Rule;
import org.junit.jupiter.api.Test;

import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {AssociateEntityWithTwoSessionsTest.Location.class,
						AssociateEntityWithTwoSessionsTest.Event.class},
		properties = @Setting(name = AvailableSettings.ALLOW_REFRESH_DETACHED_ENTITY, value = "true"))
public class AssociateEntityWithTwoSessionsTest {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, AbstractLazyInitializer.class.getName() ) );

	@Test
	@JiraKey( value = "HHH-12216" )
	public void test(EntityManagerFactoryScope scope) {

		final Location location = new Location();
		location.setCity( "Cluj" );

		final Event event = new Event();
		event.setLocation( location );

		scope.inTransaction( entityManager -> {
			entityManager.persist( location );
			entityManager.persist( event );
		} );

		final Triggerable triggerable = logInspection.watchForLogMessages( "HHH000485" );
		triggerable.reset();

		scope.inTransaction( entityManager -> {
			Event e = entityManager.find( Event.class, event.id );
			Location location1 = e.getLocation();

			try {
				scope.inTransaction( _entityManager -> {
					_entityManager.unwrap( Session.class ).lock( location1, LockMode.NONE );
				} );

				fail("Should have thrown a HibernateException");
			}
			catch (Exception expected) {
			}
		} );

		assertEquals(
			"HHH000485: Illegally attempted to associate a proxy for entity [org.hibernate.orm.test.session.AssociateEntityWithTwoSessionsTest$Location] with id [1] with two open sessions.",
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
