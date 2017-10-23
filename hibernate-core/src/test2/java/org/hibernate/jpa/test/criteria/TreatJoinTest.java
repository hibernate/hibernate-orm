/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
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
		final EntityManager em = createEntityManager();
		try {
			em.getTransaction().begin();
			try {
				Price price = new Price( 10, "EUR" );
				Author author = new Author( "Andrea Camilleri" );
				Book book = new Book( author, "Il nipote del Negus", price );
				Bid bid = new Bid( book );
				em.persist( bid );

				book = new Book( author, "La moneta di Akragas", price );
				bid = new Bid( book );
				em.persist( bid );

				em.getTransaction().commit();
			}catch (Exception e){
				if(em.getTransaction().isActive()){
					em.getTransaction().rollback();
				}
			}
		}finally {
			em.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8488")
	public void testTreatJoin() {
		EntityManager em = createEntityManager();
		try {

			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Bid> query = cb.createQuery( Bid.class );
			Root<Bid> bid = query.from( Bid.class );

			Join<Bid, Book> book = cb.treat( bid.join( "item" ), Book.class );
			query.select( book.get( "title" ) );

			final List<Bid> resultList = em.createQuery( query ).getResultList();
			assertThat(resultList.size(),is(2));
		}
		finally {
			em.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8488")
	public void testTreatJoin2() {
		EntityManager em = createEntityManager();
		try {

			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Bid> query = cb.createQuery( Bid.class );
			Root<Bid> bid = query.from( Bid.class );

			cb.treat( bid.join( "item" ), Book.class );
			cb.treat( bid.join( "item" ), Car.class );

			query.select( bid );

			final List<Bid> resultList = em.createQuery( query ).getResultList();
			assertThat(resultList.size(),is(2));
		}
		finally {
			em.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8488")
	public void testJoinMethodOnATreatedJoin() {
		EntityManager em = createEntityManager();
		try {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Bid> query = cb.createQuery( Bid.class );
			Root<Bid> bid = query.from( Bid.class );

			final Join<Bid, Book> item = bid.join( "item" );
			final Join<Object, Object> price = item.join( "price" );
			Join<Bid, Book> book = cb.treat( item, Book.class );

			Join<Book, Author> owner = book.join( "author" );
			query.select( owner.get( "name" ) );

			query.where( cb.equal( price.get("amount"), 10 ) );

			final List<Bid> resultList = em.createQuery( query ).getResultList();
			assertThat(resultList.size(),is(2));
		}
		finally {
			em.close();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11081")
	public void testTreatedJoinInWhereClause() {
		EntityManager em = createEntityManager();
		try {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Bid> query = cb.createQuery( Bid.class );
			Root<Bid> bid = query.from( Bid.class );

			final Join<Bid, Book> item = bid.join( "item" );
			Join<Bid, Book> book = cb.treat( item, Book.class );
			query.where( cb.equal( book.get("title"), "La moneta di Akragas" ) );

			final List<Bid> resultList = em.createQuery( query ).getResultList();
			assertThat(resultList.size(),is(1));
		}
		finally {
			em.close();
		}
	}

	@Entity(name = "Item")
	public static class Item {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(cascade = CascadeType.PERSIST)
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

		@ManyToOne(cascade = CascadeType.PERSIST)
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

		@ManyToOne(cascade = CascadeType.PERSIST)
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
