/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetchmode.toone;

import java.util.List;
import java.util.Locale;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfileOverride;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				ToOneWithSubselectFetchModeTests.Author.class,
				ToOneWithSubselectFetchModeTests.FetchBook.class,
				ToOneWithSubselectFetchModeTests.FetchOverrideBook.class,
				ToOneWithSubselectFetchModeTests.FetchProfileOverrideBook.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
class ToOneWithSubselectFetchModeTests {
	private static final String FETCH_OVERRIDE_PROFILE = "fetch-override-book-author";
	private static final String FETCH_PROFILE_OVERRIDE_PROFILE = "fetch-profile-override-book-author";

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var gavin = new Author( 1L, "Gavin" );
			final var steve = new Author( 2L, "Steve" );
			session.persist( gavin );
			session.persist( steve );

			session.persist( new FetchBook( 1L, "Hibernate in Action", gavin ) );
			session.persist( new FetchBook( 2L, "Hibernate Reactive", steve ) );
			session.persist( new FetchOverrideBook( 1L, "Hibernate in Action", gavin ) );
			session.persist( new FetchOverrideBook( 2L, "Hibernate Reactive", steve ) );
			session.persist( new FetchProfileOverrideBook( 1L, "Hibernate in Action", gavin ) );
			session.persist( new FetchProfileOverrideBook( 2L, "Hibernate Reactive", steve ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void queryWithToOneFetchModeSubselect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final var books = session.createQuery(
					"from ToOneSubselectFetchBook b order by b.id",
					FetchBook.class
			).getResultList();

			assertFetchBooks( books );
		} );

