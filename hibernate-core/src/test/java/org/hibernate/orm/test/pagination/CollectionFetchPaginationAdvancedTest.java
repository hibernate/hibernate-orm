/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;

import org.hibernate.cfg.QuerySettings;

import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pagination-with-collection-fetch where the root entity uses
 * {@link SecondaryTable @SecondaryTable} or {@link Inheritance(strategy=JOINED)
 * joined inheritance}. Both shapes attach extra {@code TableReferenceJoin}s to
 * the root {@code TableGroup}, so the rewrite has to absorb those tables'
 * columns into the inner derived table — and for joined inheritance the outer
 * SELECT contains a CASE-based discriminator whose internal column references
 * also need rewriting.
 */
@DomainModel(annotatedClasses = {
		CollectionFetchPaginationAdvancedTest.Album.class,
		CollectionFetchPaginationAdvancedTest.Track.class,
		CollectionFetchPaginationAdvancedTest.Vehicle.class,
		CollectionFetchPaginationAdvancedTest.Car.class,
		CollectionFetchPaginationAdvancedTest.Truck.class,
		CollectionFetchPaginationAdvancedTest.Part.class
})
@ServiceRegistry(settings = @Setting(
		name = QuerySettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH,
		value = "true"
))
@SessionFactory(useCollectingStatementInspector = true)
@SkipForDialect( dialectClass = SybaseASEDialect.class )
public class CollectionFetchPaginationAdvancedTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			for ( int i = 0; i < 4; i++ ) {
				final Album a = new Album( (long) i, "Album " + i, "Review " + i );
				for ( int j = 0; j < 3; j++ ) {
					a.addTrack( new Track( i * 10L + j, "Track " + i + "/" + j ) );
				}
				s.persist( a );
			}
			for ( int i = 0; i < 4; i++ ) {
				final Vehicle v;
				if ( i % 2 == 0 ) {
					v = new Car( (long) i, "Make " + i, 5 );
				}
				else {
					v = new Truck( (long) i, "Make " + i, 1000 );
				}
				for ( int j = 0; j < 3; j++ ) {
					v.addPart( new Part( i * 10L + j, "Part " + i + "/" + j ) );
				}
				s.persist( v );
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	/**
	 * Root entity has a {@link SecondaryTable @SecondaryTable}. Its columns
	 * (here {@code review}) live in a separate table joined onto the root's
	 * primary table. The inner derived table must include the secondary-table
	 * join and project those columns; the outer's references to the secondary
	 * table's alias get rewritten through the derived alias.
	 */
	@Test
	void fetchJoinWithSecondaryTable(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			final List<Album> albums = s.createSelectionQuery(
					"from Album a left join fetch a.tracks order by a.id",
					Album.class
			).setMaxResults( 2 ).list();

			assertEquals( 2, albums.size() );
			assertEquals( 0L, albums.get( 0 ).getId() );
			assertEquals( 1L, albums.get( 1 ).getId() );
			assertEquals( "Review 0", albums.get( 0 ).getReview() );
			assertEquals( "Review 1", albums.get( 1 ).getReview() );
			for ( var a : albums ) {
				assertEquals( 3, a.getTracks().size() );
			}

			final String generated = sql.getSqlQueries().get( 0 ).toLowerCase();
			assertTrue( generated.contains( "from (select" ) );
			// secondary table join must be inside the inner derived table
			final int derivedClose = generated.indexOf( ')' );
			final String inner = generated.substring( 0, derivedClose );
			assertTrue( inner.contains( "albumdetails" ) );
		} );
	}

	/**
	 * Root entity is the root of a joined-inheritance hierarchy. Subtype tables
	 * are joined onto the primary table; the entity loader synthesises a CASE
	 * expression on those subtype tables for the discriminator. The rewrite
	 * absorbs those subtype tables' columns and rewrites the CASE.
	 */
	@Test
	void fetchJoinWithJoinedInheritanceRoot(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			final List<Vehicle> vehicles = s.createSelectionQuery(
					"from Vehicle v left join fetch v.parts order by v.id",
					Vehicle.class
			).setMaxResults( 2 ).list();

			assertEquals( 2, vehicles.size() );
			assertEquals( 0L, vehicles.get( 0 ).getId() );
			assertEquals( 1L, vehicles.get( 1 ).getId() );
			// runtime types must come back via the discriminator
			final Car car = assertInstanceOf( Car.class, vehicles.get( 0 ) );
			final Truck truck = assertInstanceOf( Truck.class, vehicles.get( 1 ) );
			assertEquals( 5, car.getSeats() );
			assertEquals( 1000, truck.getPayload() );
			for ( var v : vehicles ) {
				assertEquals( 3, v.getParts().size() );
			}

			final String generated = sql.getSqlQueries().get( 0 ).toLowerCase();
			assertTrue( generated.contains( "from (select" ) );
			// subtype tables must be inside the derived table; parts join outside
			final int derivedClose = generated.indexOf( ')' );
			final String inner = generated.substring( 0, derivedClose );
			final String outer = generated.substring( derivedClose );
			assertTrue( inner.contains( "car" ) );
			assertTrue( inner.contains( "truck" ) );
			assertTrue( outer.contains( "part" ) );
		} );
	}

	/**
	 * Joined-inheritance query whose root is the {@code Car} <em>subclass</em>
	 * rather than the {@code Vehicle} root. The TableGroup has a primary table
	 * for Car and joins to the super-table {@code Vehicle}; the rewrite needs to
	 * absorb whichever side ends up as a {@code TableReferenceJoin}.
	 */
	@Test
	void fetchJoinWithPolymorphicSubclassRoot(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			final List<Car> cars = s.createSelectionQuery(
					"from Car c left join fetch c.parts order by c.id",
					Car.class
			).setMaxResults( 2 ).list();

			assertEquals( 2, cars.size() );
			assertEquals( 0L, cars.get( 0 ).getId() );
			assertEquals( 2L, cars.get( 1 ).getId() );
			for ( Car c : cars ) {
				assertEquals( 5, c.getSeats() );
				assertNotNull( c.getMake() );
				assertEquals( 3, c.getParts().size() );
			}

			final String generated = sql.getSqlQueries().get( 0 ).toLowerCase();
			assertTrue( generated.contains( "from (select" ) );
		} );
	}

	@Entity(name = "Album")
	@SecondaryTable(
			name = "AlbumDetails",
			pkJoinColumns = @PrimaryKeyJoinColumn(name = "id")
	)
	public static class Album {
		@Id
		private Long id;
		private String title;
		@Column(table = "AlbumDetails")
		private String review;
		@OneToMany(mappedBy = "album", cascade = CascadeType.ALL)
		private List<Track> tracks = new ArrayList<>();

		public Album() {
		}

		public Album(Long id, String title, String review) {
			this.id = id;
			this.title = title;
			this.review = review;
		}

		public Long getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public String getReview() {
			return review;
		}

		public List<Track> getTracks() {
			return tracks;
		}

		public void addTrack(Track t) {
			tracks.add( t );
			t.setAlbum( this );
		}
	}

	@Entity(name = "Track")
	public static class Track {
		@Id
		private Long id;
		private String title;
		@ManyToOne
		@JoinColumn(name = "album_id")
		private Album album;

		public Track() {
		}

		public Track(Long id, String title) {
			this.id = id;
			this.title = title;
		}

		public Long getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public Album getAlbum() {
			return album;
		}

		public void setAlbum(Album a) {
			this.album = a;
		}
	}

	// No explicit @DiscriminatorColumn: Hibernate synthesises a CASE expression on
	// the subtype tables for the discriminator, which the rewrite must handle too.
	@Entity(name = "Vehicle")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class Vehicle {
		@Id
		private Long id;
		private String make;
		@OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
		private List<Part> parts = new ArrayList<>();

		public Vehicle() {
		}

		public Vehicle(Long id, String make) {
			this.id = id;
			this.make = make;
		}

		public Long getId() {
			return id;
		}

		public String getMake() {
			return make;
		}

		public List<Part> getParts() {
			return parts;
		}

		public void addPart(Part p) {
			parts.add( p );
			p.setVehicle( this );
		}
	}

	@Entity(name = "Car")
	public static class Car extends Vehicle {
		private int seats;

		public Car() {
		}

		public Car(Long id, String make, int seats) {
			super( id, make );
			this.seats = seats;
		}

		public int getSeats() {
			return seats;
		}
	}

	@Entity(name = "Truck")
	public static class Truck extends Vehicle {
		private int payload;

		public Truck() {
		}

		public Truck(Long id, String make, int payload) {
			super( id, make );
			this.payload = payload;
		}

		public int getPayload() {
			return payload;
		}
	}

	@Entity(name = "Part")
	public static class Part {
		@Id
		private Long id;
		private String name;
		@ManyToOne
		@JoinColumn(name = "vehicle_id")
		private Vehicle vehicle;

		public Part() {
		}

		public Part(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Vehicle getVehicle() {
			return vehicle;
		}

		public void setVehicle(Vehicle v) {
			this.vehicle = v;
		}
	}
}
