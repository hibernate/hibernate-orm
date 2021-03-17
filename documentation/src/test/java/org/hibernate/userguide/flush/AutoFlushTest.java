/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.flush;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class AutoFlushTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Advertisement.class
		};
	}

	@Test
	public void testFlushAutoCommit() {
		EntityManager entityManager = null;
		EntityTransaction txn = null;
		try {
			//tag::flushing-auto-flush-commit-example[]
			entityManager = entityManagerFactory().createEntityManager();
			txn = entityManager.getTransaction();
			txn.begin();

			Person person = new Person( "John Doe" );
			entityManager.persist( person );
			log.info( "Entity is in persisted state" );

			txn.commit();
			//end::flushing-auto-flush-commit-example[]
		} catch (RuntimeException e) {
			if ( txn != null && txn.isActive()) txn.rollback();
			throw e;
		} finally {
			if (entityManager != null) {
				entityManager.close();
			}
		}
	}

	@Test
	public void testFlushAutoJPQL() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "testFlushAutoJPQL" );
			//tag::flushing-auto-flush-jpql-example[]
			Person person = new Person( "John Doe" );
			entityManager.persist( person );
			entityManager.createQuery( "select p from Advertisement p" ).getResultList();
			entityManager.createQuery( "select p from Person p" ).getResultList();
			//end::flushing-auto-flush-jpql-example[]
		} );
	}

	@Test
	public void testFlushAutoJPQLOverlap() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "testFlushAutoJPQLOverlap" );
			//tag::flushing-auto-flush-jpql-overlap-example[]
			Person person = new Person( "John Doe" );
			entityManager.persist( person );
			entityManager.createQuery( "select p from Person p" ).getResultList();
			//end::flushing-auto-flush-jpql-overlap-example[]
		} );
	}

	@Test
	public void testFlushAutoSQL() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createNativeQuery( "delete from Person" ).executeUpdate();
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "testFlushAutoSQL" );
			//tag::flushing-auto-flush-sql-example[]
			assertTrue(((Number) entityManager
					.createNativeQuery( "select count(*) from Person")
					.getSingleResult()).intValue() == 0 );

			Person person = new Person( "John Doe" );
			entityManager.persist( person );

			assertTrue(((Number) entityManager
					.createNativeQuery( "select count(*) from Person")
					.getSingleResult()).intValue() == 1 );
			//end::flushing-auto-flush-sql-example[]
		} );
	}

	@Test
	public void testFlushAutoSQLNativeSession() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createNativeQuery( "delete from Person" ).executeUpdate();
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "testFlushAutoSQLNativeSession" );
			//tag::flushing-auto-flush-sql-native-example[]
			assertTrue(((Number) entityManager
					.createNativeQuery( "select count(*) from Person")
					.getSingleResult()).intValue() == 0 );

			Person person = new Person( "John Doe" );
			entityManager.persist( person );
			Session session = entityManager.unwrap(Session.class);

			// For this to work, the Session/EntityManager must be put into COMMIT FlushMode
			//  - this is a change since 5.2 to account for merging EntityManager functionality
			// 		directly into Session.  Flushing would be the JPA-spec compliant behavior,
			//		so we know do that by default.
			session.setFlushMode( FlushModeType.COMMIT );
			//		or using Hibernate's FlushMode enum
			//session.setHibernateFlushMode( FlushMode.COMMIT );

			assertTrue(((Number) session
					.createNativeQuery( "select count(*) from Person")
					.uniqueResult()).intValue() == 0 );
			//end::flushing-auto-flush-sql-native-example[]
		} );
	}

	@Test
	public void testFlushAutoSQLSynchronization() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createNativeQuery( "delete from Person" ).executeUpdate();
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "testFlushAutoSQLSynchronization" );
			//tag::flushing-auto-flush-sql-synchronization-example[]
			assertTrue(((Number) entityManager
					.createNativeQuery( "select count(*) from Person")
					.getSingleResult()).intValue() == 0 );

			Person person = new Person( "John Doe" );
			entityManager.persist( person );
			Session session = entityManager.unwrap( Session.class );

			assertTrue(((Number) session
					.createNativeQuery( "select count(*) from Person")
					.addSynchronizedEntityClass( Person.class )
					.uniqueResult()).intValue() == 1 );
			//end::flushing-auto-flush-sql-synchronization-example[]
		} );
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
