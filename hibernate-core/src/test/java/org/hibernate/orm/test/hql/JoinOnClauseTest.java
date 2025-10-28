/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

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
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Christian Beikov
 */
@SuppressWarnings({"JUnitMalformedDeclaration", "JUnitMixedFramework"})
@DomainModel(annotatedClasses = {
		JoinOnClauseTest.Item.class,
		JoinOnClauseTest.Price.class,
		JoinOnClauseTest.Book.class,
		JoinOnClauseTest.Bid.class,
		JoinOnClauseTest.Author.class,
		JoinOnClauseTest.Car.class,
		JoinOnClauseTest.Person.class
})
@SessionFactory
public class JoinOnClauseTest {

	@BeforeEach
	public void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			Price price = new Price( 10, "EUR" );
			Author author = new Author( "Author 1" );
			Book book = new Book( author, "Book 1", price );
			session.persist( book );

			book = new Book( author, "Book 2", price );
			session.persist( book );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey(value = "HHH-11437")
	public void testOnClauseUsesSuperclassAttribute(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			List<Book> result = session.createQuery( "SELECT DISTINCT b1 FROM Book b1 JOIN Book b2 ON b1.price = b2.price", Book.class )
					.getResultList();
			assertEquals(2, result.size());
		} );
	}

	@Test
	@JiraKey(value = "HHH-11435")
	public void testOnClauseUsesNonDrivingTableAlias(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
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
