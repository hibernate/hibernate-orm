/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.orm.test.hql;

import java.util.Set;

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

/**
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				ManyToOneJoinReuseTest.Book.class,
				ManyToOneJoinReuseTest.BookList.class
		}
)
@SessionFactory(statementInspectorClass = SQLStatementInspector.class)
public class ManyToOneJoinReuseTest {

	@Test
	@TestForIssue(jiraKey = "HHH-15648")
	public void fetchAndImplicitPath(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = (SQLStatementInspector) scope.getStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					JpaCriteriaQuery<BookList> query = cb.createQuery( BookList.class );

					JpaRoot<BookList> root = query.from( BookList.class );
					root.fetch( "book", JoinType.INNER );
					query.where( root.get( "book" ).isNotNull() );

					session.createQuery( query ).getResultList();
					sqlStatementInterceptor.assertExecuted( "select b1_0.id,b2_0.isbn,b2_0.title from BookList b1_0 join book b2_0 on b2_0.isbn=b1_0.book_isbn where b2_0.isbn is not null" );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-15645")
	public void joinAndImplicitPath(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = (SQLStatementInspector) scope.getStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					JpaCriteriaQuery<BookList> query = cb.createQuery( BookList.class );

					JpaRoot<BookList> root = query.from( BookList.class );
					Join<Object, Object> join = root.join( "book", JoinType.INNER );
					query.where(
							cb.and(
									root.get( "book" ).isNotNull(),
									join.isNotNull()
							)
					);

					session.createQuery( query ).getResultList();
					sqlStatementInterceptor.assertExecuted( "select b1_0.id,b1_0.book_isbn from BookList b1_0 join book b2_0 on b2_0.isbn=b1_0.book_isbn where b2_0.isbn is not null and b1_0.book_isbn is not null" );
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
