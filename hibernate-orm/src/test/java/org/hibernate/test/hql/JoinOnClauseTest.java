/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Christian Beikov
 */
public class JoinOnClauseTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Item.class, Price.class, Book.class, Bid.class, Author.class, Car.class, Person.class};
	}

	@Before
	public void setUp(){
		doInJPA( this::entityManagerFactory, entityManager -> {
			Price price = new Price( 10, "EUR" );
			Author author = new Author( "Author 1" );
			Book book = new Book( author, "Book 1", price );
			entityManager.persist( book );

			book = new Book( author, "Book 2", price );
			entityManager.persist( book );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11437")
	public void testOnClauseUsesSuperclassAttribute() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Book> result = entityManager.createQuery( "SELECT DISTINCT b1 FROM Book b1 JOIN Book b2 ON b1.price = b2.price", Book.class )
				.getResultList();
			assertEquals(2, result.size());
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11435")
	@FailureExpected( jiraKey = "HHH-11435" )
	public void testOnClauseUsesNonDrivingTableAlias() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			try {
				entityManager.createQuery("SELECT b1 FROM Book b1 JOIN Book b2 ON b1.author = author2 LEFT JOIN b2.author author2");
				fail("Referring to a join alias in the on clause that is joined later should be invalid!");
			} catch (IllegalArgumentException ex) {
				// TODO: Assert it fails due to the alias not being defined
			}
		} );
	}

	@Entity(name = "Item")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Item {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
		private Price price;

		public Item() {
		}

		public Item(Price price) {
			this.price = price;
		}
	}

	@Entity(name = "Price")
	public static class Price{
		@Id
		long id;

		public Price() {
		}

		public Price(int amount, String currency) {
			this.amount = amount;
			this.currency = currency;
		}

		int amount;

		String currency;
	}

	@Entity(name = "Book")
	@Table(name="BOOK")
	public static class Book extends Item {
		private String title;

		@ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
		private Author author;

		public Book() {
		}

		public Book(Author author, String title, Price price) {
			super(price);
			this.author = author;
			this.title = title;
		}
	}

	@Entity(name = "Car")
	@Table(name="CAR")
	public static class Car extends Item {
		@OneToMany
		private List<Person> owners;

		String color;
	}

	@Entity(name = "Bid")
	@Table(name = "BID")
	public static class Bid {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
		private Item item;

		public Bid() {
		}

		public Bid(Item item) {
			this.item = item;
		}
	}

	@Entity(name = "Author")
	@Table(name = "AUTHOR")
	public static class Author {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Author() {
		}

		public Author(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Person")
	@Table(name = "PERSON")
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;

		private String name;
	}
}
