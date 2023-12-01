/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.locking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockScope;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.jboss.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses =  {
				ExplicitLockingTest.Person.class,
				ExplicitLockingTest.Phone.class,
		}
)
public class ExplicitLockingTest {

	protected final Logger log = Logger.getLogger( getClass() );

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager ->
						entityManager.createQuery( "delete from Person" ).executeUpdate()
		);
	}

	@Test
	public void testJPALockTimeout(EntityManagerFactoryScope scope) {
		Person p = scope.fromTransaction( entityManager -> {
			Person person = new Person("John Doe");
			entityManager.persist(person);
			return person;
		});
		scope.inTransaction( entityManager -> {
			log.info("testJPALockTimeout");
			Long id = p.getId();
			//tag::locking-jpa-query-hints-timeout-example[]
			entityManager.find(
				Person.class, id, LockModeType.PESSIMISTIC_WRITE,
				Collections.singletonMap("jakarta.persistence.lock.timeout", 200)
			);
			//end::locking-jpa-query-hints-timeout-example[]
		});
	}

	@Test
	public void testJPALockScope(EntityManagerFactoryScope scope) {
		Person p = scope.fromTransaction( entityManager -> {
			Person person = new Person("John Doe");
			entityManager.persist(person);
			Phone home = new Phone("123-456-7890");
			Phone office = new Phone("098-765-4321");
			person.getPhones().add(home);
			person.getPhones().add(office);
			entityManager.persist(person);
			return person;
		});
		scope.inTransaction( entityManager -> {
			log.info("testJPALockScope");
			Long id = p.getId();
			//tag::locking-jpa-query-hints-scope-example[]
			Person person = entityManager.find(
				Person.class, id, LockModeType.PESSIMISTIC_WRITE,
				Collections.singletonMap(
					"jakarta.persistence.lock.scope",
					PessimisticLockScope.EXTENDED)
			);
			//end::locking-jpa-query-hints-scope-example[]
			assertEquals(2, person.getPhones().size());
		});
	}

	@Test
	public void testSessionLock(EntityManagerFactoryScope scope) {
		Person p = scope.fromTransaction( entityManager -> {
			log.info("testSessionLock");
			Person person = new Person("John Doe");
			Phone home = new Phone("123-456-7890");
			Phone office = new Phone("098-765-4321");
			person.getPhones().add(home);
			person.getPhones().add(office);
			entityManager.persist(person);
			entityManager.flush();
			return person;
		});
		scope.inTransaction( entityManager -> {
			Long id = p.getId();
			//tag::locking-session-lock-example[]
			Person person = entityManager.find(Person.class, id);
			Session session = entityManager.unwrap(Session.class);
			LockOptions lockOptions = new LockOptions(LockMode.PESSIMISTIC_READ, LockOptions.NO_WAIT);
			session.lock(person, lockOptions);
			//end::locking-session-lock-example[]
		});

		scope.inTransaction( entityManager -> {
			Long id = p.getId();
			//tag::locking-session-lock-scope-example[]
			Person person = entityManager.find(Person.class, id);
			Session session = entityManager.unwrap(Session.class);
			LockOptions lockOptions = new LockOptions(LockMode.PESSIMISTIC_READ, LockOptions.NO_WAIT, PessimisticLockScope.EXTENDED);
			session.lock(person, lockOptions);
			//end::locking-session-lock-scope-example[]
		});

	}

	@Test
	@RequiresDialect(value = OracleDialect.class)
	public void testFollowOnLocking(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			log.info("testBuildLockRequest");
			Person person1 = new Person("John Doe");
			Person person2 = new Person("Mrs. John Doe");

			entityManager.persist(person1);
			entityManager.persist(person2);
			entityManager.flush();
		});

		scope.inTransaction( entityManager -> {
			//tag::locking-follow-on-example[]
			List<Person> persons = entityManager.createQuery(
				"select DISTINCT p from Person p", Person.class)
			.setLockMode(LockModeType.PESSIMISTIC_WRITE)
			.getResultList();
			//end::locking-follow-on-example[]
		});

		scope.inTransaction( entityManager -> {
			//tag::locking-follow-on-secondary-query-example[]
			List<Person> persons = entityManager.createQuery(
				"select DISTINCT p from Person p", Person.class)
			.getResultList();

			entityManager.createQuery(
				"select p.id from Person p where p in :persons")
			.setLockMode(LockModeType.PESSIMISTIC_WRITE)
			.setParameter("persons", persons)
			.getResultList();
			//end::locking-follow-on-secondary-query-example[]
		});

		scope.inTransaction( entityManager -> {
			//tag::locking-follow-on-explicit-example[]
			List<Person> persons = entityManager.createQuery(
				"select p from Person p", Person.class)
			.setMaxResults(10)
			.unwrap(Query.class)
			.setLockOptions(
				new LockOptions(LockMode.PESSIMISTIC_WRITE)
					.setFollowOnLocking(false))
			.getResultList();
			//end::locking-follow-on-explicit-example[]
		});
	}

	@Test
	@JiraKey("HHH-16672")
	public void persistAndLock(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			Person person = new Person("John Doe");
			entityManager.persist(person);
			entityManager.lock(person, LockModeType.PESSIMISTIC_WRITE);
		});
	}

	@Test
	@JiraKey("HHH-16672")
	public void persistFindAndLock(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			Person person = new Person("John Doe");
			entityManager.persist(person);
			assertNotNull(person.id);

			Person foundPerson = entityManager.find(Person.class, person.id);
			entityManager.lock(foundPerson, LockModeType.PESSIMISTIC_WRITE);
		});
	}

	@Test
	@JiraKey("HHH-16672")
	public void persistFindWithLock(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			Person person = new Person("John Doe");
			entityManager.persist(person);
			assertNotNull(person.id);

			Person foundPerson = entityManager.find(Person.class, person.id, LockModeType.PESSIMISTIC_WRITE);
			assertNotNull(foundPerson);
		});
	}


	//tag::locking-jpa-query-hints-scope-entity-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`name`")
		private String name;

		@ElementCollection
		@JoinTable(name = "person_phone", joinColumns = @JoinColumn(name = "person_id"))
		private List<Phone> phones = new ArrayList<>();

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<Phone> getPhones() {
			return phones;
		}
	}

	@Embeddable
	public static class Phone {

		@Column
		private String mobile;

		public Phone() {}

		public Phone(String mobile) {
			this.mobile = mobile;
		}
	}
	//end::locking-jpa-query-hints-scope-entity-example[]
}
