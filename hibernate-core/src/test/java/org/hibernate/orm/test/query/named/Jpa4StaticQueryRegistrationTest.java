/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.named;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Table;
import jakarta.persistence.Timeout;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.query.JakartaQuery;
import jakarta.persistence.query.QueryOptions;
import jakarta.persistence.query.StaticStatementReference;
import jakarta.persistence.query.StaticTypedQueryReference;
import org.hibernate.FlushMode;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.query.Order;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.range.Range;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.specification.MutationSpecification;
import org.hibernate.query.specification.SelectionSpecification;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.SelectionQueryImplementor;
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
		assertThat( namedObjectRepository.getSelectionQueryMemento( "Book.findByTitleWithOptions" ) ).isNotNull();
		assertThat( namedObjectRepository.getSelectionQueryMemento( "Book.nativeFindByTitleWithOptions" ) ).isNotNull();
		assertThat( namedObjectRepository.getResultSetMappingMemento( "Book.nativeTitleByTitle" ) ).isNotNull();
		assertThat( namedObjectRepository.getSelectionQueryMemento( "Book.nativeTitleAndIsbnRows" ) ).isNotNull();
		assertThat( namedObjectRepository.getResultSetMappingMemento( "Book.nativeTitleAndIsbnRows" ) ).isNotNull();
		assertSelectionResultType( namedObjectRepository, "Book.optionalByTitle", Book.class );
		assertSelectionResultType( namedObjectRepository, "Book.arrayByTitle", Book.class );
		assertSelectionResultType( namedObjectRepository, "Book.titleArrayByTitle", String.class );
		assertSelectionResultType( namedObjectRepository, "Book.titleAndIsbnArray", Object[].class );
		assertSelectionResultType( namedObjectRepository, "Book.typedQueryByTitle", Book.class );
		assertSelectionResultType( namedObjectRepository, "Book.queryByTitle", Book.class );
		assertSelectionResultType( namedObjectRepository, "Book.selectionQueryByTitle", Book.class );
		assertSelectionResultType( namedObjectRepository, "Book.keyedResultListByTitle", Book.class );
		assertThat( namedObjectRepository.getNamedQueries( Book.class ) )
				.containsKeys(
						"Book.optionalByTitle",
						"Book.arrayByTitle",
						"Book.typedQueryByTitle",
						"Book.queryByTitle",
						"Book.selectionQueryByTitle",
						"Book.keyedResultListByTitle"
				);
		assertThat( namedObjectRepository.getNamedQueries( String.class ) )
				.containsKey( "Book.titleArrayByTitle" );
		assertThat( namedObjectRepository.getNamedQueries( Object[].class ) )
				.containsKey( "Book.titleAndIsbnArray" );
		assertThat( namedObjectRepository.getMutationQueryMemento( "Book.deleteByTitle" ) ).isNotNull();
		assertThat( namedObjectRepository.getMutationQueryMemento( "Book.deleteByTitleWithOptions" ) ).isNotNull();
		assertThat( namedObjectRepository.getMutationQueryMemento( "Book.nativeDeleteByTitle" ) ).isNotNull();
		assertThat( namedObjectRepository.getMutationQueryMemento( "Book.nativeDeleteByTitleWithOptions" ) ).isNotNull();
	}

	private static void assertSelectionResultType(
			NamedObjectRepository namedObjectRepository,
			String queryName,
			Class<?> resultType) {
		final var memento = namedObjectRepository.getSelectionQueryMemento( queryName );
		assertThat( memento ).isNotNull();
		assertThat( memento.getResultType() ).isEqualTo( resultType );
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
	void appliesStaticQueryOptionsFromNamedQueryMemento(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var queryOptions = session.createNamedQuery( "Book.findByTitleWithOptions", Book.class )
					.unwrap( SelectionQueryImplementor.class )
					.getQueryOptions();
			assertThat( queryOptions.getTimeout() ).isEqualTo( Timeout.milliseconds( 123 ) );
			assertThat( queryOptions.getFlushMode() ).isEqualTo( FlushMode.MANUAL );
			assertThat( queryOptions.getCacheStoreMode() ).isEqualTo( CacheStoreMode.BYPASS );
			assertThat( queryOptions.getLockOptions().getLockMode().toJpaLockMode() )
					.isEqualTo( LockModeType.PESSIMISTIC_READ );
			assertThat( queryOptions.getAppliedGraph().getSemantic() ).isEqualTo( GraphSemantic.LOAD );
			assertThat( queryOptions.getAppliedGraph().getGraph().getName() ).isEqualTo( "Book.summary" );

			final var nativeQueryOptions = session.createNamedQuery( "Book.nativeFindByTitleWithOptions", Book.class )
					.unwrap( SelectionQueryImplementor.class )
					.getQueryOptions();
			assertThat( nativeQueryOptions.getLockOptions().getLockMode().toJpaLockMode() )
					.isEqualTo( LockModeType.PESSIMISTIC_READ );
			assertThat( nativeQueryOptions.getLockOptions().getLockScope() )
					.isEqualTo( PessimisticLockScope.EXTENDED );

			final var statementOptions = session.createNamedMutationQuery( "Book.deleteByTitleWithOptions" )
					.unwrap( MutationQueryImplementor.class )
					.getQueryOptions();
			assertThat( statementOptions.getTimeout() ).isEqualTo( Timeout.milliseconds( 234 ) );
			assertThat( statementOptions.getFlushMode() ).isEqualTo( FlushMode.MANUAL );

			final var nativeStatementOptions = session.createNamedMutationQuery( "Book.nativeDeleteByTitleWithOptions" )
					.unwrap( MutationQueryImplementor.class )
					.getQueryOptions();
			assertThat( nativeStatementOptions.getTimeout() ).isEqualTo( Timeout.milliseconds( 345 ) );
			assertThat( nativeStatementOptions.getFlushMode() ).isEqualTo( FlushMode.ALWAYS );

			final var referenceQueryOptions = session.createQuery( new StaticTypedQueryReference<>(
							"Book.findByTitleWithOptions",
							Book.class,
							"findByTitleWithOptions",
							Book.class,
							List.of( String.class ),
							List.of( "title" ),
							List.of( "Hibernate" ),
							"Book.summary",
							Map.of()
					) )
					.unwrap( SelectionQueryImplementor.class )
					.getQueryOptions();
			assertThat( referenceQueryOptions.getAppliedGraph().getSemantic() ).isEqualTo( GraphSemantic.LOAD );
			assertThat( referenceQueryOptions.getAppliedGraph().getGraph().getName() ).isEqualTo( "Book.summary" );
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

	@Test
	void createsSelectionSpecificationFromTypedQueryReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Book( 7, "SpecificationReference" ) );
			session.persist( new Book( 8, "SpecificationReference" ) );
		} );

		scope.inTransaction( session -> {
			final var reference = new StaticTypedQueryReference<>(
					"Book.findByTitle",
					Book.class,
					"findByTitle",
					Book.class,
					List.of( String.class ),
					List.of( "title" ),
					List.of( "SpecificationReference" )
			);

			final var specification = SelectionSpecification.create( reference )
					.restrict( Restriction.restrict( Book.class, "isbn", Range.singleValue( "isbn-8" ) ) )
					.sort( Order.desc( Book.class, "id" ) );

			final var books = specification
					.createQuery( session )
					.getResultList();

			assertThat( books )
					.extracting( Book::getIsbn )
					.containsExactly( "isbn-8" );
			assertThat( session.createQuery( specification.reference() ).getResultList() )
					.extracting( Book::getIsbn )
					.containsExactly( "isbn-8" );
		} );
	}

	@Test
	void createsMutationSpecificationFromStatementReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Book( 9, "SpecificationDelete" ) );
			session.persist( new Book( 10, "SpecificationDelete" ) );
		} );

		scope.inTransaction( session -> {
			final var statementReference = new StaticStatementReference(
					"Book.deleteByTitle",
					Book.class,
					"deleteByTitle",
					List.of( String.class ),
					List.of( "title" ),
					List.of( "SpecificationDelete" )
			);
			final MutationSpecification<Book> specification = MutationSpecification.create( statementReference );

			final var reference = specification
					.restrict( Restriction.restrict( Book.class, "isbn", Range.singleValue( "isbn-9" ) ) )
					.reference();

			assertThat( session.createStatement( reference ).executeUpdate() ).isEqualTo( 1 );
		} );

		scope.inTransaction( session -> {
			assertThat( session.createSelectionQuery(
							"from Jpa4StaticQueryBook where title = :title",
							Book.class )
					.setParameter( "title", "SpecificationDelete" )
					.getSingleResult()
					.getIsbn() ).isEqualTo( "isbn-10" );
		} );
	}

	@Entity( name = "Jpa4StaticQueryBook" )
	@NamedEntityGraph( name = "Book.summary" )
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

		public String getIsbn() {
			return isbn;
		}

		@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
		public List<Book> findByTitle(String title) {
			throw new UnsupportedOperationException();
		}

		@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
		public Optional<Book> optionalByTitle(String title) {
			throw new UnsupportedOperationException();
		}

		@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
		public Book[] arrayByTitle(String title) {
			throw new UnsupportedOperationException();
		}

		@JakartaQuery( "select book.title from Jpa4StaticQueryBook book where book.title = :title" )
		public String[] titleArrayByTitle(String title) {
			throw new UnsupportedOperationException();
		}

		@JakartaQuery( "select book.title, book.isbn from Jpa4StaticQueryBook book where book.title = :title" )
		public Object[] titleAndIsbnArray(String title) {
			throw new UnsupportedOperationException();
		}

		@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
		public TypedQuery<Book> typedQueryByTitle(String title) {
			throw new UnsupportedOperationException();
		}

		@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
		public org.hibernate.query.Query<Book> queryByTitle(String title) {
			throw new UnsupportedOperationException();
		}

		@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
		public org.hibernate.query.SelectionQuery<Book> selectionQueryByTitle(String title) {
			throw new UnsupportedOperationException();
		}

		@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
		public org.hibernate.query.KeyedResultList<Book> keyedResultListByTitle(String title) {
			throw new UnsupportedOperationException();
		}

		@JakartaQuery( "select count(book) from Jpa4StaticQueryBook book where book.title = :title" )
		public long countByTitle(String title) {
			throw new UnsupportedOperationException();
		}

		@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
		@QueryOptions(
				cacheStoreMode = CacheStoreMode.BYPASS,
				flush = QueryFlushMode.NO_FLUSH,
				timeout = 123,
				lockMode = LockModeType.PESSIMISTIC_READ,
				entityGraph = "Book.summary"
		)
		public List<Book> findByTitleWithOptions(String title) {
			throw new UnsupportedOperationException();
		}

		@jakarta.persistence.query.NativeQuery( "select * from jpa4_static_query_book where title = ?" )
		public Book nativeFindByTitle(String title) {
			throw new UnsupportedOperationException();
		}

		@jakarta.persistence.query.NativeQuery( "select * from jpa4_static_query_book where title = ?" )
		@QueryOptions(
				lockMode = LockModeType.PESSIMISTIC_READ,
				lockScope = PessimisticLockScope.EXTENDED
		)
		public Book nativeFindByTitleWithOptions(String title) {
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

		@JakartaQuery( "delete from Jpa4StaticQueryBook where title = :title" )
		@QueryOptions( flush = QueryFlushMode.NO_FLUSH, timeout = 234 )
		public int deleteByTitleWithOptions(String title) {
			throw new UnsupportedOperationException();
		}

		@jakarta.persistence.query.NativeQuery( "delete from jpa4_static_query_book where title = ?" )
		public int nativeDeleteByTitle(String title) {
			throw new UnsupportedOperationException();
		}

		@jakarta.persistence.query.NativeQuery( "delete from jpa4_static_query_book where title = ?" )
		@QueryOptions( flush = QueryFlushMode.FLUSH, timeout = 345 )
		public int nativeDeleteByTitleWithOptions(String title) {
			throw new UnsupportedOperationException();
		}
	}
}
