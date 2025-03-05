/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.List;
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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class TreatJoinTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Item.class, Price.class, Book.class, Bid.class, Author.class, Car.class, Person.class};
	}

	@Before
	public void setUp(){
		doInJPA( this::entityManagerFactory, entityManager -> {
			Price price = new Price( 10, "EUR" );
			Author author = new Author( "Andrea Camilleri" );
			Book book = new Book( author, "Il nipote del Negus", price );
			book.setDescription( "is a book" );
			Bid bid = new Bid( book );
			entityManager.persist( bid );

			book = new Book( author, "La moneta di Akragas", price );
			bid = new Bid( book );
			entityManager.persist( bid );
		} );
	}

	@Test
	@JiraKey(value = "HHH-8488")
	public void testTreatJoin() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<String> query = cb.createQuery( String.class );
			Root<Bid> bid = query.from( Bid.class );

			Join<Bid, Book> book = cb.treat( bid.join( "item" ), Book.class );
			query.select( book.get( "title" ) );

			final List<String> resultList = entityManager.createQuery( query ).getResultList();
			assertThat(resultList.size(),is(2));
		} );
	}

	@Test
	@JiraKey(value = "HHH-8488")
	public void testTreatJoin2() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Bid> query = cb.createQuery( Bid.class );
			Root<Bid> bid = query.from( Bid.class );

			Join<Object, Object> item = bid.join( "item" );
			cb.treat( item, Book.class );
			cb.treat( item, Car.class );

			query.select( bid );

			final List<Bid> resultList = entityManager.createQuery( query ).getResultList();
			assertThat(resultList.size(),is(2));
		} );
	}

	@Test
	@JiraKey(value = "HHH-8488")
	public void testJoinMethodOnATreatedJoin() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<String> query = cb.createQuery( String.class );
			Root<Bid> bid = query.from( Bid.class );

			final Join<Bid, Book> item = bid.join( "item" );
			final Join<Object, Object> price = item.join( "price" );
			Join<Bid, Book> book = cb.treat( item, Book.class );

			Join<Book, Author> owner = book.join( "author" );
			query.select( owner.get( "name" ) );

			query.where( cb.equal( price.get("amount"), 10 ) );

			final List<String> resultList = entityManager.createQuery( query ).getResultList();
			assertThat(resultList.size(),is(2));
		} );
	}

	@Test
	@JiraKey( value = "HHH-11081")
	public void testTreatedJoinInWhereClause() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Bid> query = cb.createQuery( Bid.class );
			Root<Bid> bid = query.from( Bid.class );

			final Join<Bid, Book> item = bid.join( "item" );
			Join<Bid, Book> book = cb.treat( item, Book.class );
			query.where( cb.equal( book.get("title"), "La moneta di Akragas" ) );

			final List<Bid> resultList = entityManager.createQuery( query ).getResultList();
			assertThat(resultList.size(),is(1));
		} );
	}

	@Test
	@JiraKey(value = "HHH-10561")
	public void testJoinOnTreatedRoot() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
			Root<Item> root = criteria.from(Item.class);
			Root<Book> treatedRoot =  cb.treat(root, Book.class);
			criteria.where(
					cb.equal(
							treatedRoot.<Book, Author>join("author").<String>get("name"),
							"Andrea Camilleri"));
			final List<Item> resultList = entityManager.createQuery( criteria.select( treatedRoot ) ).getResultList();
			final Item item = resultList.get( 0 );
			assertThat( item, instanceOf(Book.class) );
			assertEquals( "is a book", item.getDescription() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-10561")
	public void testJoinOnTreatedRootWithJoin() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Item> criteria = cb.createQuery(Item.class);
			Root<Item> root = criteria.from(Item.class);
			root.join( "price" );
			Root<Book> treatedRoot =  cb.treat(root, Book.class);
			criteria.where(
					cb.equal(
							treatedRoot.<Book, Author>join("author").<String>get("name"),
							"Andrea Camilleri"));
			entityManager.createQuery(criteria.select(treatedRoot)).getResultList();
		} );
	}

	@Test
	@JiraKey(value = "HHH-10767")
	public void testJoinOnTreatedJoin() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Bid> criteria = cb.createQuery(Bid.class);
			Root<Bid> root = criteria.from(Bid.class);
			Join<Book, Author> join = cb.treat(
					root.<Bid, Item> join("item"), Book.class)
					.join("author");
			criteria.where(cb.equal(join.<String> get("name"), "Andrea Camilleri"));
			entityManager.createQuery(criteria.select(root)).getResultList();
		} );
	}

	@Entity(name = "Item")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Item {
		@Id
		@GeneratedValue
		private Long id;

		private String description;

		@ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
		private Price price;

		public Item() {
		}

		public Item(Price price) {
			this.price = price;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
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
