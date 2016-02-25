/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.spatial;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.spatial.dialect.postgis.PostgisDialect;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import static org.junit.Assert.assertEquals;

import static org.hibernate.userguide.util.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgisDialect.class)
public class SpatialTest extends BaseEntityManagerFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
            Event.class,
        };
    }

    @Test
    public void test() {
        Long addressId = doInJPA( this::entityManagerFactory, entityManager -> {
            try {
                //tag::spatial-types-point-creation-example[]
                Event event = new Event();
                event.setId( 1L);
                event.setName( "Hibernate ORM presentation");
                event.setLocation( (Point) new WKTReader().read( "POINT(10 5)"));

                entityManager.persist( event );
                //end::spatial-types-point-creation-example[]
                return event.getId();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        });

        doInJPA( this::entityManagerFactory, entityManager -> {
            Event event = entityManager.find( Event.class, addressId);
            Coordinate coordinate = event.getLocation().getCoordinate();
            assertEquals( 10.0d, coordinate.getOrdinate( Coordinate.X), 0.1);
            assertEquals( 5.0d, coordinate.getOrdinate( Coordinate.Y), 0.1);
        });

        doInJPA( this::entityManagerFactory, entityManager -> {
            try {
                //tag::spatial-types-query-example[]
                Event event = entityManager.createQuery(
					"select e " +
					"from Event e " +
					"where within(e.location, :filter) = true", Event.class)
				.setParameter("filter", new WKTReader().read( "POLYGON((1 1,20 1,20 20,1 20,1 1))"))
                .getSingleResult();
                //end::spatial-types-query-example[]
                Coordinate coordinate = event.getLocation().getCoordinate();
                assertEquals( 10.0d, coordinate.getOrdinate( Coordinate.X), 0.1);
                assertEquals( 5.0d, coordinate.getOrdinate( Coordinate.Y), 0.1);
            }
            catch (ParseException e) {
                throw new RuntimeException(e);
            }
        });
    }

    //tag::spatial-types-mapping-example[]
    @Entity(name = "Event")
    public static class Event {

        @Id
        private Long id;

        private String name;

        @Type(type = "jts_geometry")
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
