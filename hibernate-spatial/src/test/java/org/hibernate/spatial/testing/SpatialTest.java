/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Ignore;
import org.junit.Test;

import org.locationtech.jts.geom.Coordinate;
//tag::spatial-types-mapping-example[]
import org.locationtech.jts.geom.Point;

//end::spatial-types-mapping-example[]
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import static org.junit.Assert.assertEquals;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQLDialect.class)
@Ignore
public class SpatialTest extends BaseEntityManagerFunctionalTestCase {

	private final GeometryFactory geometryFactory = new GeometryFactory();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Event.class,
		};
	}

	@Test
	public void test() {
		Long addressId = doInJPA( this::entityManagerFactory, entityManager -> {
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

		doInJPA( this::entityManagerFactory, entityManager -> {
			Event event = entityManager.find( Event.class, addressId);
			Coordinate coordinate = event.getLocation().getCoordinate();
			assertEquals( 10.0d, coordinate.getOrdinate( Coordinate.X), 0.1);
			assertEquals( 5.0d, coordinate.getOrdinate( Coordinate.Y), 0.1);
		});

		doInJPA( this::entityManagerFactory, entityManager -> {
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
