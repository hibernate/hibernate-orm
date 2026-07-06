/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tutorial.annotations;

import java.time.LocalDateTime;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;

import junit.framework.TestCase;

import static java.lang.System.out;
import static java.time.LocalDateTime.now;

/**
 * Illustrates the use of Hibernate native APIs, including the use
 * of HibernatePersistenceConfiguration for configuration and bootstrap.
 * Configuration properties are sourced from hibernate.properties.
 *
 * @author Steve Ebersole
 */
public class HibernateIllustrationTest extends TestCase {
	private SessionFactory sessionFactory;

	@Override
	protected void setUp() {
		// A SessionFactory is set up once for an application!
		sessionFactory =
				new HibernatePersistenceConfiguration( "hibernate-tutorial-annotations" )
						.managedClass(Event.class)
						.createEntityManagerFactory();
	}

	@Override
	protected void tearDown() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	public void testBasicUsage() {
		// create a couple of events...
		sessionFactory.inTransaction(session -> {
			session.persist(new Event("Our very first event!", now()));
			session.persist(new Event("A follow up event", now()));
		});

		// now lets pull events from the database and list them
		sessionFactory.inTransaction(session -> {
			session.createSelectionQuery("from Event", Event.class).getResultList()
					.forEach(event -> out.println("Event (" + event.getDate() + ") : " + event.getTitle()));
		});
	}
}
