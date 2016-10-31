/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.pc;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.PersistenceUtil;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.NaturalId;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class PersistenceContextTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger log = Logger.getLogger( PersistenceContextTest.class );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Book.class,
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::pc-unwrap-example[]
			Session session = entityManager.unwrap( Session.class );
			SessionImplementor sessionImplementor = entityManager.unwrap( SessionImplementor.class );

			SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap( SessionFactory.class );
			//end::pc-unwrap-example[]
		} );
		Long _personId = doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createQuery( "delete from Book" ).executeUpdate();
			entityManager.createQuery( "delete from Person" ).executeUpdate();

			//tag::pc-persist-jpa-example[]
			Person person = new Person();
			person.setId( 1L );
			person.setName("John Doe");

			entityManager.persist( person );
			//end::pc-persist-jpa-example[]

			//tag::pc-remove-jpa-example[]
			entityManager.remove( person );
			//end::pc-remove-jpa-example[]

			entityManager.persist( person );
			Long personId = person.getId();

			//tag::pc-get-reference-jpa-example[]
			Book book = new Book();
			book.setAuthor( entityManager.getReference( Person.class, personId ) );
			//end::pc-get-reference-jpa-example[]

			return personId;
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Long personId = _personId;

			//tag::pc-find-jpa-example[]
			Person person = entityManager.find( Person.class, personId );
			//end::pc-find-jpa-example[]
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			entityManager.createQuery( "delete from Book" ).executeUpdate();
			entityManager.createQuery( "delete from Person" ).executeUpdate();

			//tag::pc-persist-native-example[]
			Person person = new Person();
			person.setId( 1L );
			person.setName("John Doe");

			session.save( person );
			//end::pc-persist-native-example[]

			//tag::pc-remove-native-example[]
			session.delete( person );
			//end::pc-remove-native-example[]

			session.save( person );
			Long personId = person.getId();

			//tag::pc-get-reference-native-example[]
			Book book = new Book();
			book.setId( 1L );
			book.setIsbn( "123-456-7890" );
			entityManager.persist( book );
			book.setAuthor( session.load( Person.class, personId ) );
			//end::pc-get-reference-native-example[]
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			Long personId = _personId;

			//tag::pc-find-native-example[]
			Person person = session.get( Person.class, personId );
			//end::pc-find-native-example[]
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			Long personId = _personId;

			//tag::pc-find-by-id-native-example[]
			Person person = session.byId( Person.class ).load( personId );
			//end::pc-find-by-id-native-example[]

			//tag::pc-find-optional-by-id-native-example[]
			Optional<Person> optionalPerson = session.byId( Person.class ).loadOptional( personId );
			//end::pc-find-optional-by-id-native-example[]

			String isbn = "123-456-7890";

			//tag::pc-find-by-simple-natural-id-example[]
			Book book = session.bySimpleNaturalId( Book.class ).getReference( isbn );
			//end::pc-find-by-simple-natural-id-example[]
			assertNotNull(book);
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			String isbn = "123-456-7890";

			//tag::pc-find-by-natural-id-example[]
			Book book = session
				.byNaturalId( Book.class )
				.using( "isbn", isbn )
				.load( );
			//end::pc-find-by-natural-id-example[]
			assertNotNull(book);

			//tag::pc-find-optional-by-simple-natural-id-example[]
			Optional<Book> optionalBook = session
				.byNaturalId( Book.class )
				.using( "isbn", isbn )
				.loadOptional( );
			//end::pc-find-optional-by-simple-natural-id-example[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Long personId = _personId;

			//tag::pc-managed-state-jpa-example[]
			Person person = entityManager.find( Person.class, personId );
			person.setName("John Doe");
			entityManager.flush();
			//end::pc-managed-state-jpa-example[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Long personId = _personId;

			//tag::pc-refresh-jpa-example[]
			Person person = entityManager.find( Person.class, personId );

			entityManager.createQuery( "update Person set name = UPPER(name)" ).executeUpdate();

			entityManager.refresh( person );
			assertEquals("JOHN DOE", person.getName() );
			//end::pc-refresh-jpa-example[]
		} );

		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				Long personId = _personId;

				//tag::pc-refresh-child-entity-jpa-example[]
				try {
					Person person = entityManager.find( Person.class, personId );

					Book book = new Book();
					book.setId( 100L );
					book.setTitle( "Hibernate User Guide" );
					book.setAuthor( person );
					person.getBooks().add( book );

					entityManager.refresh( person );
				}
				catch ( EntityNotFoundException expected ) {
					log.info( "Beware when cascading the refresh associations to transient entities!" );
				}
				//end::pc-refresh-child-entity-jpa-example[]
			} );
		}
		catch ( Exception expected ) {
		}

		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			Long personId = _personId;

			//tag::pc-managed-state-native-example[]
			Person person = session.byId( Person.class ).load( personId );
			person.setName("John Doe");
			entityManager.flush();
			//end::pc-managed-state-native-example[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			Long personId = _personId;

			//tag::pc-refresh-native-example[]
			Person person = session.byId( Person.class ).load( personId );

			session.doWork( connection -> {
				try(Statement statement = connection.createStatement()) {
					statement.executeUpdate( "UPDATE person SET name = UPPER(name)" );
				}
			} );

			session.refresh( person );
			assertEquals("JOHN DOE", person.getName() );
			//end::pc-refresh-native-example[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			Long personId = _personId;

			//tag::pc-detach-reattach-lock-example[]
			Person person = session.byId( Person.class ).load( personId );
			//Clear the Session so the person entity becomes detached
			session.clear();
			person.setName( "Mr. John Doe" );

			session.lock( person, LockMode.NONE );
			//end::pc-detach-reattach-lock-example[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			Long personId = _personId;

			//tag::pc-detach-reattach-saveOrUpdate-example[]
			Person person = session.byId( Person.class ).load( personId );
			//Clear the Session so the person entity becomes detached
			session.clear();
			person.setName( "Mr. John Doe" );

			session.saveOrUpdate( person );
			//end::pc-detach-reattach-saveOrUpdate-example[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			Long personId = _personId;

			Person personDetachedReference = session.byId( Person.class ).load( personId );
			//Clear the Session so the person entity becomes detached
			session.clear();
			new MergeVisualizer( session ).merge( personDetachedReference );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Long personId = _personId;

			//tag::pc-merge-jpa-example[]
			Person person = entityManager.find( Person.class, personId );
			//Clear the EntityManager so the person entity becomes detached
			entityManager.clear();
			person.setName( "Mr. John Doe" );

			person = entityManager.merge( person );
			//end::pc-merge-jpa-example[]

			//tag::pc-contains-jpa-example[]
			boolean contained = entityManager.contains( person );
			//end::pc-contains-jpa-example[]
			assertTrue( contained );

			//tag::pc-verify-lazy-jpa-example[]
			PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

			boolean personInitialized = persistenceUnitUtil.isLoaded( person );

			boolean personBooksInitialized = persistenceUnitUtil.isLoaded( person.getBooks() );

			boolean personNameInitialized = persistenceUnitUtil.isLoaded( person, "name" );
			//end::pc-verify-lazy-jpa-example[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Long personId = _personId;

			Person person = entityManager.find( Person.class, personId );

			//tag::pc-verify-lazy-jpa-alternative-example[]
			PersistenceUtil persistenceUnitUtil = Persistence.getPersistenceUtil();

			boolean personInitialized = persistenceUnitUtil.isLoaded( person );

			boolean personBooksInitialized = persistenceUnitUtil.isLoaded( person.getBooks() );

			boolean personNameInitialized = persistenceUnitUtil.isLoaded( person, "name" );
			//end::pc-verify-lazy-jpa-alternative-example[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			Long personId = _personId;

			//tag::pc-merge-native-example[]
			Person person = session.byId( Person.class ).load( personId );
			//Clear the Session so the person entity becomes detached
			session.clear();
			person.setName( "Mr. John Doe" );

			person = (Person) session.merge( person );
			//end::pc-merge-native-example[]

			//tag::pc-contains-native-example[]
			boolean contained = session.contains( person );
			//end::pc-contains-native-example[]
			assertTrue( contained );

			//tag::pc-verify-lazy-native-example[]
			boolean personInitialized = Hibernate.isInitialized( person );

			boolean personBooksInitialized = Hibernate.isInitialized( person.getBooks() );

			boolean personNameInitialized = Hibernate.isPropertyInitialized( person, "name" );
			//end::pc-verify-lazy-native-example[]
		} );
	}

	public static class MergeVisualizer {
		private final Session session;

		public MergeVisualizer(Session session) {
			this.session = session;
		}

		//tag::pc-merge-visualize-example[]
		public Person merge(Person detached) {
			Person newReference = session.byId( Person.class ).load( detached.getId() );
			newReference.setName( detached.getName() );
			return newReference;
		}
		//end::pc-merge-visualize-example[]
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
		private List<Book> books = new ArrayList<>(  );

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Book> getBooks() {
			return books;
		}
	}

	//tag::pc-find-by-natural-id-entity-example[]
	@Entity(name = "Book")
	public static class Book {

		@Id
		private Long id;

		private String title;

		@NaturalId
		private String isbn;

		@ManyToOne
		private Person author;

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

		public Person getAuthor() {
			return author;
		}

		public void setAuthor(Person author) {
			this.author = author;
		}

		public String getIsbn() {
			return isbn;
		}

		public void setIsbn(String isbn) {
			this.isbn = isbn;
		}
	}
	//end::pc-find-by-natural-id-entity-example[]
}
