/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static jakarta.persistence.CascadeType.ALL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		NestedAssociationEmbeddableTest.Location.class,
		NestedAssociationEmbeddableTest.Poi.class,
		NestedAssociationEmbeddableTest.Report.class,
		NestedAssociationEmbeddableTest.ReportTripId.class,
		NestedAssociationEmbeddableTest.ReportTrip.class,
		NestedAssociationEmbeddableTest.Trip.class,
} )
public class NestedAssociationEmbeddableTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Poi poi = new Poi( 1L, "poi_1" );
			session.persist( poi );
			final Trip trip = new Trip( 2L, new Location( 1, poi ), new Location( 2, null ) );
			session.persist( trip );
			final Report report = new Report( 3L );
			report.getReportTripList().add( new ReportTrip( new ReportTripId( report, trip ), "other" ) );
			session.persist( report );
		} );
	}

	@Test
	public void testFindTrip(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Trip trip = session.find( Trip.class, 2L );
			assertThat( trip.getPosition1() ).isNotNull();
			assertThat( trip.getPosition1().getDistance() ).isEqualTo( 1 );
			assertThat( trip.getPosition1().getPoi().getName() ).isEqualTo( "poi_1" );
			assertThat( trip.getPosition2() ).isNotNull();
			assertThat( trip.getPosition2().getDistance() ).isEqualTo( 2 );
			assertThat( trip.getPosition2().getPoi() ).isNull();
		} );
	}

	@Test
	public void testFindReport(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Report report = session.find( Report.class, 3L );
			final ReportTrip reportTrip = report.getReportTripList().get( 0 );
			assertThat( reportTrip.getCompositeKey().getTrip().getPosition1() ).isNotNull();
			assertThat( reportTrip.getCompositeKey().getTrip().getPosition1().getDistance() ).isEqualTo( 1 );
			assertThat( reportTrip.getCompositeKey().getTrip().getPosition1().getPoi().getName() ).isEqualTo( "poi_1" );
			assertThat( reportTrip.getCompositeKey().getTrip().getPosition2() ).isNotNull();
			assertThat( reportTrip.getCompositeKey().getTrip().getPosition2().getDistance() ).isEqualTo( 2 );
			assertThat( reportTrip.getCompositeKey().getTrip().getPosition2().getPoi() ).isNull();
		} );
	}

	@Embeddable
	public static class Location {
		private Integer distance;
		@ManyToOne
		private Poi poi;

		public Location() {
		}

		public Location(Integer distance, Poi poi) {
			this.distance = distance;
			this.poi = poi;
		}

		public Integer getDistance() {
			return distance;
		}

		public Poi getPoi() {
			return poi;
		}
	}

	@Entity( name = "Poi" )
	public static class Poi {
		@Id
		private Long id;
		private String name;

		public Poi() {
		}

		public Poi(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "Report" )
	public static class Report {
		@Id
		private Long id;

		@OneToMany( mappedBy = "compositeKey.report", cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER )
		private List<ReportTrip> reportTripList = new ArrayList<>();

		public Report() {
		}

		public Report(Long id) {
			this.id = id;
		}

		public List<ReportTrip> getReportTripList() {
			return reportTripList;
		}
	}

	@Embeddable
	public static class ReportTripId implements Serializable {
		@ManyToOne
		@JoinColumn( name = "report_id_fk" )
		private Report report;

		@ManyToOne
		@JoinColumn( name = "trip_id_fk" )
		private Trip trip;

		public ReportTripId() {
		}

		public ReportTripId(Report report, Trip trip) {
			this.report = report;
			this.trip = trip;
		}

		public Trip getTrip() {
			return trip;
		}
	}


	@Entity( name = "ReportTrip" )
	public static class ReportTrip {
		@EmbeddedId
		private ReportTripId compositeKey = new ReportTripId();

		private String other;

		public ReportTrip() {
		}

		public ReportTrip(ReportTripId compositeKey, String other) {
			this.compositeKey = compositeKey;
			this.other = other;
		}

		public ReportTripId getCompositeKey() {
			return compositeKey;
		}
	}

	@Entity( name = "Trip" )
	public static class Trip {
		@Id
		private Long id;

		@Embedded
		@AttributeOverrides( {
				@AttributeOverride( name = "distance", column = @Column( name = "distance_1" ) ),
		} )
		@AssociationOverride( name = "poi", joinColumns = @JoinColumn( name = "poi_id_1" ) )
		private Location position1 = new Location();

		@Embedded
		@AttributeOverrides( {
				@AttributeOverride( name = "distance", column = @Column( name = "distance_2" ) ),
		} )
		@AssociationOverride( name = "poi", joinColumns = @JoinColumn( name = "poi_id_2" ) )
		private Location position2 = new Location();

		public Trip() {
		}

		public Trip(Long id, Location position1, Location position2) {
			this.id = id;
			this.position1 = position1;
			this.position2 = position2;
		}

		public Location getPosition1() {
			return position1;
		}

		public Location getPosition2() {
			return position2;
		}
	}
}
