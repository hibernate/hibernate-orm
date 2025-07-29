/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.postgis;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.geolatte.geom.G2D;
import org.geolatte.geom.Point;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;


import static org.geolatte.geom.builder.DSL.g;
import static org.geolatte.geom.builder.DSL.point;
import static org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = PostgisDollarQuoteNativeQueryTest.Location.class )
@SessionFactory
@RequiresDialect(PostgreSQLDialect.class)
@Jira( "https://hibernate.atlassian.net/browse/HHH-18956" )
class PostgisDollarQuoteNativeQueryTest {

	private static final Long LOCATION_ID = 123412L;
	private static final String DOLLAR_QUOTE = "$asdas$";

	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final Point<G2D> point = point( WGS84, g( 30.5, 50.4 ) );
			//noinspection SqlSourceToSinkFlow
			s.createNativeMutationQuery(
					String.format(
							"INSERT INTO location (id, point) " +
							"VALUES (%s, %s) " +
							"ON CONFLICT DO NOTHING;",
							DOLLAR_QUOTE + LOCATION_ID + DOLLAR_QUOTE,
							String.format(
									"ST_SetSRID(ST_GeomFromGeoJSON(%s%s%s), 4326)",
									DOLLAR_QUOTE,
									toJsonString( point ),
									DOLLAR_QUOTE
							)
					)
			).executeUpdate();

			final Location location = s.find( Location.class, LOCATION_ID );
			assertEquals( point, location.point );
		} );
	}

	private static String toJsonString(Point<G2D> point) {
		return "{\"type\":\"" + point.getGeometryType().getCamelCased() + "\",\"coordinates\":" + point.getPosition() + "}";
	}

	@Entity(name = "Location")
	public static class Location {

		@Id
		Long id;

		@JdbcTypeCode(SqlTypes.GEOMETRY)
		Point<G2D> point;

		public Location() {
		}

		public Location(Long id, Point<G2D> point) {
			this.id = id;
			this.point = point;
		}

		@Override
		public String toString() {
			return "Location{" +
				"id=" + id +
				", point=" + point +
				'}';
		}
	}
}
