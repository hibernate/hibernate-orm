/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tutorial.envers;

import java.util.Date;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import junit.framework.TestCase;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

import static java.time.LocalDateTime.now;

import static jakarta.persistence.Persistence.createEntityManagerFactory;

/**
 * Illustrates the setup and use of Envers.
 * <p>
 * This example is different from the others because we need to have
 * multiple revisions to the entity in order to get a good look at
 * Envers in action.
 *
 * @author Steve Ebersole
 */
public class EnversIllustrationTest extends TestCase {
	private EntityManagerFactory entityManagerFactory;

	@Override
	protected void setUp() {
		entityManagerFactory = createEntityManagerFactory( "org.hibernate.tutorial.envers" );
	}

	@Override
	protected void tearDown() {
		entityManagerFactory.close();
	}

	public void testBasicUsage() {
		// create a couple of events
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		entityManager.getTransaction().begin();
		entityManager.persist( new Event( "Our very first event!", now() ) );
		entityManager.persist( new Event( "A follow up event", now() ) );
		entityManager.getTransaction().commit();
		entityManager.close();

		// now lets pull events from the database and list them
		entityManager = entityManagerFactory.createEntityManager();
		entityManager.getTransaction().begin();
        List<Event> result = entityManager.createQuery( "from Event", Event.class ).getResultList();
		for ( Event event : result ) {
			System.out.println( "Event (" + event.getDate() + ") : " + event.getTitle() );
		}
        entityManager.getTransaction().commit();
        entityManager.close();

		// so far the code is the same as we have seen in previous tutorials.  Now lets leverage Envers...

		// first lets create some revisions
		entityManager = entityManagerFactory.createEntityManager();
		entityManager.getTransaction().begin();
		Event myEvent = entityManager.find( Event.class, 2L ); // we are using the increment generator, so we know 2 is a valid id
		myEvent.setDate( now() );
		myEvent.setTitle( myEvent.getTitle() + " (rescheduled)" );
        entityManager.getTransaction().commit();
        entityManager.close();

		// and then use an AuditReader to look back through history
		entityManager = entityManagerFactory.createEntityManager();
		entityManager.getTransaction().begin();
		myEvent = entityManager.find( Event.class, 2L );
		assertEquals( "A follow up event (rescheduled)", myEvent.getTitle() );
		AuditReader reader = AuditReaderFactory.get( entityManager );
		Event firstRevision = reader.find( Event.class, 2L, 1 );
		assertFalse( firstRevision.getTitle().equals( myEvent.getTitle() ) );
		assertFalse( firstRevision.getDate().equals( myEvent.getDate() ) );
		Event secondRevision = reader.find( Event.class, 2L, 2 );
		assertTrue( secondRevision.getTitle().equals( myEvent.getTitle() ) );
		assertTrue( secondRevision.getDate().equals( myEvent.getDate() ) );
		entityManager.getTransaction().commit();
        entityManager.close();
	}
}
