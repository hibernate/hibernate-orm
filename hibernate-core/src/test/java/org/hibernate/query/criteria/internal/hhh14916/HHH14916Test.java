package org.hibernate.query.criteria.internal.hhh14916;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

@TestForIssue( jiraKey = "HHH-14916" )
public class HHH14916Test extends BaseEntityManagerFunctionalTestCase {
	
	@Before
	public void before() {
		populateData();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Author.class, Book.class, Chapter.class };
	}

	@Test
	public void testJoinOnFetchNoExceptionThrow() {
		doInJPA( this::entityManagerFactory, entityManager -> {

			final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Author> query = builder.createQuery(Author.class);

			final Root<Author> root = query.from(Author.class);
			final ListJoin<Author, Book> authorBookJoin =  (ListJoin)root.fetch("books", JoinType.LEFT);

			final ListJoin<Book, Chapter> bookChapterJoin = authorBookJoin.joinList("chapters", JoinType.LEFT);

			final Predicate finalPredicate = builder.equal(bookChapterJoin.get("name"), "Overview of HTTP");
			query.where(finalPredicate);

			Author author = entityManager.createQuery(query).getSingleResult();

			assertEquals(author.name, "David Gourley");
			assertEquals(author.books.get(0).name, "HTTP Definitive guide");
			assertEquals(author.books.get(0).chapters.get(0).name, "Overview of HTTP");
		} );
	}

	public void populateData() {
		doInJPA(this::entityManagerFactory, entityManager -> {	
			// Insert data
			Chapter chapter = new Chapter();
			chapter.name = "Overview of HTTP";
			
			Book book = new Book();
			book.name = "HTTP Definitive guide";
	
			Author author = new Author();
			author.name = "David Gourley";
	
			book.chapters.add(chapter);
			author.books.add(book);
	
			chapter.book = book;
			book.author = author;
	
			entityManager.persist(author);
		});
	}
}
