package org.hibernate.spatial.dialect.sqlserver;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.geolatte.geom.G2D;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.LineString;
import org.geolatte.geom.Point;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.spatial.GeolatteGeometryJavaType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.geolatte.geom.builder.DSL.g;
import static org.geolatte.geom.builder.DSL.point;
import static org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks the axes of a {@code geography} value against what SQL Server itself writes and reads: a point is
 * serialized latitude first, while a {@link G2D} holds the longitude first.
 */
@DomainModel(annotatedClasses = SqlServerGeographyTest.Place.class)
@SessionFactory
@RequiresDialect(SQLServerDialect.class)
@Jira("https://hibernate.atlassian.net/browse/HHH-21058")
class SqlServerGeographyTest {

	// Seattle; its longitude is no valid latitude, so a swap of the axes is rejected rather than stored
	private static final double LATITUDE = 47.651;
	private static final double LONGITUDE = -122.349;

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void readsWhatSqlServerWrites(SessionFactoryScope scope) {
		scope.inTransaction( s -> s.createNativeMutationQuery(
						"insert into Place (id, location) values (1, geography::Point(:lat, :lon, 4326))" )
				.setParameter( "lat", LATITUDE )
				.setParameter( "lon", LONGITUDE )
				.executeUpdate() );
		scope.inTransaction( s -> {
			final G2D position = ( (Point<G2D>) s.find( Place.class, 1 ).location ).getPosition();
			assertEquals( LATITUDE, position.getLat(), 1e-9 );
			assertEquals( LONGITUDE, position.getLon(), 1e-9 );
		} );
	}

	@Test
	void readsEveryPositionOfALineString(SessionFactoryScope scope) {
		// the Well-Known Text is longitude first
		scope.inTransaction( s -> s.createNativeMutationQuery(
						"insert into Place (id, location) values (1, "
						+ "geography::STGeomFromText('LINESTRING(-122.36 47.656, -122.343 47.657)', 4326))" )
				.executeUpdate() );
		scope.inTransaction( s -> {
			@SuppressWarnings("unchecked")
			final LineString<G2D> line = (LineString<G2D>) s.find( Place.class, 1 ).location;
			assertEquals( -122.36, line.getPositionN( 0 ).getLon(), 1e-9 );
			assertEquals( 47.656, line.getPositionN( 0 ).getLat(), 1e-9 );
			assertEquals( -122.343, line.getPositionN( 1 ).getLon(), 1e-9 );
			assertEquals( 47.657, line.getPositionN( 1 ).getLat(), 1e-9 );
		} );
	}

	@Test
	void writesWhatSqlServerReads(SessionFactoryScope scope) {
		scope.inTransaction( s -> s.persist( new Place( 1, point( WGS84, g( LONGITUDE, LATITUDE ) ) ) ) );
		scope.inTransaction( s -> {
			final Object[] row = s.createNativeQuery(
							"select location.Lat, location.Long from Place where id = 1", Object[].class )
					.getSingleResult();
			assertEquals( LATITUDE, ( (Number) row[0] ).doubleValue(), 1e-9 );
			assertEquals( LONGITUDE, ( (Number) row[1] ).doubleValue(), 1e-9 );
		} );
	}

	// a parameter bound as a geography; one bound by its value alone takes the default type of a geometry
	@Test
	void bindsAParameterAsSqlServerReadsIt(SessionFactoryScope scope) {
		@SuppressWarnings({"unchecked", "rawtypes"})
		final BasicType<Geometry<G2D>> geography = (BasicType) scope.getSessionFactory().getTypeConfiguration()
				.getBasicTypeRegistry()
				.resolve( GeolatteGeometryJavaType.GEOMETRY_INSTANCE, SqlServerGeographyType.INSTANCE );
		scope.inTransaction( s -> s.createNativeMutationQuery(
						"insert into Place (id, location) values (1, geography::Point(:lat, :lon, 4326))" )
				.setParameter( "lat", LATITUDE )
				.setParameter( "lon", LONGITUDE )
				.executeUpdate() );
		scope.inTransaction( s -> {
			final Double distance = s.createQuery(
							"select st_distance(p.location, :point) from Place p where p.id = 1", Double.class )
					.setParameter( "point", point( WGS84, g( LONGITUDE, LATITUDE ) ), geography )
					.getSingleResult();
			assertEquals( 0.0d, distance, 1e-6 );
		} );
	}

	@Entity(name = "Place")
	public static class Place {

		@Id
		Integer id;

		@JdbcTypeCode(SqlTypes.GEOGRAPHY)
		Geometry<G2D> location;

		public Place() {
		}

		public Place(Integer id, Geometry<G2D> location) {
			this.id = id;
			this.location = location;
		}
	}
}
