/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.postgis;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.geolatte.geom.C2D;
import org.geolatte.geom.Point;
import org.geolatte.geom.Polygon;
import org.geolatte.geom.crs.CoordinateReferenceSystem;
import org.geolatte.geom.crs.CoordinateReferenceSystems;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.geolatte.geom.builder.DSL.c;
import static org.geolatte.geom.builder.DSL.point;
import static org.geolatte.geom.builder.DSL.polygon;
import static org.geolatte.geom.builder.DSL.ring;

/**
 * Integration tests for Postgis
 *
 * @author Vlad Mihalcea, Karel Maesen
 */
@RequiresDialect(PostgreSQLDialect.class)
@DomainModel(
		annotatedClasses = {
				PostgisTest.Event.class
		}
)
@SessionFactory
public class PostgisTest {

	public static CoordinateReferenceSystem<C2D> crs = CoordinateReferenceSystems.PROJECTED_2D_METER;

	private final Polygon<C2D> window = polygon( crs, ring( c( 1, 1 ), c( 1, 20 ),
			c( 20, 20 ), c( 20, 1 ), c( 1, 1 )
	) );

	@AfterEach
	public void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testBuffer(SessionFactoryScope scope) {
		insertEvent( c( 10, 5 ), scope );

		scope.inTransaction(
				session -> {
					List<Event> events = session.createQuery(
									"select e " +
									"from Event e " +
									"where within( e.location, buffer(:window, 100)) = true", Event.class )
							.setParameter( "window", window )
							.getResultList();

					assertThat( events ).hasSize( 1 );
				}
		);
	}

	@Test
	@Disabled
	//TODO -- register these extra functions
	public void testMakeEnvelope(SessionFactoryScope scope) {
		insertEvent( c( 10, 5 ), scope );

		scope.inTransaction( session -> {
			List<Event> events = session.createQuery(
							"select e " +
							"from Event e " +
							"where within(e.location, makeenvelope(0, 0, 11, 11, -1 )) = true", Event.class )
					.getResultList();

			assertThat( events ).hasSize( 1 );
		} );
	}

	private Long insertEvent(C2D position, SessionFactoryScope scope) {
		return scope.fromTransaction( session -> {
			Event event = new Event();
			event.setName( "Hibernate ORM presentation" );
			Point<C2D> pnt = point( crs, position );
			event.setLocation( pnt );
			session.persist( event );
			return event.getId();
		} );
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		private Point<C2D> location;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Point getLocation() {
			return location;
		}

		public void setLocation(Point location) {
			this.location = location;
		}
	}
}
