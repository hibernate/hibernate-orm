/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.spatial;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.spatial.dialect.postgis.PostgisDialect;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
//tag::spatial-types-mapping-example[]
import com.vividsolutions.jts.geom.Point;

//end::spatial-types-mapping-example[]
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import static org.junit.Assert.assertEquals;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgisDialect.class)
public class SpatialTest extends BaseEntityManagerFunctionalTestCase {

    GeometryFactory geometryFactory = new GeometryFactory();

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
