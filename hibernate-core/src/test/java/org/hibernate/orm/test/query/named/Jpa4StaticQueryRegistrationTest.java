/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.named;

import java.util.List;
import java.util.Map;

import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
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

@DomainModel( annotatedClasses = {
		Book.class,
		BookRepository.class
} )
@SessionFactory
class Jpa4StaticQueryRegistrationTest {

	@Test
	void registersMethodLevelQueriesAsNamedQueries(SessionFactoryScope scope) {
		final var namedObjectRepository = scope.getSessionFactory()
				.getQueryEngine()
				.getNamedObjectRepository();

		assertThat( namedObjectRepository.getSelectionQueryMemento( "Book.findByTitle" ) ).isNotNull();
		assertThat( namedObjectRepository.getSelectionQueryMemento( "Book.countByTitle" ) ).isNotNull();
		assertThat( namedObjectRepository.getSelectionQueryMemento( "Book.blankFindAll" ).getSelectionString() )
				.isEqualTo( "from Jpa4StaticQueryBook" );
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

	@Test
	void registersInheritedRepositoryMethodLevelQueriesAsNamedQueries(SessionFactoryScope scope) {
		final var namedObjectRepository = scope.getSessionFactory()
				.getQueryEngine()
				.getNamedObjectRepository();

		assertSelectionResultType( namedObjectRepository, "BookRepository.inheritedFindByTitle", Book.class );
		assertSelectionResultType( namedObjectRepository, "BookRepository.inheritedGenericFindByTitle", Book.class );
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

			assertThat( session.createQuery( new StaticTypedQueryReference<>(
							"Book.blankFindAll",
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
	void executesInheritedRepositoryMethodLevelQueriesByStaticReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Book( 11, "InheritedReference" ) );
			session.persist( new Book( 12, "InheritedGenericReference" ) );
		} );

		scope.inTransaction( session -> {
			final var inheritedReference = new StaticTypedQueryReference<>(
					"BookRepository.inheritedFindByTitle",
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
					"BookRepository.inheritedGenericFindByTitle",
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

}
