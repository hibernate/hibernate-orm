/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.cfg.QuerySettings;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pagination over fetch joins of collection shapes other than the basic
 * {@code @OneToMany} on entities: {@link ElementCollection @ElementCollection}
 * of basic types, {@link ElementCollection @ElementCollection} of embeddables,
 * {@link MapKey @MapKey}-keyed maps, and sibling plural fetches on the same
 * root.
 */
@DomainModel(annotatedClasses = {
		CollectionFetchPaginationCollectionShapesTest.Document.class,
		CollectionFetchPaginationCollectionShapesTest.Library.class,
		CollectionFetchPaginationCollectionShapesTest.LibBook.class,
		CollectionFetchPaginationCollectionShapesTest.Tournament.class,
		CollectionFetchPaginationCollectionShapesTest.Match.class,
		CollectionFetchPaginationCollectionShapesTest.Player.class
})
@ServiceRegistry(settings = @Setting(
		name = QuerySettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH,
		value = "true"
))
@SessionFactory(useCollectingStatementInspector = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOffsetInSubquery.class)
public class CollectionFetchPaginationCollectionShapesTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			for ( int i = 0; i < 4; i++ ) {
				final Document d = new Document( (long) i, "Doc " + i );
				d.getTags().add( "tag-" + i + "-a" );
				d.getTags().add( "tag-" + i + "-b" );
				d.getTags().add( "tag-" + i + "-c" );
				d.getComments().add( new Comment( "Author " + i + "/0", "Body " + i + "/0" ) );
				d.getComments().add( new Comment( "Author " + i + "/1", "Body " + i + "/1" ) );
				s.persist( d );
			}
			for ( int i = 0; i < 4; i++ ) {
				final Library lib = new Library( (long) i, "Library " + i );
				for ( int j = 0; j < 3; j++ ) {
					final LibBook book = new LibBook(
							i * 10L + j,
							"category-" + j,
							"LibBook " + i + "/" + j
					);
					lib.addBook( book );
				}
				s.persist( lib );
			}
			for ( int i = 0; i < 4; i++ ) {
				final Tournament t = new Tournament( (long) i, "Tournament " + i );
				for ( int j = 0; j < 2; j++ ) {
					t.addMatch( new Match( i * 10L + j, "Match " + i + "/" + j ) );
				}
				for ( int j = 0; j < 3; j++ ) {
					t.addPlayer( new Player( i * 10L + j, "Player " + i + "/" + j ) );
				}
				s.persist( t );
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	/**
	 * {@code @ElementCollection} of a basic type. The collection lives in its own
	 * join-table (no entity persister) and the join group's primary table is the
	 * collection table itself; the rewrite has to treat it like a plural fetch.
	 */
	@Test
	void fetchJoinWithElementCollectionOfBasic(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			final List<Document> docs = s.createSelectionQuery(
					"from Document d left join fetch d.tags order by d.id",
					Document.class
			).setMaxResults( 2 ).list();

			assertEquals( 2, docs.size() );
			assertEquals( 0L, docs.get( 0 ).getId() );
			assertEquals( 1L, docs.get( 1 ).getId() );
			for ( Document d : docs ) {
				assertEquals( 3, d.getTags().size() );
			}

			final String generated = sql.getSqlQueries().get( 0 ).toLowerCase();
			assertTrue( generated.contains( "from (select" ) );
			final int derivedClose = generated.indexOf( ')' );
			final String outer = generated.substring( derivedClose );
			assertTrue( outer.contains( "tags" ) );
		} );
	}

	/**
	 * {@code @ElementCollection} of an {@link Embeddable @Embeddable}. The join
	 * table has multiple value columns (one per embeddable field) — same shape
	 * as an entity-table from the join's POV, so the rewrite path is the same.
	 */
	@Test
	void fetchJoinWithElementCollectionOfEmbeddable(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			final List<Document> docs = s.createSelectionQuery(
					"from Document d left join fetch d.comments order by d.id",
					Document.class
			).setMaxResults( 2 ).list();

			assertEquals( 2, docs.size() );
			assertEquals( 0L, docs.get( 0 ).getId() );
			assertEquals( 1L, docs.get( 1 ).getId() );
			for ( Document d : docs ) {
				assertEquals( 2, d.getComments().size() );
			}

			final String generated = sql.getSqlQueries().get( 0 ).toLowerCase();
			assertTrue( generated.contains( "from (select" ) );
		} );
	}

	/**
	 * {@code @OneToMany} typed as a {@link Map} keyed by an attribute on the
	 * value entity. Hibernate joins the index column alongside the value
	 * columns; the rewrite must move the whole map join (key + value) to the
	 * outer.
	 */
	@Test
	void fetchJoinWithMapByKey(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			final List<Library> libs = s.createSelectionQuery(
					"from Library l left join fetch l.booksByCategory order by l.id",
					Library.class
			).setMaxResults( 2 ).list();

			assertEquals( 2, libs.size() );
			assertEquals( 0L, libs.get( 0 ).getId() );
			assertEquals( 1L, libs.get( 1 ).getId() );
			for ( Library lib : libs ) {
				assertEquals( 3, lib.getBooksByCategory().size() );
				assertNotNull( lib.getBooksByCategory().get( "category-0" ) );
				assertNotNull( lib.getBooksByCategory().get( "category-1" ) );
				assertNotNull( lib.getBooksByCategory().get( "category-2" ) );
			}

			final String generated = sql.getSqlQueries().get( 0 ).toLowerCase();
			assertTrue( generated.contains( "from (select" ) );
		} );
	}

	/**
	 * Two sibling plural fetches on the same root (one with 2 children, one with
	 * 3). With both moved to the outer the cartesian over the inner-paginated
	 * roots is {@code N × M} per parent; the rewrite still produces the right
	 * number of distinct parents and full collections on each.
	 */
	@Test
	void fetchJoinWithSiblingPluralFetches(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			final List<Tournament> tournaments = s.createSelectionQuery(
					"from Tournament t left join fetch t.matches left join fetch t.players "
							+ "order by t.id",
					Tournament.class
			).setMaxResults( 2 ).list();

			assertEquals( 2, tournaments.size() );
			assertEquals( 0L, tournaments.get( 0 ).getId() );
			assertEquals( 1L, tournaments.get( 1 ).getId() );
			for ( Tournament t : tournaments ) {
				assertEquals( 2, t.getMatches().size() );
				assertEquals( 3, t.getPlayers().size() );
			}

			final String generated = sql.getSqlQueries().get( 0 ).toLowerCase();
			assertTrue( generated.contains( "from (select" ) );
			final int derivedClose = generated.indexOf( ')' );
			final String outer = generated.substring( derivedClose );
			// both fetched collections joined on the outer
			assertTrue( outer.contains( "match" ) );
			assertTrue( outer.contains( "player" ) );
		} );
	}

	@Entity(name = "Document")
	public static class Document {
		@Id
		private Long id;
		private String title;

		@ElementCollection
		@CollectionTable(name = "Document_tags", joinColumns = @JoinColumn(name = "doc_id"))
		@Column(name = "tag")
		private Set<String> tags = new HashSet<>();

		@ElementCollection
		@CollectionTable(name = "Document_comments", joinColumns = @JoinColumn(name = "doc_id"))
		private List<Comment> comments = new ArrayList<>();

		public Document() {
		}

		public Document(Long id, String title) {
			this.id = id;
			this.title = title;
		}

		public Long getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public Set<String> getTags() {
			return tags;
		}

		public List<Comment> getComments() {
			return comments;
		}
	}

	@Embeddable
	public static class Comment {
		private String author;
		private String body;

		public Comment() {
		}

		public Comment(String author, String body) {
			this.author = author;
			this.body = body;
		}

		public String getAuthor() {
			return author;
		}

		public String getBody() {
			return body;
		}
	}

	// "Library" is a reserved word in recent MySQL; override the table name.
	@Entity(name = "Library")
	@Table(name = "library_entity")
	public static class Library {
		@Id
		private Long id;
		private String name;

		@OneToMany(mappedBy = "library", cascade = CascadeType.ALL)
		@MapKey(name = "category")
		private Map<String, LibBook> booksByCategory = new HashMap<>();

		public Library() {
		}

		public Library(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Map<String, LibBook> getBooksByCategory() {
			return booksByCategory;
		}

		public void addBook(LibBook book) {
			booksByCategory.put( book.getCategory(), book );
			book.setLibrary( this );
		}
	}

	@Entity(name = "LibBook")
	public static class LibBook {
		@Id
		private Long id;
		private String category;
		private String title;
		@ManyToOne
		@JoinColumn(name = "library_id")
		private Library library;

		public LibBook() {
		}

		public LibBook(Long id, String category, String title) {
			this.id = id;
			this.category = category;
			this.title = title;
		}

		public Long getId() {
			return id;
		}

		public String getCategory() {
			return category;
		}

		public String getTitle() {
			return title;
		}

		public Library getLibrary() {
			return library;
		}

		public void setLibrary(Library l) {
			this.library = l;
		}
	}

	@Entity(name = "Tournament")
	public static class Tournament {
		@Id
		private Long id;
		private String name;
		@OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
		private Set<Match> matches = new HashSet<>();
		@OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
		private Set<Player> players = new HashSet<>();

		public Tournament() {
		}

		public Tournament(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Set<Match> getMatches() {
			return matches;
		}

		public Set<Player> getPlayers() {
			return players;
		}

		public void addMatch(Match m) {
			matches.add( m );
			m.setTournament( this );
		}

		public void addPlayer(Player p) {
			players.add( p );
			p.setTournament( this );
		}
	}

	// "Match" is reserved in MySQL (MATCH...AGAINST full-text search); override the table name.
	@Entity(name = "Match")
	@Table(name = "match_entity")
	public static class Match {
		@Id
		private Long id;
		private String name;
		@ManyToOne
		@JoinColumn(name = "tournament_id")
		private Tournament tournament;

		public Match() {
		}

		public Match(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Tournament getTournament() {
			return tournament;
		}

		public void setTournament(Tournament t) {
			this.tournament = t;
		}
	}

	@Entity(name = "Player")
	public static class Player {
		@Id
		private Long id;
		private String name;
		@ManyToOne
		@JoinColumn(name = "tournament_id")
		private Tournament tournament;

		public Player() {
		}

		public Player(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Tournament getTournament() {
			return tournament;
		}

		public void setTournament(Tournament t) {
			this.tournament = t;
		}
	}
}
