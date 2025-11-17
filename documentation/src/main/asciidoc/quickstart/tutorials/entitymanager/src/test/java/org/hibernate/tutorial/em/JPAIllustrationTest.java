/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tutorial.em;

import java.util.function.Consumer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import junit.framework.TestCase;

import static java.lang.System.out;
import static java.time.LocalDateTime.now;

import static jakarta.persistence.Persistence.createEntityManagerFactory;


/**
 * Illustrates basic use of Hibernate as a Jakarta Persistence provider.
 * Configuration properties are sourced from persistence.xml.
 *
 * @author Steve Ebersole
 */
public class JPAIllustrationTest extends TestCase {
	private EntityManagerFactory entityManagerFactory;

	@Override
	protected void setUp() {
		// an EntityManagerFactory is set up once for an application
		// IMPORTANT: notice how the name here matches the name we
		// gave the persistence-unit in persistence.xml
		entityManagerFactory = createEntityManagerFactory("org.hibernate.tutorial.jpa");
	}

	@Override
	protected void tearDown() {
		entityManagerFactory.close();
	}

	public void testBasicUsage() {
		// create a couple of events...
		inTransaction(entityManager -> {
			entityManager.persist(new Event("Our very first event!", now()));
			entityManager.persist(new Event("A follow up event", now()));
		});

		// now lets pull events from the database and list them
		inTransaction(entityManager -> {
			entityManager.createQuery("select e from Event e", Event.class).getResultList()
					.forEach(event -> out.println("Event (" + event.getDate() + ") : " + event.getTitle()));
		});
	}

	void inTransaction(Consumer<EntityManager> work) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		EntityTransaction transaction = entityManager.getTransaction();
		try {
			transaction.begin();
			work.accept(entityManager);
			transaction.commit();
		}
		catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
	}

}
