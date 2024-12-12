/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.orm.test.hql;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				ManyToOneJoinReuseTest.Book.class,
				ManyToOneJoinReuseTest.BookList.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
public class ManyToOneJoinReuseTest {

	@Test
	@TestForIssue(jiraKey = "HHH-15648")
	public void fetchAndImplicitPath(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					sqlStatementInterceptor.clear();
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					JpaCriteriaQuery<BookList> query = cb.createQuery( BookList.class );

					JpaRoot<BookList> root = query.from( BookList.class );
					root.fetch( "book", JoinType.INNER );
					query.where( root.get( "book" ).isNotNull() );

					session.createQuery( query ).getResultList();
					assertEquals( 1, sqlStatementInterceptor.getSqlQueries().size() );
					assertEquals(
							"select bl1_0.id,b1_0.isbn,b1_0.title from BookList bl1_0 join book b1_0 on b1_0.isbn=bl1_0.book_isbn where b1_0.isbn is not null",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-15645")
	public void joinAndImplicitPath(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					sqlStatementInterceptor.clear();
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					JpaCriteriaQuery<BookList> query = cb.createQuery( BookList.class );

					JpaRoot<BookList> root = query.from( BookList.class );
					Join<Object, Object> join = root.join( "book", JoinType.INNER );
					query.where(
							cb.and(
									root.get( "book" ).isNotNull(),
									cb.fk( root.get( "book" ) ).isNotNull()
							)
					);

					session.createQuery( query ).getResultList();
					assertEquals( 1, sqlStatementInterceptor.getSqlQueries().size() );
					assertEquals(
							"select bl1_0.id,bl1_0.book_isbn from BookList bl1_0 join book b1_0 on b1_0.isbn=bl1_0.book_isbn where b1_0.isbn is not null and bl1_0.book_isbn is not null",
							sqlStatementInterceptor.getSqlQueries().get( 0 )
					);
				}
		);
	}

	@Entity(name = "BookList")
	@Table
	public static class BookList {

		@Id
		private String id;

		@ManyToOne
		private Book book;

	}

	@Entity(name = "Book")
	@Table(name = "book")
	public static class Book {

		@Id
		private String isbn;

		private String title;

	}
}
