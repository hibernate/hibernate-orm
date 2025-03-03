/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.testing.orm.domain.userguide.Account;
import org.hibernate.testing.orm.domain.userguide.Call;
import org.hibernate.testing.orm.domain.userguide.Partner;
import org.hibernate.testing.orm.domain.userguide.Payment;
import org.hibernate.testing.orm.domain.userguide.Person;
import org.hibernate.testing.orm.domain.userguide.Phone;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class BatchTests extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Phone.class,
			Call.class,
			Account.class,
			Payment.class,
			Partner.class
		};
	}

	@Test
	public void testScroll() {
		withScroll();
	}

	@Test
	public void testStatelessSession() {
		withStatelessSession();
	}

	@Test
	public void testBulk() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			entityManager.persist(new Person("Vlad"));
			entityManager.persist(new Person("Mihalcea"));
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			String oldName = "Vlad";
			String newName = "Alexandru";
			//tag::batch-session-jdbc-batch-size-example[]
			entityManager
				.unwrap(Session.class)
				.setJdbcBatchSize(10);
			//end::batch-session-jdbc-batch-size-example[]
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			String oldName = "Vlad";
			String newName = "Alexandru";
			//tag::batch-bulk-jpql-update-example[]
			int updatedEntities = entityManager.createQuery(
				"update Person p " +
				"set p.name = :newName " +
				"where p.name = :oldName")
			.setParameter("oldName", oldName)
			.setParameter("newName", newName)
			.executeUpdate();
			//end::batch-bulk-jpql-update-example[]
			assertEquals(1, updatedEntities);
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			String oldName = "Alexandru";
			String newName = "Vlad";

			Session session = entityManager.unwrap(Session.class);
			//tag::batch-bulk-hql-update-example[]
			int updatedEntities = session.createMutationQuery(
				"update Person " +
				"set name = :newName " +
				"where name = :oldName")
			.setParameter("oldName", oldName)
			.setParameter("newName", newName)
			.executeUpdate();
			//end::batch-bulk-hql-update-example[]
			assertEquals(1, updatedEntities);
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			String oldName = "Vlad";
			String newName = "Alexandru";

			Session session = entityManager.unwrap(Session.class);
			//tag::batch-bulk-hql-update-version-example[]
			int updatedEntities = session.createMutationQuery(
				"update versioned Person " +
				"set name = :newName " +
				"where name = :oldName")
			.setParameter("oldName", oldName)
			.setParameter("newName", newName)
			.executeUpdate();
			//end::batch-bulk-hql-update-version-example[]
			assertEquals(1, updatedEntities);
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			String name = "Alexandru";

			//tag::batch-bulk-jpql-delete-example[]
			int deletedEntities = entityManager.createQuery(
				"delete Person p " +
				"where p.name = :name")
			.setParameter("name", name)
			.executeUpdate();
			//end::batch-bulk-jpql-delete-example[]
			assertEquals(1, deletedEntities);
		});

		doInJPA(this::entityManagerFactory, entityManager -> {

			Session session = entityManager.unwrap(Session.class);
			//tag::batch-bulk-hql-insert-example[]
			int insertedEntities = session.createMutationQuery(
				"insert into Partner (id, name) " +
				"select p.id, p.name " +
				"from Person p ")
			.executeUpdate();
			//end::batch-bulk-hql-insert-example[]
			assertEquals(1, insertedEntities);
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			String name = "Mihalcea";

			Session session = entityManager.unwrap(Session.class);
			//tag::batch-bulk-hql-delete-example[]
			int deletedEntities = session.createMutationQuery(
				"delete Person " +
				"where name = :name")
			.setParameter("name", name)
			.executeUpdate();
			//end::batch-bulk-hql-delete-example[]
			assertEquals(1, deletedEntities);
		});
	}

	private void withoutBatch() {
		//tag::batch-session-batch-example[]
		EntityManager entityManager = null;
		EntityTransaction txn = null;
		try {
			entityManager = entityManagerFactory().createEntityManager();

			txn = entityManager.getTransaction();
			txn.begin();

			for (int i = 0; i < 100_000; i++) {
				Person Person = new Person(String.format("Person %d", i));
				entityManager.persist(Person);
			}

			txn.commit();
		}
		catch (RuntimeException e) {
			if (txn != null && txn.isActive()) {
				txn.rollback();
			}
			throw e;
		}
		finally {
			if (entityManager != null) {
				entityManager.close();
			}
		}
		//end::batch-session-batch-example[]
	}

	private void withBatch() {
		int entityCount = 100;
		//tag::batch-session-batch-insert-example[]
		EntityManager entityManager = null;
		EntityTransaction txn = null;
		try {
			entityManager = entityManagerFactory().createEntityManager();

			txn = entityManager.getTransaction();
			txn.begin();

			int batchSize = 25;

			for (int i = 0; i < entityCount; i++) {
				if (i > 0 && i % batchSize == 0) {
					//flush a batch of inserts and release memory
					entityManager.flush();
					entityManager.clear();
				}

				Person Person = new Person(String.format("Person %d", i));
				entityManager.persist(Person);
			}

			txn.commit();
		}
		catch (RuntimeException e) {
			if (txn != null && txn.isActive()) {
				txn.rollback();
			}
			throw e;
		}
		finally {
			if (entityManager != null) {
				entityManager.close();
			}
		}
		//end::batch-session-batch-insert-example[]
	}

	private void withScroll() {
		withBatch();

		//tag::batch-session-scroll-example[]
		EntityManager entityManager = null;
		EntityTransaction txn = null;
		ScrollableResults scrollableResults = null;
		try {
			entityManager = entityManagerFactory().createEntityManager();

			txn = entityManager.getTransaction();
			txn.begin();

			int batchSize = 25;

			Session session = entityManager.unwrap(Session.class);

			scrollableResults =
					session.createSelectionQuery("select p from Person p")
							.setCacheMode(CacheMode.IGNORE)
							.scroll(ScrollMode.FORWARD_ONLY);

			int count = 0;
			while (scrollableResults.next()) {
				Person Person = (Person) scrollableResults.get();
				processPerson(Person);
				if (++count % batchSize == 0) {
					//flush a batch of updates and release memory:
					entityManager.flush();
					entityManager.clear();
				}
			}

			txn.commit();
		}
		catch (RuntimeException e) {
			if (txn != null && txn.isActive()) {
				txn.rollback();
			}
			throw e;
		}
		finally {
			if (scrollableResults != null) {
				scrollableResults.close();
			}
			if (entityManager != null) {
				entityManager.close();
			}
		}
		//end::batch-session-scroll-example[]
	}

	private void withStatelessSession() {
		withBatch();

		//tag::batch-stateless-session-example[]
		StatelessSession statelessSession = null;
		Transaction txn = null;
		ScrollableResults<?> scrollableResults = null;
		try {
			SessionFactory sessionFactory = entityManagerFactory().unwrap(SessionFactory.class);
			statelessSession = sessionFactory.openStatelessSession();

			txn = statelessSession.getTransaction();
			txn.begin();

			scrollableResults =
					statelessSession.createSelectionQuery("select p from Person p")
							.scroll(ScrollMode.FORWARD_ONLY);

			while (scrollableResults.next()) {
				Person Person = (Person) scrollableResults.get();
				processPerson(Person);
				statelessSession.update(Person);
			}

			txn.commit();
		}
		catch (RuntimeException e) {
			if (txn != null && txn.getStatus() == TransactionStatus.ACTIVE) {
				txn.rollback();
			}
			throw e;
		}
		finally {
			if (scrollableResults != null) {
				scrollableResults.close();
			}
			if (statelessSession != null) {
				statelessSession.close();
			}
		}
		//end::batch-stateless-session-example[]
	}

	private void processPerson(Person Person) {
		if (Person.getId() % 1000 == 0) {
			log.infof("Processing [%s]", Person.getName());
		}
	}

}
