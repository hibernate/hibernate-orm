/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.named;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Timeout;
import jakarta.persistence.query.JakartaQuery;
import jakarta.persistence.query.StaticStatementReference;
import jakarta.persistence.query.StaticTypedQueryReference;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.query.Order;
import org.hibernate.query.named.spi.NamedObjectRepository;
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

@DomainModel( annotatedClasses = {
		Book.class,
		BookRepository.class,
		CompanionRepository$.class,
		Jpa4StaticQueryRegistrationTest.NestedQueries.class
} )
@SessionFactory
class Jpa4StaticQueryRegistrationTest {

	private static final String BOOK_FIND_BY_TITLE = queryName( Book.class, "findByTitle", String.class );
	private static final String BOOK_BLANK_FIND_ALL = queryName( Book.class, "blankFindAll" );
	private static final String BOOK_OPTIONAL_BY_TITLE = queryName( Book.class, "optionalByTitle", String.class );
	private static final String BOOK_ARRAY_BY_TITLE = queryName( Book.class, "arrayByTitle", String.class );
	private static final String BOOK_TITLE_ARRAY_BY_TITLE = queryName( Book.class, "titleArrayByTitle", String.class );
	private static final String BOOK_TITLE_AND_ISBN_ARRAY = queryName( Book.class, "titleAndIsbnArray", String.class );
	private static final String BOOK_TYPED_QUERY_BY_TITLE = queryName( Book.class, "typedQueryByTitle", String.class );
	private static final String BOOK_QUERY_BY_TITLE = queryName( Book.class, "queryByTitle", String.class );
	private static final String BOOK_SELECTION_QUERY_BY_TITLE = queryName( Book.class, "selectionQueryByTitle", String.class );
	private static final String BOOK_KEYED_RESULT_LIST_BY_TITLE = queryName( Book.class, "keyedResultListByTitle", String.class );
	private static final String BOOK_COUNT_BY_TITLE = queryName( Book.class, "countByTitle", String.class );
	private static final String BOOK_FIND_BY_TITLE_WITH_OPTIONS =
			queryName( Book.class, "findByTitleWithOptions", String.class );
	private static final String BOOK_NATIVE_FIND_BY_TITLE = queryName( Book.class, "nativeFindByTitle", String.class );
	private static final String BOOK_NATIVE_FIND_BY_TITLE_WITH_OPTIONS =
			queryName( Book.class, "nativeFindByTitleWithOptions", String.class );
	private static final String BOOK_NATIVE_FIND_ALL_BY_TITLE =
			queryName( Book.class, "nativeFindAllByTitle", String.class );
	private static final String BOOK_NATIVE_COUNT_BY_TITLE = queryName( Book.class, "nativeCountByTitle", String.class );
	private static final String BOOK_NATIVE_TITLE_BY_TITLE = queryName( Book.class, "nativeTitleByTitle", String.class );
	private static final String BOOK_NATIVE_TITLE_AND_ISBN_ROWS =
			queryName( Book.class, "nativeTitleAndIsbnRows", String.class );
	private static final String BOOK_DELETE_BY_TITLE = queryName( Book.class, "deleteByTitle", String.class );
	private static final String BOOK_DELETE_BY_TITLE_WITH_OPTIONS =
			queryName( Book.class, "deleteByTitleWithOptions", String.class );
	private static final String BOOK_NATIVE_DELETE_BY_TITLE =
			queryName( Book.class, "nativeDeleteByTitle", String.class );
	private static final String BOOK_NATIVE_DELETE_BY_TITLE_WITH_OPTIONS =
			queryName( Book.class, "nativeDeleteByTitleWithOptions", String.class );
	private static final String BOOK_REPOSITORY_INHERITED_FIND_BY_TITLE =
			queryName( BookRepository.class, "inheritedFindByTitle", String.class );
	private static final String BOOK_REPOSITORY_INHERITED_GENERIC_FIND_BY_TITLE =
			queryName( BookRepository.class, "inheritedGenericFindByTitle", String.class );
	private static final String COMPANION_REPOSITORY_FIND_BY_TITLE =
			queryName( CompanionRepository$.class, "findByTitle", String.class );
	private static final String NESTED_QUERIES_FIND_BY_TITLE =
			queryName( NestedQueries.class, "findByTitle", String.class );

