package org.hibernate.orm.test.entitygraph;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import org.hibernate.FetchMethod;
import org.hibernate.Hibernate;
import org.hibernate.orm.test.entitygraph.EntityGraphNestedFetchTest_.Author_;
import org.hibernate.orm.test.entitygraph.EntityGraphNestedFetchTest_.Book_;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				EntityGraphNestedFetchTest.Author.class,
				EntityGraphNestedFetchTest.Book.class,
				EntityGraphNestedFetchTest.Review.class,
				EntityGraphNestedFetchTest.Note.class,
				EntityGraphNestedFetchTest.Publisher.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNestedCollectionFetch.class)
@JiraKey("HHH-9558")
class EntityGraphNestedFetchTest {

	private static final LocalDateTime CREATED = LocalDateTime.of( 2024, 5, 17, 10, 15, 30, 123_456_000 );

	@BeforeEach
	void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var publisher = new Publisher( 1L, "Manning" );
			session.persist( publisher );

			final var gavin = new Author( 1L, "Gavin" );
			gavin.tags.add( "orm" );
			gavin.tags.add( "it's \"quoted\" \\ and ünicode" );
			gavin.addresses.add( new Address( "Atlanta", "Peachtree St" ) );
			gavin.addresses.add( new Address( "Melbourne", null ) );
			gavin.attributes.put( "language", "Java" );
			session.persist( gavin );
			session.persist( new Book( 1L, "Java Persistence with Hibernate", new BigDecimal( "49.90" ),
					LocalDate.of( 2015, 11, 1 ), CREATED, true, Genre.TECHNICAL, Format.HARDCOVER, gavin, publisher ) );
			session.persist( new Book( 2L, "Hibernate in Action", new BigDecimal( "39.00" ),
					LocalDate.of( 2004, 8, 1 ), null, false, Genre.TECHNICAL, Format.PAPERBACK, gavin, null ) );
			session.persist( new Review( 1L, 5, "Excellent", gavin ) );
			session.persist( new Review( 2L, 3, null, gavin ) );
			final var second = new Note( 2L, "second" );
			final var first = new Note( 1L, "first" );
			session.persist( second );
			session.persist( first );
			gavin.notes.add( first );
			gavin.notes.add( second );

			final var steve = new Author( 2L, "Steve" );
			session.persist( steve );
			session.persist( new Book( 3L, "Hibernate Reactive", null, null, null, true, null, null, steve, null ) );

