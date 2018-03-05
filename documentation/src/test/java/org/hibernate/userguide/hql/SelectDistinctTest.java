/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.hql;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class SelectDistinctTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Book.class
		};
	}

	@Before
	public void init() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person gavinKing = new Person("Gavin", "King" );
			Person stephanKing = new Person("Stephen", "King" );
			Person vladMihalcea = new Person("Vlad", "Mihalcea" );

			gavinKing.addBook( new Book( "Hibernate in Action" ) );
			gavinKing.addBook( new Book( "Java Persistence with Hibernate" ) );

			stephanKing.addBook( new Book( "The Green Mile" ) );

			vladMihalcea.addBook( new Book( "High-Performance Java Persistence" ) );

			entityManager.persist( gavinKing );
			entityManager.persist( stephanKing );
			entityManager.persist( vladMihalcea );
		});
	}

	@Test
	public void testDistinctProjection() {

        doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-distinct-projection-query-example[]
            List<String> lastNames = entityManager.createQuery(
				"select distinct p.lastName " +
				"from Person p", String.class)
			.getResultList();
			//end::hql-distinct-projection-query-example[]

			assertTrue(
				lastNames.size() == 2 &&
				lastNames.contains( "King" ) &&
				lastNames.contains( "Mihalcea" )
			);
		});
	}

	@Test
	public void testAllAuthors() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Person> authors = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"left join fetch p.books", Person.class)
			.getResultList();

			authors.forEach( author -> {
				log.infof( "Author %s wrote %d books",
				   author.getFirstName() + " " + author.getLastName(),
				   author.getBooks().size()
				);
			} );
		});
	}

	@Test
	public void testDistinctAuthors() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-distinct-entity-query-example[]
			List<Person> authors = entityManager.createQuery(
				"select distinct p " +
				"from Person p " +
				"left join fetch p.books", Person.class)
			.getResultList();
			//end::hql-distinct-entity-query-example[]

			authors.forEach( author -> {
				log.infof( "Author %s wrote %d books",
				   author.getFirstName() + " " + author.getLastName(),
				   author.getBooks().size()
				);
			} );
		});
	}

	@Test
	public void testDistinctAuthorsWithoutPassThrough() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::hql-distinct-entity-query-hint-example[]
			List<Person> authors = entityManager.createQuery(
				"select distinct p " +
				"from Person p " +
				"left join fetch p.books", Person.class)
			.setHint( QueryHints.HINT_PASS_DISTINCT_THROUGH, false )
			.getResultList();
			//end::hql-distinct-entity-query-hint-example[]

			authors.forEach( author -> {
				log.infof( "Author %s wrote %d books",
				   author.getFirstName() + " " + author.getLastName(),
				   author.getBooks().size()
				);
			} );
		});
	}

	@Entity(name = "Person") @Table( name = "person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "first_name")
		private String firstName;

		@Column(name = "last_name")
		private String lastName;

		@OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
		private List<Book> books = new ArrayList<>(  );

		public Person() {
		}

		public Person(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public List<Book> getBooks() {
			return books;
		}

		public void addBook(Book book) {
			books.add( book );
			book.setAuthor( this );
		}
	}

	@Entity(name = "Book") @Table( name = "book")
	public static class Book {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		@ManyToOne
		private Person author;

		public Book() {
		}

		public Book(String title) {
			this.title = title;
		}

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
	}
}
