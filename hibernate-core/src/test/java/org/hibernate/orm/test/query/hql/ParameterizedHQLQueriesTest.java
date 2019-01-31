/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class ParameterizedHQLQueriesTest extends SessionFactoryBasedFunctionalTest {

	final VideoGame GOD_OF_WAR = new VideoGame( "GOW_2018", "God of war", LocalDate.of( 2018, Month.APRIL, 20 ) );
	final VideoGame THE_LAST_OF_US = new VideoGame( "TLOU_2013", "The last of us", LocalDate.of( 2013, Month.JUNE, 14 ) );

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				VideoGame.class,
		};
	}

	@Test
	public void testQueryWithoutParameters() {
		inTransaction(
				session -> {
					List<VideoGame> results = session.createQuery( "select vg from VideoGame vg" )
							.list();
					assertThat( results, containsInAnyOrder( GOD_OF_WAR, THE_LAST_OF_US ) );
				} );
	}

	@Test
	public void testQueryWithSingleParameters() {
		inTransaction(
				session -> {
					List<VideoGame> results = session.createQuery( "select vg from VideoGame vg where vg.title=:title" )
							.setParameter( "title",  GOD_OF_WAR.getTitle())
							.list();
					assertThat( results, contains( GOD_OF_WAR ) );
				} );
	}

	@BeforeEach
	public void setUp() {
		inTransaction(
				session -> {
					session.save( GOD_OF_WAR );
					session.save( THE_LAST_OF_US );
				} );
	}

	@AfterEach
	public void tearDown() {
		inTransaction(
				session -> {
					session.createQuery( "from VideoGame vg" )
							.list()
							.forEach( vg -> session.delete( vg ) );
				} );
	}

	@Entity(name = "VideoGame")
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