	@Test
	void registersMethodLevelQueriesAsNamedQueries(SessionFactoryScope scope) {
		final var namedObjectRepository = scope.getSessionFactory()
				.getQueryEngine()
				.getNamedObjectRepository();

		assertThat( namedObjectRepository.getSelectionQueryMemento( BOOK_FIND_BY_TITLE ) ).isNotNull();
		assertThat( namedObjectRepository.getSelectionQueryMemento( BOOK_COUNT_BY_TITLE ) ).isNotNull();
		assertThat( namedObjectRepository.getSelectionQueryMemento( BOOK_BLANK_FIND_ALL ).getSelectionString() )
				.isEqualTo( "from Jpa4StaticQueryBook" );
		assertThat( namedObjectRepository.getSelectionQueryMemento( BOOK_NATIVE_FIND_BY_TITLE ) ).isNotNull();
		assertThat( namedObjectRepository.getSelectionQueryMemento( BOOK_NATIVE_FIND_ALL_BY_TITLE ) ).isNotNull();
		assertThat( namedObjectRepository.getSelectionQueryMemento( BOOK_NATIVE_COUNT_BY_TITLE ) ).isNotNull();
		assertThat( namedObjectRepository.getSelectionQueryMemento( BOOK_NATIVE_TITLE_BY_TITLE ) ).isNotNull();
		assertThat( namedObjectRepository.getSelectionQueryMemento( BOOK_FIND_BY_TITLE_WITH_OPTIONS ) ).isNotNull();
		assertThat( namedObjectRepository.getSelectionQueryMemento( BOOK_NATIVE_FIND_BY_TITLE_WITH_OPTIONS ) ).isNotNull();
		assertThat( namedObjectRepository.getResultSetMappingMemento( BOOK_NATIVE_TITLE_BY_TITLE ) ).isNotNull();
		assertThat( namedObjectRepository.getSelectionQueryMemento( BOOK_NATIVE_TITLE_AND_ISBN_ROWS ) ).isNotNull();
		assertThat( namedObjectRepository.getResultSetMappingMemento( BOOK_NATIVE_TITLE_AND_ISBN_ROWS ) ).isNotNull();
		assertSelectionResultType( namedObjectRepository, BOOK_OPTIONAL_BY_TITLE, Book.class );
		assertSelectionResultType( namedObjectRepository, BOOK_ARRAY_BY_TITLE, Book.class );
		assertSelectionResultType( namedObjectRepository, BOOK_TITLE_ARRAY_BY_TITLE, String.class );
		assertSelectionResultType( namedObjectRepository, BOOK_TITLE_AND_ISBN_ARRAY, Object[].class );
		assertSelectionResultType( namedObjectRepository, BOOK_TYPED_QUERY_BY_TITLE, Book.class );
		assertSelectionResultType( namedObjectRepository, BOOK_QUERY_BY_TITLE, Book.class );
		assertSelectionResultType( namedObjectRepository, BOOK_SELECTION_QUERY_BY_TITLE, Book.class );
		assertSelectionResultType( namedObjectRepository, BOOK_KEYED_RESULT_LIST_BY_TITLE, Book.class );
		assertThat( namedObjectRepository.getNamedQueries( Book.class ) )
				.containsKeys(
						BOOK_OPTIONAL_BY_TITLE,
						BOOK_ARRAY_BY_TITLE,
						BOOK_TYPED_QUERY_BY_TITLE,
						BOOK_QUERY_BY_TITLE,
						BOOK_SELECTION_QUERY_BY_TITLE,
						BOOK_KEYED_RESULT_LIST_BY_TITLE
				);
		assertThat( namedObjectRepository.getNamedQueries( String.class ) )
				.containsKey( BOOK_TITLE_ARRAY_BY_TITLE );
		assertThat( namedObjectRepository.getNamedQueries( Object[].class ) )
				.containsKey( BOOK_TITLE_AND_ISBN_ARRAY );
		assertThat( namedObjectRepository.getMutationQueryMemento( BOOK_DELETE_BY_TITLE ) ).isNotNull();
		assertThat( namedObjectRepository.getMutationQueryMemento( BOOK_DELETE_BY_TITLE_WITH_OPTIONS ) ).isNotNull();
		assertThat( namedObjectRepository.getMutationQueryMemento( BOOK_NATIVE_DELETE_BY_TITLE ) ).isNotNull();
		assertThat( namedObjectRepository.getMutationQueryMemento( BOOK_NATIVE_DELETE_BY_TITLE_WITH_OPTIONS ) ).isNotNull();
	}

