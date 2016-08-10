/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria;

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
import java.util.List;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10844")
public class TreatJoinTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Item.class, Price.class, Book.class, Bid.class, Author.class, Car.class, Person.class};
	}

	@Test
	public void testTreatJoin() {
		EntityManager em = createEntityManager();
		try {

			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Bid> query = cb.createQuery( Bid.class );
			Root<Bid> bid = query.from( Bid.class );

			Join<Bid, Book> book = cb.treat( bid.join( "item" ), Book.class );
			query.select( book.get( "title" ) );

			em.createQuery( query ).getResultList();
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testTreatJoin2() {
		EntityManager em = createEntityManager();
		try {

			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Bid> query = cb.createQuery( Bid.class );
			Root<Bid> bid = query.from( Bid.class );

			cb.treat( bid.join( "item" ), Book.class );
			cb.treat( bid.join( "item" ), Car.class );

			query.select( bid );

			em.createQuery( query ).getResultList();
		}
		finally {
			em.close();
		}
	}

	@Test
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

			query.where( cb.equal( price.get("amount"), 1L ) );

			em.createQuery( query ).getResultList();

		}
		finally {
			em.close();
		}
	}

	@Entity
	public static class Item {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private Price price;

	}

	@Entity
	public static class Price{
		@Id
		long id;

		int amount;

		String currency;
	}

	@Entity(name = "Book")
	@Table(name="BOOK")
	public static class Book extends Item {
		@ManyToOne
		private Author author;

		String title;
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

		@ManyToOne
		private Item item;
	}

	@Entity(name = "Author")
	@Table(name = "AUTHOR")
	public static class Author {
		@Id
		@GeneratedValue
		private Long id;

		private String name;
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
