/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStatement;
import jakarta.persistence.StatementReference;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.query.StaticStatementReference;
import jakarta.persistence.query.StaticTypedQueryReference;
import org.hibernate.orm.test.query.criteria.CriteriaBuilderAugmentTest_.Book_;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = CriteriaBuilderAugmentTest.Book.class )
@SessionFactory
class CriteriaBuilderAugmentTest {

	private static final String FIND_BY_TITLE = "CriteriaBuilderAugmentTest.Book.findByTitle";
	private static final String DELETE_BY_TITLE = "CriteriaBuilderAugmentTest.Book.deleteByTitle";

	@Test
	void augmentsTypedQueryReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Book( 1, "typed-overload", "isbn-typed-1" ) );
			session.persist( new Book( 2, "typed-overload", "isbn-typed-2" ) );
		} );

		scope.inTransaction( session -> {
			final CriteriaBuilder builder = session.getCriteriaBuilder();
			final TypedQueryReference<Book> augmentedReference = builder.augment(
					findByTitleReference( "typed-overload" ),
					query -> {
						final Root<Book> book = getRoot( query, Book.class );
						Predicate existingRestriction = query.getRestriction();
						Predicate restriction = builder.equal( book.get( Book_.isbn ), "isbn-typed-2" );
						query.where( existingRestriction == null ? restriction
								: builder.and( existingRestriction, restriction ) );
					}
			);

			assertThat( session.createQuery( augmentedReference ).getSingleResult().getIsbn() )
					.isEqualTo( "isbn-typed-2" );
		} );
	}

	@Test
	void augmentsTypedQueryReferenceWithDifferentResultType(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Book( 11, "result-type-overload", "isbn-result-type-1" ) );
			session.persist( new Book( 12, "result-type-overload", "isbn-result-type-2" ) );
		} );

		scope.inTransaction( session -> {
			final CriteriaBuilder builder = session.getCriteriaBuilder();
			final TypedQueryReference<String> augmentedReference = builder.augment(
					findByTitleReference( "result-type-overload" ),
					String.class,
					query -> {
						final Root<Book> book = getRoot( query, Book.class );
						query.select( book.get( Book_.isbn ) );
						Predicate existingRestriction = query.getRestriction();
						Predicate restriction = builder.equal( book.get( Book_.isbn ), "isbn-result-type-2" );
						query.where( existingRestriction == null ? restriction
								: builder.and( existingRestriction, restriction ) );
					}
			);

			assertThat( session.createQuery( augmentedReference ).getSingleResult() )
					.isEqualTo( "isbn-result-type-2" );
		} );
	}

	@Test
	void augmentsStatementReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Book( 21, "statement-overload", "isbn-statement-1" ) );
			session.persist( new Book( 22, "statement-overload", "isbn-statement-2" ) );
			session.persist( new Book( 23, "statement-unmatched", "isbn-statement-3" ) );
		} );

		scope.inTransaction( session -> {
			final CriteriaBuilder builder = session.getCriteriaBuilder();
			final StatementReference augmentedReference = builder.augment(
					deleteByTitleReference( "statement-overload" ),
					statement -> {
						@SuppressWarnings("unchecked")
						final var delete = (CriteriaDelete<Book>) statement;
						final var book = delete.getRoot();
						Predicate existingRestriction = statement.getRestriction();
						Predicate restriction = builder.equal( book.get( Book_.isbn ), "isbn-statement-1" );
						delete.where( existingRestriction == null ? restriction
								: builder.and( existingRestriction, restriction ) );
					}
			);

			assertThat( session.createStatement( augmentedReference ).executeUpdate() )
					.isEqualTo( 1 );
		} );

		scope.inTransaction( session -> assertThat( session.createSelectionQuery(
						"select book.isbn from CriteriaBuilderAugmentBook book where book.title = :title order by book.id",
						String.class
				)
				.setParameter( "title", "statement-overload" )
				.getResultList() )
				.containsExactly( "isbn-statement-2" ) );
	}

	private static TypedQueryReference<Book> findByTitleReference(String title) {
		return new StaticTypedQueryReference<>(
				FIND_BY_TITLE,
				Book.class,
				"findByTitle",
				Book.class,
				List.of( String.class ),
				List.of( "title" ),
				List.of( title )
		);
	}

	private static StatementReference deleteByTitleReference(String title) {
		return new StaticStatementReference(
				DELETE_BY_TITLE,
				Book.class,
				"deleteByTitle",
				List.of( String.class ),
				List.of( "title" ),
				List.of( title )
		);
	}

	@SuppressWarnings("unchecked")
	private static <T> Root<T> getRoot(CriteriaQuery<?> query, Class<T> entityType) {
		return (Root<T>) query.getRoots()
				.stream()
				.filter( root -> entityType.equals( root.getJavaType() ) )
				.findFirst()
				.orElseThrow();
	}

	@Entity( name = "CriteriaBuilderAugmentBook" )
	@Table( name = "criteria_builder_augment_book" )
	@NamedQuery(
			name = FIND_BY_TITLE,
			query = "from CriteriaBuilderAugmentBook book where book.title = :title"
	)
	@NamedStatement(
			name = DELETE_BY_TITLE,
			statement = "delete from CriteriaBuilderAugmentBook book where book.title = :title"
	)
	public static class Book {
		@Id
		private Integer id;

		private String title;

		private String isbn;

		public Book() {
		}

		private Book(Integer id, String title, String isbn) {
			this.id = id;
			this.title = title;
			this.isbn = isbn;
		}

		public String getIsbn() {
			return isbn;
		}
	}
}
