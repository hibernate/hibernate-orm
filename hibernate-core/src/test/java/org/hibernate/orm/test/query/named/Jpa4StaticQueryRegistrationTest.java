/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.named;

import java.util.List;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.query.JakartaQuery;
import jakarta.persistence.query.StaticStatementReference;
import jakarta.persistence.query.StaticTypedQueryReference;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = Jpa4StaticQueryRegistrationTest.Book.class )
@SessionFactory
class Jpa4StaticQueryRegistrationTest {

	@Test
	void registersMethodLevelQueriesAsNamedQueries(SessionFactoryScope scope) {
		final var namedObjectRepository = scope.getSessionFactory()
				.getQueryEngine()
				.getNamedObjectRepository();

		assertThat( namedObjectRepository.getSelectionQueryMemento( "Book.findByTitle" ) ).isNotNull();
		assertThat( namedObjectRepository.getSelectionQueryMemento( "Book.countByTitle" ) ).isNotNull();
		assertThat( namedObjectRepository.getSelectionQueryMemento( "Book.nativeFindByTitle" ) ).isNotNull();
		assertThat( namedObjectRepository.getSelectionQueryMemento( "Book.nativeFindAllByTitle" ) ).isNotNull();
		assertThat( namedObjectRepository.getSelectionQueryMemento( "Book.nativeCountByTitle" ) ).isNotNull();
		assertThat( namedObjectRepository.getSelectionQueryMemento( "Book.nativeTitleByTitle" ) ).isNotNull();
		assertThat( namedObjectRepository.getResultSetMappingMemento( "Book.nativeTitleByTitle" ) ).isNotNull();
		assertThat( namedObjectRepository.getSelectionQueryMemento( "Book.nativeTitleAndIsbnRows" ) ).isNotNull();
		assertThat( namedObjectRepository.getResultSetMappingMemento( "Book.nativeTitleAndIsbnRows" ) ).isNotNull();
		assertThat( namedObjectRepository.getMutationQueryMemento( "Book.deleteByTitle" ) ).isNotNull();
		assertThat( namedObjectRepository.getMutationQueryMemento( "Book.nativeDeleteByTitle" ) ).isNotNull();
	}

