/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.array;

import java.util.Arrays;

import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				ArrayConstructorInSelectClauseTest.Book.class,
				ArrayConstructorInSelectClauseTest.Author.class
		})
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStructuralArrays.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsArrayConstructor.class)
// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
public class ArrayConstructorInSelectClauseTest {
	private static final Long AUTHOR_ID = 1l;
	private static final Long CO_AUTHOR_ID = 2l;
	private static final Long BOOK_ID = 3l;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					var dialect = session.getDialect();
					if ( dialect instanceof OracleDialect ) {
						session.createNativeQuery(
										"create or replace type LongArray as varying array(127) of number(19,0)" )
								.executeUpdate();
					}
					var author = new Author( AUTHOR_ID, "D Thomas" );
					var coAuthor = new Author( CO_AUTHOR_ID, "A Hunt" );
					session.persist( author );
					session.persist( coAuthor );

					var book = new Book( BOOK_ID, "The Pragmatic Programmer", author, coAuthor );
					session.persist( book );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					scope.getSessionFactory().getSchemaManager().truncate();
					if ( session.getDialect() instanceof OracleDialect ) {
						session.createNativeQuery( "drop type LongArray" ).executeUpdate();
					}
				}
		);
	}

	@Test
	@Jira("HHH-18353")
	public void testArraySelectionNoCast(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					var query = session.createQuery(
							"SELECT ARRAY(b.author.id, b.coAuthor.id) FROM Book b WHERE b.id = :bookId",
							Long[].class
					).setParameter( "bookId", BOOK_ID );

					assertThat( query.getSingleResult() ).containsAll( Arrays.asList( AUTHOR_ID, CO_AUTHOR_ID ) );
				}
		);
	}

	@Test
	public void testArraySelectionCast(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					var query = session.createQuery(
							"SELECT ARRAY(CAST(b.author.id as Long), CAST(b.coAuthor.id as Long)) FROM Book b WHERE b.id = :bookId",
							Long[].class
					).setParameter( "bookId", BOOK_ID );

					assertThat( query.getSingleResult() ).containsAll( Arrays.asList( AUTHOR_ID, CO_AUTHOR_ID ) );
				}
		);
	}

	@Entity(name = "Author")
	public static class Author {

		@Id
		private Long id;

		private String name;

		public Author() {
		}

		public Author(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Book")
	public static class Book {

		@Id
		private Long id;

		private String title;

		@ManyToOne
		private Author author;

		@ManyToOne
		private Author coAuthor;

		public Book() {
		}

		public Book(long id, String title, Author author, Author coAuthor) {
			this.id = id;
			this.title = title;
			this.author = author;
			this.coAuthor = coAuthor;
		}
	}
}
