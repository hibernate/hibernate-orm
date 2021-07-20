package org.hibernate.query;

import java.util.List;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Игорь Маслов
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-14200" )
public class CountOnCollectionValuedAttributeTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Book.class,
				Author.class,
				Address.class
		};
	}

	@Test
	public void testNoInvalidSqlGenerated_ManyToMany() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final String hql = "SELECT b.title FROM Book b GROUP BY b.title HAVING count(b.authors) > 1";
			entityManager.createQuery( hql, String.class ).getResultList();
		} );
	}

	@Test
	public void testNoInvalidSqlGenerated_OneToMany() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final String hql = "SELECT a.name FROM Author a GROUP BY a.name HAVING count(a.addresses) > 1";
			entityManager.createQuery( hql, String.class ).getResultList();
		} );
	}

	@Test
	public void testNoInvalidSqlGenerated_ElementCollection() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final String hql = "SELECT b.title FROM Book b GROUP BY b.title HAVING count(b.tags) > 1";
			entityManager.createQuery( hql, String.class ).getResultList();
		} );
	}

	@Entity(name = "Book")
	static class Book {

		@Id
		int bookId;

		String title;

		@ManyToMany
		List<Author> authors;

		@ElementCollection
		Set<String> tags;

	}

	@Entity(name = "Author")
	static class Author {

		@Id
		int authorId;

		String name;

		@ManyToMany(mappedBy = "authors")
		List<Book> books;

		@OneToMany(mappedBy = "author")
		List<Address> addresses;
	}

	@Entity(name = "Address")
	static class Address {

		@Id
		int addressId;

		@ManyToOne
		Author author;

	}
}
