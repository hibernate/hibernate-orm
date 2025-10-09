/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.Session;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Jpa(annotatedClasses = {
		AutoFlushTest.Person.class,
		AutoFlushTest.Advertisement.class
})
public class AutoFlushTest {
	private final Logger log = Logger.getLogger( AutoFlushTest.class );

	@AfterEach
	void tearDown(EntityManagerFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testFlushAutoCommit(EntityManagerFactoryScope factoryScope) {
		EntityManager entityManager = null;
		EntityTransaction txn = null;

		final var entityManagerFactory = factoryScope.getEntityManagerFactory();
		//tag::flushing-auto-flush-commit-example[]
		entityManager = entityManagerFactory.createEntityManager();
		txn = entityManager.getTransaction();
		txn.begin();

		var person = new Person("John Doe");
		entityManager.persist(person);
		log.info("Entity is in persisted state");

		txn.commit();
		//end::flushing-auto-flush-commit-example[]
	}

	@Test
	public void testFlushAutoJPQL(EntityManagerFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			log.info("testFlushAutoJPQL");
			//tag::flushing-auto-flush-jpql-example[]
			Person person = new Person("John Doe");
			entityManager.persist(person);
			entityManager.createQuery("select p from Advertisement p").getResultList();
			entityManager.createQuery("select p from Person p").getResultList();
			//end::flushing-auto-flush-jpql-example[]
		});
	}

	@Test
	public void testFlushAutoJPQLOverlap(EntityManagerFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			log.info("testFlushAutoJPQLOverlap");
			//tag::flushing-auto-flush-jpql-overlap-example[]
			Person person = new Person("John Doe");
			entityManager.persist(person);
			entityManager.createQuery("select p from Person p").getResultList();
			//end::flushing-auto-flush-jpql-overlap-example[]
		});
	}

	@Test
	public void testFlushAutoSQL(EntityManagerFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			log.info("testFlushAutoSQL");
			//tag::flushing-auto-flush-sql-example[]
			assertEquals( 0, ((Number) entityManager
					.createNativeQuery( "select count(*) from Person" )
					.getSingleResult()).intValue() );

			Person person = new Person("John Doe");
			entityManager.persist(person);

			assertEquals( 1, ((Number) entityManager
					.createNativeQuery( "select count(*) from Person" )
					.getSingleResult()).intValue() );
			//end::flushing-auto-flush-sql-example[]
		});
	}

	@Test
	public void testFlushAutoSQLNativeSession(EntityManagerFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			log.info("testFlushAutoSQLNativeSession");
			//tag::flushing-auto-flush-sql-native-example[]
			assertEquals( 0, ((Number) entityManager
					.createNativeQuery( "select count(*) from Person" )
					.getSingleResult()).intValue() );

			Person person = new Person("John Doe");
			entityManager.persist(person);
			Session session = entityManager.unwrap(Session.class);

			// For this to work, the Session/EntityManager must be put into COMMIT FlushMode
			//  - this is a change since 5.2 to account for merging EntityManager functionality
			// 		directly into Session.  Flushing would be the JPA-spec compliant behavior,
			//		so we know do that by default.
			session.setFlushMode(FlushModeType.COMMIT);
			//		or using Hibernate's FlushMode enum
			//session.setHibernateFlushMode(FlushMode.COMMIT);

			assertEquals( 0, (int) session.createNativeQuery( "select count(*) from Person", Integer.class )
					.uniqueResult() );
			//end::flushing-auto-flush-sql-native-example[]
		});
	}

	@Test
	public void testFlushAutoSQLSynchronization(EntityManagerFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			log.info("testFlushAutoSQLSynchronization");
			//tag::flushing-auto-flush-sql-synchronization-example[]
			assertEquals( 0, ((Number) entityManager
					.createNativeQuery( "select count(*) from Person" )
					.getSingleResult()).intValue() );

			Person person = new Person("John Doe");
			entityManager.persist(person);
			Session session = entityManager.unwrap(Session.class);

			assertEquals( 1, (int) session.createNativeQuery( "select count(*) from Person", Integer.class )
					.addSynchronizedEntityClass( Person.class )
					.uniqueResult() );
			//end::flushing-auto-flush-sql-synchronization-example[]
		});
	}

	//tag::flushing-auto-flush-jpql-entity-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		//Getters and setters are omitted for brevity

	//end::flushing-auto-flush-jpql-entity-example[]

		public Person() {}

		public Person(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	//tag::flushing-auto-flush-jpql-entity-example[]
	}

	@Entity(name = "Advertisement")
	public static class Advertisement {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		//Getters and setters are omitted for brevity

	//end::flushing-auto-flush-jpql-entity-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	//tag::flushing-auto-flush-jpql-entity-example[]
	}
	//end::flushing-auto-flush-jpql-entity-example[]
}
