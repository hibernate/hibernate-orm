/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Imported;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		MultipleJoinFetchesDifferentAliasTest.Link.class,
		MultipleJoinFetchesDifferentAliasTest.Point.class,
		MultipleJoinFetchesDifferentAliasTest.StopPoint.class,
		MultipleJoinFetchesDifferentAliasTest.TimingPoint.class,
		MultipleJoinFetchesDifferentAliasTest.PointWrapper.class,
		MultipleJoinFetchesDifferentAliasTest.StartAndEndModel.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18086" )
public class MultipleJoinFetchesDifferentAliasTest {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final StartAndEndModel result = session.createQuery(
					"select new StartAndEndModel(start, end) " +
							"from Link l " +
							"join PointWrapper startW ON startW.link = l " +
							"join PointWrapper endW ON endW.link = l " +
							"join startW.point start " +
							"join endW.point end " +
							"join fetch start.stopPoint startStop " +
							"join fetch start.timingPoint startTiming " +
							"join fetch end.stopPoint endStop " +
							"join fetch end.timingPoint endTiming",
					StartAndEndModel.class
			).getSingleResult();
			final Point start = result.getStart();
			assertThat( start ).isSameAs( result.getEnd() );
			assertThat( start.stopPoint ).matches( Hibernate::isInitialized )
					.extracting( "id" )
					.isEqualTo( 3L );
			assertThat( start.timingPoint ).matches( Hibernate::isInitialized )
					.extracting( TimingPoint::getId )
					.isEqualTo( 4L );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Link link = new Link();
			link.id = 1L;
			session.persist( link );
			final Point point = new Point();
			point.id = 2L;
			session.persist( point );
			final StopPoint stopPoint = new StopPoint();
			stopPoint.id = 3L;
			stopPoint.point = point;
			session.persist( stopPoint );
			final TimingPoint timingPoint = new TimingPoint();
			timingPoint.id = 4L;
			timingPoint.point = point;
			session.persist( timingPoint );
			final PointWrapper pointWrapper = new PointWrapper();
			pointWrapper.id = 5L;
			pointWrapper.point = point;
			pointWrapper.link = link;
			session.persist( pointWrapper );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from PointWrapper" ).executeUpdate();
			session.createMutationQuery( "delete from StopPoint" ).executeUpdate();
			session.createMutationQuery( "delete from TimingPoint" ).executeUpdate();
			session.createMutationQuery( "delete from Point" ).executeUpdate();
			session.createMutationQuery( "delete from Link" ).executeUpdate();
		} );
	}

	@Entity( name = "Link" )
	static class Link {
		@Id
		private Long id;

		public void setId(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity( name = "Point" )
	static class Point {
		@Id
		private Long id;

		@OneToOne( mappedBy = "point", fetch = FetchType.LAZY )
		private StopPoint stopPoint;

		@OneToOne( mappedBy = "point", fetch = FetchType.LAZY )
		private TimingPoint timingPoint;

		public void setId(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity( name = "StopPoint" )
	static class StopPoint {
		@Id
		private Long id;

		@OneToOne
		private Point point;

		public void setId(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity( name = "TimingPoint" )
	static class TimingPoint {
		@Id
		private Long id;

		@OneToOne
		private Point point;

		public void setId(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity( name = "PointWrapper" )
	static class PointWrapper {
		@Id
		private Long id;

		@ManyToOne
		private Point point;

		@ManyToOne
		private Link link;

		public void setId(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
	}

	@Imported
	static class StartAndEndModel {
		private Point start;
		private Point end;

		public StartAndEndModel(Point start, Point end) {
			this.start = start;
			this.end = end;
		}

		public Point getStart() {
			return start;
		}

		public Point getEnd() {
			return end;
		}
	}
}
