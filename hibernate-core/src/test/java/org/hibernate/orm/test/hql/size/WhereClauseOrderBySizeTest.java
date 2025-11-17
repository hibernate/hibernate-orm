/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql.size;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.TypedQuery;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.jdbc.Expectation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-14585")
@RequiresDialect(value = PostgreSQLDialect.class, comment = "Other databases may not support boolean data types")
@RequiresDialect(value = H2Dialect.class, comment = "Other databases may not support boolean data types")
@DomainModel(annotatedClasses = { WhereClauseOrderBySizeTest.Person.class, WhereClauseOrderBySizeTest.Book.class })
@SessionFactory
public class WhereClauseOrderBySizeTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testSizeAsOrderByExpression(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (entityManager) -> {
			// initial situation: Alice has 1 book, Bob none
			final Person alice = new Person( "Alice" );
			entityManager.persist( alice );

			final Book book1 = new Book();
			book1.setOwner( alice );
			entityManager.persist( book1 );

			final Person bob = new Person( "Bob" );
			entityManager.persist( bob );

			final TypedQuery<Person> orderByBroken = entityManager.createQuery(
					"SELECT p FROM Person p ORDER BY size(p.books) DESC",
					Person.class
			);

			List<Person> dbPeopleBroken = orderByBroken.getResultList();
			assertEquals( Arrays.asList( alice, bob ), dbPeopleBroken );

			// add 2 books to Bob
			final Book book2 = new Book();
			book2.setOwner( bob );
			entityManager.persist( book2 );

			final Book book3 = new Book();
			book3.setOwner( bob );
			entityManager.persist( book3 );

			dbPeopleBroken = orderByBroken.getResultList();
			assertEquals( Arrays.asList( bob, alice ), dbPeopleBroken );

			// remove (soft-deleting) both Bob's books
			entityManager.remove( book2 );
			entityManager.remove( book3 );

			// result lists are not equal anymore
			dbPeopleBroken = orderByBroken.getResultList();
			assertEquals( Arrays.asList( alice, bob ), dbPeopleBroken );
		} );
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;
		private String name;
		@OneToMany(mappedBy = "owner", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
		private List<Book> books = new ArrayList<>();

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

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

		public void setBooks(List<Book> books) {
			this.books = books;
		}

		@Override
		public String toString() {
			return "Person{" +
					"id=" + id +
					", name='" + name + '\'' +
					'}';
		}
	}

	@Entity(name = "Book")
	@SQLDelete(sql = "UPDATE Book SET deleted = true WHERE id = ?",
			verify = Expectation.RowCount.class)
	@SQLRestriction("deleted = false")
	public static class Book {
		@Id
		@GeneratedValue
		private Long id;
		private Boolean deleted = false;
		@ManyToOne
		private Person owner;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Boolean getDeleted() {
			return deleted;
		}

		public void setDeleted(Boolean deleted) {
			this.deleted = deleted;
		}

		public Person getOwner() {
			return owner;
		}

		public void setOwner(Person owner) {
			this.owner = owner;
		}
	}
}