	@Test
	void executesRegisteredMethodLevelQueriesByName(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Book( 1, "Hibernate" ) );
			session.persist( new Book( 2, "Jakarta" ) );
		} );

		scope.inTransaction( session -> {
			assertThat( session.createNamedQuery( "Book.findByTitle", Book.class )
					.setParameter( "title", "Hibernate" )
					.getSingleResult()
					.getTitle() ).isEqualTo( "Hibernate" );

			assertThat( session.createNamedQuery( "Book.countByTitle", Long.class )
					.setParameter( "title", "Jakarta" )
					.getSingleResult() ).isEqualTo( 1L );

			assertThat( session.createNamedQuery( "Book.nativeFindByTitle", Book.class )
					.setParameter( 1, "Jakarta" )
					.getSingleResult()
					.getTitle() ).isEqualTo( "Jakarta" );

			assertThat( session.createNamedQuery( "Book.nativeFindAllByTitle", Book.class )
					.setParameter( 1, "Jakarta" )
					.getSingleResult()
					.getTitle() ).isEqualTo( "Jakarta" );

			assertThat( session.createNamedQuery( "Book.nativeCountByTitle", Long.class )
					.setParameter( 1, "Jakarta" )
					.getSingleResult() ).isEqualTo( 1L );

			assertThat( session.createNamedQuery( "Book.nativeTitleByTitle", String.class )
					.setParameter( 1, "Jakarta" )
					.getSingleResult() ).isEqualTo( "Jakarta" );

			assertThat( session.createNamedQuery( "Book.nativeTitleAndIsbnRows", Object[].class )
					.setParameter( 1, "Jakarta" )
					.getSingleResult() ).containsExactly( "Jakarta", "isbn-2" );
		} );
	}

	@Test
	void executesRegisteredMethodLevelQueriesByStaticReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Book( 3, "Reference" ) );
			session.persist( new Book( 4, "NativeReference" ) );
			session.persist( new Book( 5, "DeleteMe" ) );
			session.persist( new Book( 6, "NativeDeleteMe" ) );
		} );

		scope.inTransaction( session -> {
			final var hqlReference = new StaticTypedQueryReference<>(
					"Book.findByTitle",
					Book.class,
					"findByTitle",
					Book.class,
					List.of( String.class ),
					List.of( "title" ),
					List.of( "Reference" )
			);
			assertThat( session.createQuery( hqlReference ).getSingleResult().getTitle() )
					.isEqualTo( "Reference" );

			final var nativeReference = new StaticTypedQueryReference<>(
					"Book.nativeFindByTitle",
					Book.class,
					"nativeFindByTitle",
					Book.class,
					List.of( String.class ),
					List.of( "title" ),
					List.of( "NativeReference" )
			);
			assertThat( session.createQuery( nativeReference ).getSingleResult().getTitle() )
					.isEqualTo( "NativeReference" );

			final var statementReference = new StaticStatementReference(
					"Book.deleteByTitle",
					Book.class,
					"deleteByTitle",
					List.of( String.class ),
					List.of( "title" ),
					List.of( "DeleteMe" )
			);
			assertThat( session.createStatement( statementReference ).executeUpdate() ).isEqualTo( 1 );

			final var nativeStatementReference = new StaticStatementReference(
					"Book.nativeDeleteByTitle",
					Book.class,
					"nativeDeleteByTitle",
					List.of( String.class ),
					List.of( "title" ),
					List.of( "NativeDeleteMe" )
			);
			assertThat( session.createStatement( nativeStatementReference ).executeUpdate() ).isEqualTo( 1 );
		} );
	}

	@Entity( name = "Jpa4StaticQueryBook" )
	@Table( name = "jpa4_static_query_book" )
	public static class Book {
		@Id
		private Integer id;

		private String title;

		private String isbn;

		public Book() {
		}

		private Book(Integer id, String title) {
			this.id = id;
			this.title = title;
			this.isbn = "isbn-" + id;
		}

		public String getTitle() {
			return title;
		}

		@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
		public List<Book> findByTitle(String title) {
			throw new UnsupportedOperationException();
		}

		@JakartaQuery( "select count(book) from Jpa4StaticQueryBook book where book.title = :title" )
		public long countByTitle(String title) {
			throw new UnsupportedOperationException();
		}

		@jakarta.persistence.query.NativeQuery( "select * from jpa4_static_query_book where title = ?" )
		public Book nativeFindByTitle(String title) {
			throw new UnsupportedOperationException();
		}

		@jakarta.persistence.query.NativeQuery( "select * from jpa4_static_query_book where title = ?" )
		public List<Book> nativeFindAllByTitle(String title) {
			throw new UnsupportedOperationException();
		}

		@jakarta.persistence.query.NativeQuery( "select count(*) from jpa4_static_query_book where title = ?" )
		public long nativeCountByTitle(String title) {
			throw new UnsupportedOperationException();
		}

		@ColumnResult( name = "title" )
		@jakarta.persistence.query.NativeQuery( "select title from jpa4_static_query_book where title = ?" )
		public String nativeTitleByTitle(String title) {
			throw new UnsupportedOperationException();
		}

		@ColumnResult( name = "title", type = String.class )
		@ColumnResult( name = "isbn" )
		@jakarta.persistence.query.NativeQuery( "select title, isbn from jpa4_static_query_book where title = ?" )
		public List<Object[]> nativeTitleAndIsbnRows(String title) {
			throw new UnsupportedOperationException();
		}

		@JakartaQuery( "delete from Jpa4StaticQueryBook where title = :title" )
		public int deleteByTitle(String title) {
			throw new UnsupportedOperationException();
		}

		@jakarta.persistence.query.NativeQuery( "delete from jpa4_static_query_book where title = ?" )
		public int nativeDeleteByTitle(String title) {
			throw new UnsupportedOperationException();
		}
	}
}