			// no books, no reviews
			session.persist( new Author( 3L, "Emmanuel" ) );
		} );
		scope.getCollectingStatementInspector().clear();
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void findWithNested(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			//tag::fetching-graph-method-nested[]
			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.books )
					.addOption( FetchMethod.NESTED );
			graph.addAttributeNode( Author_.reviews )
					.addOption( FetchMethod.NESTED );
			// a single SQL select, with one row for the author
			final var author = session.find( graph, 1L );
			//end::fetching-graph-method-nested[]

			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfJoins( 0, 0 );

			assertThat( Hibernate.isInitialized( author.books ) ).isTrue();
			assertThat( Hibernate.isInitialized( author.reviews ) ).isTrue();
			// @OrderBy("title") is applied inside the aggregate
			assertThat( author.books ).extracting( book -> book.title )
					.containsExactly( "Hibernate in Action", "Java Persistence with Hibernate" );
			assertThat( author.reviews ).extracting( review -> review.text )
					.containsExactlyInAnyOrder( "Excellent", null );

			final var book = author.books.get( 1 );
			assertThat( book.price ).isEqualByComparingTo( "49.90" );
			assertThat( book.published ).isEqualTo( LocalDate.of( 2015, 11, 1 ) );
			assertThat( book.created ).isEqualTo( CREATED );
			assertThat( book.available ).isTrue();
			assertThat( book.genre ).isEqualTo( Genre.TECHNICAL );
			assertThat( book.format ).isEqualTo( Format.HARDCOVER );
			assertThat( author.books.get( 0 ).available ).isFalse();
			assertThat( author.books.get( 0 ).created ).isNull();
			assertThat( author.books.get( 0 ).publisher ).isNull();

			// the back reference is the owner, the lazy to-one is a proxy for the FK in the row
			assertThat( book.author ).isSameAs( author );
			assertThat( author.reviews ).allSatisfy( review -> assertThat( review.author ).isSameAs( author ) );
			assertThat( Hibernate.isInitialized( book.publisher ) ).isFalse();
			assertThat( Hibernate.unproxy( book.publisher, Publisher.class ).name ).isEqualTo( "Manning" );
		} );
	}

	@Test
	void queryWithNested(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.books ).addOption( FetchMethod.NESTED );
			graph.addAttributeNode( Author_.reviews ).addOption( FetchMethod.NESTED );
			final var authors =
					session.createQuery( "from NestedAuthor a order by a.id" )
							.withEntityGraph( graph )
							.getResultList();

			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfJoins( 0, 0 );

			assertThat( authors ).hasSize( 3 );
			assertThat( authors.get( 0 ).books ).hasSize( 2 );
			assertThat( authors.get( 0 ).reviews ).hasSize( 2 );
			assertThat( authors.get( 1 ).books ).extracting( book -> book.title ).containsExactly( "Hibernate Reactive" );
			assertThat( authors.get( 1 ).books.get( 0 ).price ).isNull();
			assertThat( authors.get( 1 ).books.get( 0 ).genre ).isNull();
			assertThat( authors.get( 1 ).reviews ).isEmpty();
			assertThat( Hibernate.isInitialized( authors.get( 2 ).books ) ).isTrue();
			assertThat( authors.get( 2 ).books ).isEmpty();
			assertThat( authors.get( 2 ).reviews ).isEmpty();
		} );
	}

	@Test
	void paginatedQueryWithNested(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.books ).addOption( FetchMethod.NESTED );
			// unlike with a join fetch, the limit is applied by the database
			final var authors =
					session.createSelectionQuery( "from NestedAuthor a order by a.id", graph )
							.setFirstResult( 1 )
							.setMaxResults( 1 )
							.getResultList();

			inspector.assertExecutedCount( 1 );
			assertThat( authors ).hasSize( 1 );
			assertThat( authors.get( 0 ).name ).isEqualTo( "Steve" );
			assertThat( authors.get( 0 ).books ).extracting( book -> book.title ).containsExactly( "Hibernate Reactive" );
		} );
	}

	@Test
	void indexedListWithNested(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.notes ).addOption( FetchMethod.NESTED );
			final var author = session.find( graph, 1L );

			inspector.assertExecutedCount( 1 );
			assertThat( author.notes ).extracting( note -> note.text ).containsExactly( "first", "second" );
		} );
	}

	@Test
	void elementCollectionsWithNested(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.tags ).addOption( FetchMethod.NESTED );
			graph.addAttributeNode( Author_.addresses ).addOption( FetchMethod.NESTED );
			final var author = session.find( graph, 1L );

			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfJoins( 0, 0 );
			assertThat( author.tags ).containsExactlyInAnyOrder( "orm", "it's \"quoted\" \\ and ünicode" );
			assertThat( author.addresses ).containsExactly(
					new Address( "Atlanta", "Peachtree St" ),
					new Address( "Melbourne", null )
			);
		} );
	}

	@Test
	void loadedCollectionIsManaged(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.books ).addOption( FetchMethod.NESTED );
			graph.addAttributeNode( Author_.tags ).addOption( FetchMethod.NESTED );
			final var author = session.find( graph, 1L );
			author.books.get( 0 ).title = "Hibernate in Action, 2nd edition";
			author.tags.remove( "orm" );
			author.tags.add( "jpa" );
		} );

		scope.inTransaction( session -> {
			final var author = session.find( Author.class, 1L );
			assertThat( author.books ).extracting( book -> book.title )
					.contains( "Hibernate in Action, 2nd edition" );
			assertThat( author.tags ).contains( "jpa" ).doesNotContain( "orm" );
		} );
	}

	@Test
	void subgraphOfElementUsesSelect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.books ).addOption( FetchMethod.NESTED );
			graph.addElementSubgraph( Author_.books ).addAttributeNode( Book_.publisher );
			final var author = session.find( graph, 1L );

			// the publisher is not joined into the subquery, but fetched by a secondary select
			inspector.assertExecutedCount( 2 );
			inspector.assertNumberOfJoins( 0, 0 );
			final var book = author.books.get( 1 );
			assertThat( Hibernate.isInitialized( book.publisher ) ).isTrue();
			assertThat( book.publisher.name ).isEqualTo( "Manning" );
		} );
	}

	@Test
	void explicitJoinFetchWins(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.books ).addOption( FetchMethod.NESTED );
			final var authors =
					session.createQuery( "from NestedAuthor a join fetch a.books where a.id = 1" )
							.withEntityGraph( graph )
							.getResultList();

			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfJoins( 0, 1 );
			assertThat( authors.get( 0 ).books ).hasSize( 2 );
		} );
	}

	@Test
	void unsupportedCollectionFallsBackToSelect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			// maps are not supported yet
			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.attributes ).addOption( FetchMethod.NESTED );
			final var author = session.find( graph, 1L );

			inspector.assertExecutedCount( 2 );
			assertThat( Hibernate.isInitialized( author.attributes ) ).isTrue();
			assertThat( author.attributes ).containsEntry( "language", "Java" );
		} );
	}

	@Test
	void ownerAlreadyInPersistenceContext(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var loaded = session.find( Author.class, 1L );
			assertThat( Hibernate.isInitialized( loaded.books ) ).isFalse();

			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.books ).addOption( FetchMethod.NESTED );
			final var authors =
					session.createQuery( "from NestedAuthor a order by a.id" )
							.withEntityGraph( graph )
							.getResultList();

			assertThat( authors.get( 0 ) ).isSameAs( loaded );
			assertThat( Hibernate.isInitialized( loaded.books ) ).isTrue();
			assertThat( loaded.books ).hasSize( 2 );
			assertThat( authors.get( 1 ).books ).hasSize( 1 );
		} );
	}

	enum Genre { FICTION, TECHNICAL }

	enum Format { PAPERBACK, HARDCOVER }

	@Entity(name = "NestedAuthor")
	static class Author {
		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "author")
		@OrderBy("title")
		private List<Book> books = new ArrayList<>();

		@OneToMany(mappedBy = "author")
		private Set<Review> reviews = new HashSet<>();

		@OneToMany
		@JoinColumn(name = "author_id")
		@OrderColumn(name = "list_index")
		private List<Note> notes = new ArrayList<>();

		@ElementCollection
		private Set<String> tags = new HashSet<>();

		@ElementCollection
		@OrderColumn(name = "list_index")
		private List<Address> addresses = new ArrayList<>();

		@ElementCollection
		private Map<String, String> attributes = new HashMap<>();

		Author() {
		}

		Author(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "NestedBook")
	static class Book {
		@Id
		private Long id;

		private String title;

		@Column(precision = 10, scale = 2)
		private BigDecimal price;

		private LocalDate published;

		private LocalDateTime created;

		private boolean available;

		@Enumerated(EnumType.STRING)
		private Genre genre;

		@Enumerated(EnumType.ORDINAL)
		private Format format;

		@ManyToOne(fetch = FetchType.LAZY)
		private Author author;

		@ManyToOne(fetch = FetchType.LAZY)
		private Publisher publisher;

		Book() {
		}

		Book(Long id, String title, BigDecimal price, LocalDate published, LocalDateTime created, boolean available,
				Genre genre, Format format, Author author, Publisher publisher) {
			this.id = id;
			this.title = title;
			this.price = price;
			this.published = published;
			this.created = created;
			this.available = available;
			this.genre = genre;
			this.format = format;
			this.author = author;
			this.publisher = publisher;
		}
	}

	@Entity(name = "NestedReview")
	static class Review {
		@Id
		private Long id;

		private int stars;

		private String text;

		@ManyToOne
		private Author author;

		Review() {
		}

		Review(Long id, int stars, String text, Author author) {
			this.id = id;
			this.stars = stars;
			this.text = text;
			this.author = author;
		}
	}

	@Entity(name = "NestedNote")
	static class Note {
		@Id
		private Long id;

		private String text;

		Note() {
		}

		Note(Long id, String text) {
			this.id = id;
			this.text = text;
		}
	}

	@Entity(name = "NestedPublisher")
	static class Publisher {
		@Id
		private Long id;

		private String name;

		Publisher() {
		}

		Publisher(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Embeddable
	record Address(String city, String street) {
	}
}