		assertSubselectFetch( inspector.getSqlQueries() );
	}

	@Test
	void criteriaQueryWithToOneFetchModeSubselect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final var criteriaBuilder = session.getCriteriaBuilder();
			final var criteriaQuery = criteriaBuilder.createQuery( FetchBook.class );
			final var book = criteriaQuery.from( FetchBook.class );
			criteriaQuery.select( book )
					.orderBy( criteriaBuilder.asc( book.get( "id" ) ) );

			final var books = session.createQuery( criteriaQuery ).getResultList();

			assertFetchBooks( books );
		} );

		assertSubselectFetch( inspector.getSqlQueries() );
	}

	@Test
	void queryWithToOneFetchOverrideModeSubselect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			session.enableFetchProfile( FETCH_OVERRIDE_PROFILE );
			final var books = session.createQuery(
					"from ToOneSubselectFetchOverrideBook b order by b.id",
					FetchOverrideBook.class
			).getResultList();

			assertFetchOverrideBooks( books );
		} );

		assertSubselectFetch( inspector.getSqlQueries() );
	}

	@Test
	void criteriaQueryWithToOneFetchOverrideModeSubselect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			session.enableFetchProfile( FETCH_OVERRIDE_PROFILE );
			final var criteriaBuilder = session.getCriteriaBuilder();
			final var criteriaQuery = criteriaBuilder.createQuery( FetchOverrideBook.class );
			final var book = criteriaQuery.from( FetchOverrideBook.class );
			criteriaQuery.select( book )
					.orderBy( criteriaBuilder.asc( book.get( "id" ) ) );

			final var books = session.createQuery( criteriaQuery ).getResultList();

			assertFetchOverrideBooks( books );
		} );

		assertSubselectFetch( inspector.getSqlQueries() );
	}

	@Test
	void queryWithToOneFetchProfileOverrideModeSubselect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			session.enableFetchProfile( FETCH_PROFILE_OVERRIDE_PROFILE );
			final var books = session.createQuery(
					"from ToOneSubselectFetchProfileOverrideBook b order by b.id",
					FetchProfileOverrideBook.class
			).getResultList();

			assertFetchProfileOverrideBooks( books );
		} );

		assertSubselectFetch( inspector.getSqlQueries() );
	}

	@Test
	void criteriaQueryWithToOneFetchProfileOverrideModeSubselect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			session.enableFetchProfile( FETCH_PROFILE_OVERRIDE_PROFILE );
			final var criteriaBuilder = session.getCriteriaBuilder();
			final var criteriaQuery = criteriaBuilder.createQuery( FetchProfileOverrideBook.class );
			final var book = criteriaQuery.from( FetchProfileOverrideBook.class );
			criteriaQuery.select( book )
					.orderBy( criteriaBuilder.asc( book.get( "id" ) ) );

			final var books = session.createQuery( criteriaQuery ).getResultList();

			assertFetchProfileOverrideBooks( books );
		} );

		assertSubselectFetch( inspector.getSqlQueries() );
	}

	private static void assertSubselectFetch(List<String> sqlQueries) {
		assertThat( sqlQueries ).hasSize( 2 );
		assertThat( sqlQueries.get( 0 ).toLowerCase( Locale.ROOT ) )
				.doesNotContain( " join " );
		assertThat( sqlQueries.get( 1 ).toLowerCase( Locale.ROOT ) )
				.doesNotContain( " join " )
				.contains( " in (select " );
	}

	private static void assertFetchBooks(List<FetchBook> books) {
		assertThat( books ).hasSize( 2 );
		assertBook( books.get( 0 ), "Hibernate in Action", "Gavin" );
		assertBook( books.get( 1 ), "Hibernate Reactive", "Steve" );
	}

	private static void assertFetchOverrideBooks(List<FetchOverrideBook> books) {
		assertThat( books ).hasSize( 2 );
		assertBook( books.get( 0 ), "Hibernate in Action", "Gavin" );
		assertBook( books.get( 1 ), "Hibernate Reactive", "Steve" );
	}

	private static void assertFetchProfileOverrideBooks(List<FetchProfileOverrideBook> books) {
		assertThat( books ).hasSize( 2 );
		assertBook( books.get( 0 ), "Hibernate in Action", "Gavin" );
		assertBook( books.get( 1 ), "Hibernate Reactive", "Steve" );
	}

	private static void assertBook(Book book, String title, String author) {
		assertThat( book.title() ).isEqualTo( title );
		assertThat( Hibernate.isInitialized( book.author() ) ).isTrue();
		assertThat( book.author().name ).isEqualTo( author );
	}

	private interface Book {
		String title();

		Author author();
	}

	@Entity(name = "ToOneSubselectFetchAuthor")
	@Table(name = "ToOneSubselectFetchAuthor")
	static class Author {
		@Id
		private Long id;

		private String name;

		Author() {
		}

		Author(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "ToOneSubselectFetchBook")
	@Table(name = "ToOneSubselectFetchBook")
	static class FetchBook implements Book {
		@Id
		private Long id;

		private String title;

		@ManyToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SUBSELECT)
		private Author author;

		FetchBook() {
		}

		FetchBook(Long id, String title, Author author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}

		@Override
		public String title() {
			return title;
		}

		@Override
		public Author author() {
			return author;
		}
	}

	@Entity(name = "ToOneSubselectFetchOverrideBook")
	@Table(name = "ToOneSubselectFetchOverrideBook")
	@FetchProfile(
			name = FETCH_OVERRIDE_PROFILE,
			fetchOverrides = @FetchProfile.FetchOverride(
					entity = FetchOverrideBook.class,
					association = "author",
					mode = FetchMode.SUBSELECT,
					fetch = FetchType.EAGER
			)
	)
	static class FetchOverrideBook implements Book {
		@Id
		private Long id;

		private String title;

		@ManyToOne(fetch = FetchType.EAGER)
		private Author author;

		FetchOverrideBook() {
		}

		FetchOverrideBook(Long id, String title, Author author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}

		@Override
		public String title() {
			return title;
		}

		@Override
		public Author author() {
			return author;
		}
	}

	@Entity(name = "ToOneSubselectFetchProfileOverrideBook")
	@Table(name = "ToOneSubselectFetchProfileOverrideBook")
	@FetchProfile(name = FETCH_PROFILE_OVERRIDE_PROFILE)
	static class FetchProfileOverrideBook implements Book {
		@Id
		private Long id;

		private String title;

		@ManyToOne(fetch = FetchType.EAGER)
		@FetchProfileOverride(
				profile = FETCH_PROFILE_OVERRIDE_PROFILE,
				mode = FetchMode.SUBSELECT,
				fetch = FetchType.EAGER
		)
		private Author author;

		FetchProfileOverrideBook() {
		}

		FetchProfileOverrideBook(Long id, String title, Author author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}

		@Override
		public String title() {
			return title;
		}

		@Override
		public Author author() {
			return author;
		}
	}
}
