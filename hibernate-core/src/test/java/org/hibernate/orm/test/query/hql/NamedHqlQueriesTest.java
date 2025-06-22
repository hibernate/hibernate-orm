/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

@DomainModel( annotatedClasses = NamedHqlQueriesTest.VideoGame.class )
@SessionFactory
public class NamedHqlQueriesTest {

	final VideoGame GOD_OF_WAR = new VideoGame( "GOW_2018", "God of war", LocalDate.of( 2018, Month.APRIL, 20 ) );
	final VideoGame THE_LAST_OF_US = new VideoGame(
			"TLOU_2013", "The last of us", LocalDate.of( 2013, Month.JUNE, 14 ) );

	@Test
	public void testQueryWithoutParameters(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<VideoGame> results = session.createNamedQuery( "videogames", VideoGame.class ).list();
					assertThat( results, containsInAnyOrder( GOD_OF_WAR, THE_LAST_OF_US ) );
				}
		);
	}

	@Test
	public void testQueryWithSingleParameters(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<VideoGame> results = session.createNamedQuery( "title", VideoGame.class )
							.setParameter("title", GOD_OF_WAR.getTitle() )
							.list();
					assertThat( results, contains( GOD_OF_WAR ) );
				}
		);
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( GOD_OF_WAR );
					session.persist( THE_LAST_OF_US );
				} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "VideoGame")
	@NamedQueries({
			@NamedQuery(name = "videogames", query = "select vg from VideoGame vg"),
			@NamedQuery(name = "title", query = "select vg from VideoGame vg where vg.title=:title")
	})
	static class VideoGame {

		public VideoGame() {
		}

		public VideoGame(String id, String title, LocalDate releaseDate) {
			this.id = id;
			this.title = title;
			this.releaseDate = releaseDate;
		}

		@Id
		private String id;
		private String title;
		private LocalDate releaseDate;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public LocalDate getReleaseDate() {
			return releaseDate;
		}

		public void setReleaseDate(LocalDate releaseDate) {
			this.releaseDate = releaseDate;
		}


		@Override
		public String toString() {
			return title;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			VideoGame videoGame = (VideoGame) o;
			return Objects.equals( id, videoGame.id ) &&
					Objects.equals( title, videoGame.title ) &&
					Objects.equals( releaseDate, videoGame.releaseDate );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, title, releaseDate );
		}
	}
}
