package org.hibernate.jpa.test.userguide.flush;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.jpa.test.util.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * <code>AutoFlushTest</code> - Auto Flush Test
 *
 * @author Vlad Mihalcea
 */
public class AutoFlushTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger log = Logger.getLogger( AutoFlushTest.class );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Phone.class,
		};
	}

	@Test
	public void testFlushAutoCommit() {
		EntityManager entityManager = null;
		EntityTransaction txn = null;
		try {
			//tag::auto-flush-commit[]
			entityManager = entityManagerFactory().createEntityManager();
			txn = entityManager.getTransaction();
			txn.begin();

			Person person = new Person( "Vlad" );
			log.info( "Entity is in persisted state" );

			txn.commit();
			//end::auto-flush-commit[]
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
			Person person = new Person( "Vlad" );
			entityManager.createQuery( "select p from Phone p" ).getResultList();
			entityManager.createQuery( "select p from Person p" ).getResultList();
		} );
	}

	@Test
	public void testFlushAutoJPQLOverlap() {
		final Person vlad = doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person( "Vlad" );
			entityManager.persist( person );
			return person;
		} );
		log.info( "Add Phone" );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Phone phone = new Phone( "1234567890" );
			vlad.addPhone( phone );
			entityManager.persist( phone );
			Person person = entityManager.createQuery( "select p from Person p", Person.class).getSingleResult();
			assertEquals(1, person.getPhones().size());
		} );
	}

	@Test
	public void testFlushAutoJPQLFlush() {
		final Person vlad = doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person( "Vlad" );
			entityManager.persist( person );
			return person;
		} );
		log.info( "Add Phone and flush" );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Phone phone = new Phone( "1234567890" );
			vlad.addPhone( phone );
			entityManager.persist( phone );
			Person person = entityManager.createQuery( "select p from Person p", Person.class).getSingleResult();
			entityManager.flush();
			assertEquals(1, person.getPhones().size());
			entityManager.createQuery( "select p from Phone p" ).getResultList();
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
		private List<Phone> phones = new ArrayList<>(  );

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

		public List<Phone> getPhones() {
			return phones;
		}

		public void addPhone(Phone phone) {
			phones.add( phone );
			phone.setPerson( this );
		}
	}

	@Entity(name = "Phone")
	public static class Phone {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private Person person;

		private String number;

		public Phone() {
		}

		public Phone(String number) {
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public String getNumber() {
			return number;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	}
}
