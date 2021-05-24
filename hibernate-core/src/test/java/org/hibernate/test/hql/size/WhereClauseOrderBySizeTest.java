/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.hql.size;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.TypedQuery;

import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-14585")
@RequiresDialect(value = PostgreSQL82Dialect.class, comment = "Other databases may not support boolean data types")
@RequiresDialect(value = H2Dialect.class, comment = "Other databases may not support boolean data types")
public class WhereClauseOrderBySizeTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Person.class, Book.class };
	}

	@Test
	public void testSizeAsOrderByExpression() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
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
					final TypedQuery<Person> orderByWorking = entityManager.createQuery(
							"SELECT p FROM Person p ORDER BY p.books.size DESC",
							Person.class
					);

					List<Person> dbPeopleBroken = orderByBroken.getResultList();
					List<Person> dbPeopleWorking = orderByWorking.getResultList();
					assertEquals( Arrays.asList( alice, bob ), dbPeopleWorking );
					assertEquals( dbPeopleWorking, dbPeopleBroken );

					// add 2 books to Bob
					final Book book2 = new Book();
					book2.setOwner( bob );
					entityManager.persist( book2 );

					final Book book3 = new Book();
					book3.setOwner( bob );
					entityManager.persist( book3 );

					dbPeopleBroken = orderByBroken.getResultList();
					dbPeopleWorking = orderByWorking.getResultList();
					assertEquals( Arrays.asList( bob, alice ), dbPeopleWorking );
					assertEquals( dbPeopleWorking, dbPeopleBroken );

					// remove (soft-deleting) both Bob's books
					entityManager.remove( book2 );
					entityManager.remove( book3 );

					// result lists are not equal anymore
					dbPeopleBroken = orderByBroken.getResultList();
					dbPeopleWorking = orderByWorking.getResultList();
					assertEquals( Arrays.asList( alice, bob ), dbPeopleWorking );
					assertEquals( dbPeopleWorking, dbPeopleBroken );
				}
		);
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
	@SQLDelete(sql = "UPDATE Book SET deleted = true WHERE id = ?", check = ResultCheckStyle.COUNT)
	@Where(clause = "deleted = false")
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
