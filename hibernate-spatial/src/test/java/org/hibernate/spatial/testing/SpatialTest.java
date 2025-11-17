/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.dialect.PostgreSQLDialect;


import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
//tag::spatial-types-mapping-example[]
import org.locationtech.jts.geom.Point;

//end::spatial-types-mapping-example[]
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQLDialect.class)
@Disabled
@Jpa(
		annotatedClasses = {
				SpatialTest.Event.class
		}
)
public class SpatialTest  {

	private final GeometryFactory geometryFactory = new GeometryFactory();

	@Test
	public void test(EntityManagerFactoryScope scope) {
		Long addressId = scope.fromTransaction( entityManager -> {
			//tag::spatial-types-point-creation-example[]
			Event event = new Event();
			event.setId( 1L);
			event.setName( "Hibernate ORM presentation");
			Point point = geometryFactory.createPoint( new Coordinate( 10, 5 ) );
			event.setLocation( point );

			entityManager.persist( event );
			//end::spatial-types-point-creation-example[]
			return event.getId();
		});

		scope.inTransaction(  entityManager -> {
			Event event = entityManager.find( Event.class, addressId);
			Coordinate coordinate = event.getLocation().getCoordinate();
			assertEquals( 10.0d, coordinate.getOrdinate( Coordinate.X), 0.1);
			assertEquals( 5.0d, coordinate.getOrdinate( Coordinate.Y), 0.1);
		});

		scope.inTransaction(  entityManager -> {
			Coordinate [] coordinates = new Coordinate[] {
				new Coordinate(1,1), new Coordinate(20,1), new Coordinate(20,20),
				new Coordinate(1,20), new Coordinate(1,1)
			};
			//tag::spatial-types-query-example[]
			Polygon window = geometryFactory.createPolygon( coordinates );
			Event event = entityManager.createQuery(
				"select e " +
				"from Event e " +
				"where within(e.location, :window) = true", Event.class)
			.setParameter("window", window)
			.getSingleResult();
			//end::spatial-types-query-example[]
			Coordinate coordinate = event.getLocation().getCoordinate();
			assertEquals( 10.0d, coordinate.getOrdinate( Coordinate.X), 0.1);
			assertEquals( 5.0d, coordinate.getOrdinate( Coordinate.Y), 0.1);
		});
	}

//tag::spatial-types-mapping-example[]
@Entity(name = "Event")
public static class Event {

	@Id
	private Long id;

	private String name;

	private Point location;

	//Getters and setters are omitted for brevity
//end::spatial-types-mapping-example[]
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
//tag::spatial-types-mapping-example[]
}
//end::spatial-types-mapping-example[]
}
