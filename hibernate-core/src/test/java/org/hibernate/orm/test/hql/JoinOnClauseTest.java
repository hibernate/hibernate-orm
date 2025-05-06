/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
	@JiraKey(value = "HHH-11437")
	public void testOnClauseUsesSuperclassAttribute() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Book> result = entityManager.createQuery( "SELECT DISTINCT b1 FROM Book b1 JOIN Book b2 ON b1.price = b2.price", Book.class )
				.getResultList();
			assertEquals(2, result.size());
		} );
	}

	@Test
	@JiraKey(value = "HHH-11435")
	public void testOnClauseUsesNonDrivingTableAlias() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			try {
				entityManager.createQuery(
						"SELECT b1 FROM Book b1 JOIN Book b2 ON b1.author = author2 LEFT JOIN b2.author author2" );
				fail( "Referring to a join alias in the on clause that is joined later should be invalid!" );
			}
			catch (IllegalArgumentException ex) {
				assertTrue( ex.getCause().getMessage().contains( "'author2'" ) );
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