	@Test
	void registersInheritedRepositoryMethodLevelQueriesAsNamedQueries(SessionFactoryScope scope) {
		final var namedObjectRepository = scope.getSessionFactory()
				.getQueryEngine()
				.getNamedObjectRepository();

		assertSelectionResultType( namedObjectRepository, BOOK_REPOSITORY_INHERITED_FIND_BY_TITLE, Book.class );
		assertSelectionResultType( namedObjectRepository, BOOK_REPOSITORY_INHERITED_GENERIC_FIND_BY_TITLE, Book.class );
	}

	@Test
	void usesJavadocLinkStyleQueryNames(SessionFactoryScope scope) {
		final var namedObjectRepository = scope.getSessionFactory()
				.getQueryEngine()
				.getNamedObjectRepository();

		assertThat( NESTED_QUERIES_FIND_BY_TITLE )
				.isEqualTo( Jpa4StaticQueryRegistrationTest.class.getName()
						+ ".NestedQueries#findByTitle(java.lang.String)" );
		assertSelectionResultType( namedObjectRepository, NESTED_QUERIES_FIND_BY_TITLE, Book.class );

		assertThat( COMPANION_REPOSITORY_FIND_BY_TITLE )
				.isEqualTo( CompanionRepository$.class.getName() + "#findByTitle(java.lang.String)" );
		assertSelectionResultType( namedObjectRepository, COMPANION_REPOSITORY_FIND_BY_TITLE, Book.class );
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
			assertThat( session.createNamedQuery( BOOK_FIND_BY_TITLE, Book.class )
					.setParameter( "title", "Hibernate" )
					.getSingleResult()
					.getTitle() ).isEqualTo( "Hibernate" );

			assertThat( session.createNamedQuery( BOOK_COUNT_BY_TITLE, Long.class )
					.setParameter( "title", "Jakarta" )
					.getSingleResult() ).isEqualTo( 1L );

			assertThat( session.createQuery( new StaticTypedQueryReference<>(
							BOOK_BLANK_FIND_ALL,
							Book.class,
							"blankFindAll",
							Book.class,
							List.of(),
							List.of(),
							List.of()
					) )
					.getResultList() )
					.extracting( Book::getTitle )
					.contains( "Hibernate", "Jakarta" );

			assertThat( session.createNamedQuery( BOOK_NATIVE_FIND_BY_TITLE, Book.class )
					.setParameter( 1, "Jakarta" )
					.getSingleResult()
					.getTitle() ).isEqualTo( "Jakarta" );

			assertThat( session.createNamedQuery( BOOK_NATIVE_FIND_ALL_BY_TITLE, Book.class )
					.setParameter( 1, "Jakarta" )
					.getSingleResult()
					.getTitle() ).isEqualTo( "Jakarta" );

			assertThat( session.createNamedQuery( BOOK_NATIVE_COUNT_BY_TITLE, Long.class )
					.setParameter( 1, "Jakarta" )
					.getSingleResult() ).isEqualTo( 1L );

			assertThat( session.createNamedQuery( BOOK_NATIVE_TITLE_BY_TITLE, String.class )
					.setParameter( 1, "Jakarta" )
					.getSingleResult() ).isEqualTo( "Jakarta" );

			assertThat( session.createNamedQuery( BOOK_NATIVE_TITLE_AND_ISBN_ROWS, Object[].class )
					.setParameter( 1, "Jakarta" )
					.getSingleResult() ).containsExactly( "Jakarta", "isbn-2" );
		} );
	}

	@Test
	void executesInheritedRepositoryMethodLevelQueriesByStaticReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Book( 11, "InheritedReference" ) );
			session.persist( new Book( 12, "InheritedGenericReference" ) );
		} );

		scope.inTransaction( session -> {
			final var inheritedReference = new StaticTypedQueryReference<>(
					BOOK_REPOSITORY_INHERITED_FIND_BY_TITLE,
					BookRepository.class,
					"inheritedFindByTitle",
					Book.class,
					List.of( String.class ),
					List.of( "title" ),
					List.of( "InheritedReference" )
			);
			assertThat( session.createQuery( inheritedReference ).getSingleResult().getTitle() )
					.isEqualTo( "InheritedReference" );

			final var inheritedGenericReference = new StaticTypedQueryReference<>(
					BOOK_REPOSITORY_INHERITED_GENERIC_FIND_BY_TITLE,
					BookRepository.class,
					"inheritedGenericFindByTitle",
					Book.class,
					List.of( String.class ),
					List.of( "title" ),
					List.of( "InheritedGenericReference" )
			);
			assertThat( session.createQuery( inheritedGenericReference ).getSingleResult().getTitle() )
					.isEqualTo( "InheritedGenericReference" );
		} );
	}

	@Test
	void appliesStaticQueryOptionsFromNamedQueryMemento(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var queryOptions = session.createNamedQuery( BOOK_FIND_BY_TITLE_WITH_OPTIONS, Book.class )
					.unwrap( SelectionQueryImplementor.class )
					.getQueryOptions();
			assertThat( queryOptions.getTimeout() ).isEqualTo( Timeout.milliseconds( 123 ) );
			assertThat( queryOptions.getQueryFlushMode() ).isEqualTo( QueryFlushMode.NO_FLUSH );
			assertThat( queryOptions.getCacheStoreMode() ).isEqualTo( CacheStoreMode.BYPASS );
			assertThat( queryOptions.getLockOptions().getLockMode().toJpaLockMode() )
					.isEqualTo( LockModeType.PESSIMISTIC_READ );
			assertThat( queryOptions.getAppliedGraph().getSemantic() ).isEqualTo( GraphSemantic.LOAD );
			assertThat( queryOptions.getAppliedGraph().getGraph().getName() ).isEqualTo( "Book.summary" );

			final var nativeQueryOptions = session.createNamedQuery( BOOK_NATIVE_FIND_BY_TITLE_WITH_OPTIONS, Book.class )
					.unwrap( SelectionQueryImplementor.class )
					.getQueryOptions();
			assertThat( nativeQueryOptions.getLockOptions().getLockMode().toJpaLockMode() )
					.isEqualTo( LockModeType.PESSIMISTIC_READ );
			assertThat( nativeQueryOptions.getLockOptions().getLockScope() )
					.isEqualTo( PessimisticLockScope.EXTENDED );

			final var statementOptions = session.createNamedMutationQuery( BOOK_DELETE_BY_TITLE_WITH_OPTIONS )
					.unwrap( MutationQueryImplementor.class )
					.getQueryOptions();
			assertThat( statementOptions.getTimeout() ).isEqualTo( Timeout.milliseconds( 234 ) );
			assertThat( statementOptions.getQueryFlushMode() ).isEqualTo( QueryFlushMode.NO_FLUSH );

			final var nativeStatementOptions = session.createNamedMutationQuery( BOOK_NATIVE_DELETE_BY_TITLE_WITH_OPTIONS )
					.unwrap( MutationQueryImplementor.class )
					.getQueryOptions();
			assertThat( nativeStatementOptions.getTimeout() ).isEqualTo( Timeout.milliseconds( 345 ) );
			assertThat( nativeStatementOptions.getQueryFlushMode() ).isEqualTo( QueryFlushMode.FLUSH );

			final var referenceQueryOptions = session.createQuery( new StaticTypedQueryReference<>(
							BOOK_FIND_BY_TITLE_WITH_OPTIONS,
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
					BOOK_FIND_BY_TITLE,
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
					BOOK_NATIVE_FIND_BY_TITLE,
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
					BOOK_DELETE_BY_TITLE,
					Book.class,
					"deleteByTitle",
					List.of( String.class ),
					List.of( "title" ),
					List.of( "DeleteMe" )
			);
			assertThat( session.createStatement( statementReference ).executeUpdate() ).isEqualTo( 1 );

			final var nativeStatementReference = new StaticStatementReference(
					BOOK_NATIVE_DELETE_BY_TITLE,
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
					BOOK_FIND_BY_TITLE,
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
					BOOK_DELETE_BY_TITLE,
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

	private static String queryName(Class<?> type, String methodName, Class<?>... parameterTypes) {
		final StringJoiner name = new StringJoiner( ",", javadocTypeName( type ) + "#" + methodName + "(", ")" );
		for ( Class<?> parameterType : parameterTypes ) {
			name.add( parameterType.getCanonicalName() );
		}
		return name.toString();
	}

	private static String javadocTypeName(Class<?> type) {
		final String className = type.getName();
		return className.endsWith( "$" ) ? className : type.getCanonicalName();
	}

	abstract static class NestedQueries {
		@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
		abstract List<Book> findByTitle(String title);
	}

}
