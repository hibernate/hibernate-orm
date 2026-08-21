/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
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
 * Pagination + collection-fetch edge cases:
 * <ul>
 *   <li>Chained / multi-level fetches ({@code a.b.c}).</li>
 *   <li>{@link OrderColumn @OrderColumn}-driven implicit ORDER BY on the
 *       fetched collection.</li>
 *   <li>{@link SQLRestriction @SQLRestriction} adding a predicate to the join's
 *       ON clause.</li>
 * </ul>
 */
@DomainModel(annotatedClasses = {
		CollectionFetchPaginationEdgeCasesTest.Show.class,
		CollectionFetchPaginationEdgeCasesTest.Episode.class,
		CollectionFetchPaginationEdgeCasesTest.Scene.class,
		CollectionFetchPaginationEdgeCasesTest.Playlist.class,
		CollectionFetchPaginationEdgeCasesTest.Song.class,
		CollectionFetchPaginationEdgeCasesTest.Article.class,
		CollectionFetchPaginationEdgeCasesTest.ArticleComment.class
})
@ServiceRegistry(settings = @Setting(
		name = QuerySettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH,
		value = "true"
))
@SessionFactory(useCollectingStatementInspector = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOffsetInSubquery.class)
public class CollectionFetchPaginationEdgeCasesTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			for ( int i = 0; i < 4; i++ ) {
				final Show show = new Show( (long) i, "Show " + i );
				for ( int j = 0; j < 2; j++ ) {
					final Episode ep = new Episode( i * 10L + j, "Ep " + i + "/" + j );
					for ( int k = 0; k < 2; k++ ) {
						ep.addScene( new Scene( i * 100L + j * 10 + k, "Scene " + i + "/" + j + "/" + k ) );
					}
					show.addEpisode( ep );
				}
				s.persist( show );
			}
			for ( int i = 0; i < 4; i++ ) {
				final Playlist pl = new Playlist( (long) i, "Playlist " + i );
				// add songs in non-alphabetical order to verify @OrderColumn preserves it
				pl.addSong( new Song( i * 10L + 0, "Charlie " + i ) );
				pl.addSong( new Song( i * 10L + 1, "Alpha " + i ) );
				pl.addSong( new Song( i * 10L + 2, "Bravo " + i ) );
				s.persist( pl );
			}
			for ( int i = 0; i < 4; i++ ) {
				final Article art = new Article( (long) i, "Article " + i );
				// approved comments are kept by the @SQLRestriction filter,
				// rejected ones are not
				art.addComment( new ArticleComment( i * 10L + 0, "approved-A " + i, true ) );
				art.addComment( new ArticleComment( i * 10L + 1, "REJECTED " + i, false ) );
				art.addComment( new ArticleComment( i * 10L + 2, "approved-B " + i, true ) );
				s.persist( art );
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	/**
	 * Multi-level fetch: {@code show.episodes.scenes}. The plural fetch on
	 * {@code scenes} hangs off the {@code episodes} group, not the primary root,
	 * so the rewrite has to handle a fetched plural join nested under an
	 * already-moved fetched join.
	 */
	@Test
	void chainedPluralFetches(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			final List<Show> shows = s.createSelectionQuery(
					"from Show s left join fetch s.episodes e left join fetch e.scenes "
							+ "order by s.id",
					Show.class
			).setMaxResults( 2 ).list();

			assertEquals( 2, shows.size() );
			assertEquals( 0L, shows.get( 0 ).getId() );
			assertEquals( 1L, shows.get( 1 ).getId() );
			for ( Show show : shows ) {
				assertEquals( 2, show.getEpisodes().size() );
				for ( Episode ep : show.getEpisodes() ) {
					assertEquals( 2, ep.getScenes().size() );
				}
			}

			final String generated = sql.getSqlQueries().get( 0 ).toLowerCase();
			assertTrue( generated.contains( "from (select" ) );
		} );
	}

	/**
	 * {@link OrderColumn @OrderColumn} on the fetched list. Hibernate adds an
	 * implicit ORDER BY on the index column (and sometimes on the FK) so the
	 * collection comes back in declared order. After the fetch join moves to the
	 * outer that synthetic ORDER BY references the moved alias and must move
	 * with it; the inner keeps only the user's order-by.
	 */
	@Test
	void orderColumnOnFetchedList(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			final List<Playlist> playlists = s.createSelectionQuery(
					"from Playlist p left join fetch p.songs order by p.id",
					Playlist.class
			).setMaxResults( 2 ).list();

			assertEquals( 2, playlists.size() );
			for ( Playlist pl : playlists ) {
				final List<Song> songs = pl.getSongs();
				assertEquals( 3, songs.size() );
				// order must be the insertion order Charlie / Alpha / Bravo,
				// preserved by the @OrderColumn-driven sort
				assertTrue( songs.get( 0 ).getTitle().contains( "Charlie" ) );
				assertTrue( songs.get( 1 ).getTitle().contains( "Alpha" ) );
				assertTrue( songs.get( 2 ).getTitle().contains( "Bravo" ) );
			}

			final String generated = sql.getSqlQueries().get( 0 ).toLowerCase();
			assertTrue( generated.contains( "from (select" ) );
		} );
	}

	/**
	 * The plural fetch hangs off a fetched <em>singular</em> association, not off
	 * the root: {@code from Episode e join fetch e.show sh join fetch sh.episodes}.
	 * The rewrite must walk down through fetched singulars to find the nested
	 * plural and move it to the outer, while pulling the singular's table into
	 * the inner derived table (its FK from the root, plus the join row, must be
	 * reachable from the outer so the plural can join through it).
	 */
	@Test
	void nestedPluralUnderSingularFetchPaginates(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			// Episode ids in order: 0, 1, 10, 11, 20, 21, 30, 31.
			// First page (max=3): 0, 1, 10. Episodes 0 and 1 share Show 0,
			// episode 10 sits under Show 1.
			final List<Episode> episodes = s.createSelectionQuery(
					"from Episode e left join fetch e.show sh left join fetch sh.episodes "
							+ "order by e.id",
					Episode.class
			).setMaxResults( 3 ).list();

			assertEquals( 3, episodes.size() );
			assertEquals( 0L, episodes.get( 0 ).getId() );
			assertEquals( 1L, episodes.get( 1 ).getId() );
			assertEquals( 10L, episodes.get( 2 ).getId() );

			for ( Episode ep : episodes ) {
				assertNotNull( ep.getShow() );
				// each fetched Show has its full collection of 2 episodes
				assertEquals( 2, ep.getShow().getEpisodes().size() );
			}

			// pagination must be in the SQL, not in memory
			final String generated = sql.getSqlQueries().get( 0 ).toLowerCase();
			assertTrue( generated.contains( "from (select" ) );
		} );
	}

	/**
	 * {@link SQLRestriction @SQLRestriction} on the fetched collection adds a
	 * predicate to the join's ON clause. After the join moves to the outer the
	 * predicate moves with it, and any column references inside the predicate
	 * that point at absorbed aliases get rewritten through the derived alias.
	 */
	@Test
	void sqlRestrictionOnFetchedCollection(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			final List<Article> articles = s.createSelectionQuery(
					"from Article a left join fetch a.comments order by a.id",
					Article.class
			).setMaxResults( 2 ).list();

			assertEquals( 2, articles.size() );
			assertEquals( 0L, articles.get( 0 ).getId() );
			assertEquals( 1L, articles.get( 1 ).getId() );
			for ( Article art : articles ) {
				// REJECTED comments are filtered by @SQLRestriction("approved=true")
				assertEquals( 2, art.getComments().size() );
				for ( ArticleComment c : art.getComments() ) {
					assertTrue( c.isApproved() );
				}
			}

			final String generated = sql.getSqlQueries().get( 0 ).toLowerCase();
			assertTrue( generated.contains( "from (select" ) );
			final int derivedClose = generated.indexOf( ')' );
			final String outer = generated.substring( derivedClose );
			// the @SQLRestriction predicate moves with the join to the outer
			assertTrue( outer.contains( "approved" ) );
		} );
	}

	// "Show" is reserved in MySQL (SHOW DATABASES, etc.); override the table name.
	@Entity(name = "Show")
	@Table(name = "show_entity")
	public static class Show {
		@Id
		private Long id;
		private String name;
		@OneToMany(mappedBy = "show", cascade = CascadeType.ALL)
		private Set<Episode> episodes = new HashSet<>();

		public Show() {
		}

		public Show(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Set<Episode> getEpisodes() {
			return episodes;
		}

		public void addEpisode(Episode e) {
			episodes.add( e );
			e.setShow( this );
		}
	}

	@Entity(name = "Episode")
	public static class Episode {
		@Id
		private Long id;
		private String title;
		@ManyToOne
		@JoinColumn(name = "show_id")
		private Show show;
		@OneToMany(mappedBy = "episode", cascade = CascadeType.ALL)
		private Set<Scene> scenes = new HashSet<>();

		public Episode() {
		}

		public Episode(Long id, String title) {
			this.id = id;
			this.title = title;
		}

		public Long getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public Show getShow() {
			return show;
		}

		public void setShow(Show s) {
			this.show = s;
		}

		public Set<Scene> getScenes() {
			return scenes;
		}

		public void addScene(Scene s) {
			scenes.add( s );
			s.setEpisode( this );
		}
	}

	@Entity(name = "Scene")
	public static class Scene {
		@Id
		private Long id;
		private String title;
		@ManyToOne
		@JoinColumn(name = "episode_id")
		private Episode episode;

		public Scene() {
		}

		public Scene(Long id, String title) {
			this.id = id;
			this.title = title;
		}

		public Long getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public Episode getEpisode() {
			return episode;
		}

		public void setEpisode(Episode e) {
			this.episode = e;
		}
	}

	@Entity(name = "Playlist")
	public static class Playlist {
		@Id
		private Long id;
		private String name;
		@OneToMany(cascade = CascadeType.ALL)
		@JoinColumn(name = "playlist_id")
		@OrderColumn(name = "position")
		private List<Song> songs = new ArrayList<>();

		public Playlist() {
		}

		public Playlist(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<Song> getSongs() {
			return songs;
		}

		public void addSong(Song song) {
			songs.add( song );
		}
	}

	@Entity(name = "Song")
	public static class Song {
		@Id
		private Long id;
		private String title;

		public Song() {
		}

		public Song(Long id, String title) {
			this.id = id;
			this.title = title;
		}

		public Long getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}
	}

	@Entity(name = "Article")
	public static class Article {
		@Id
		private Long id;
		private String title;
		@OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
		@SQLRestriction("approved = true")
		private List<ArticleComment> comments = new ArrayList<>();

		public Article() {
		}

		public Article(Long id, String title) {
			this.id = id;
			this.title = title;
		}

		public Long getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public List<ArticleComment> getComments() {
			return comments;
		}

		public void addComment(ArticleComment c) {
			comments.add( c );
			c.setArticle( this );
		}
	}

	@Entity(name = "ArticleComment")
	public static class ArticleComment {
		@Id
		private Long id;
		private String body;
		private boolean approved;
		@ManyToOne
		@JoinColumn(name = "article_id")
		private Article article;

		public ArticleComment() {
		}

		public ArticleComment(Long id, String body, boolean approved) {
			this.id = id;
			this.body = body;
			this.approved = approved;
		}

		public Long getId() {
			return id;
		}

		public String getBody() {
			return body;
		}

		public boolean isApproved() {
			return approved;
		}

		public Article getArticle() {
			return article;
		}

		public void setArticle(Article a) {
			this.article = a;
		}
	}
}
