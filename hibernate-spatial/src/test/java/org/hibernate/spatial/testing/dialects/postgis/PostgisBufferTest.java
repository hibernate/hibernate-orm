/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.testing.dialects.postgis;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.spatial.dialect.postgis.PostgisPG95Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgisPG95Dialect.class)
public class PostgisBufferTest extends BaseCoreFunctionalTestCase {

    private GeometryFactory geometryFactory = new GeometryFactory();

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
            Event.class,
        };
    }

    @Test
    public void test() {
        Long addressId = doInHibernate( this::sessionFactory, session -> {
            Event event = new Event();
            event.setId( 1L);
            event.setName( "Hibernate ORM presentation");
            Point point = geometryFactory.createPoint( new Coordinate( 10, 5 ) );
            event.setLocation( point );

            session.persist( event );
            return event.getId();
        });

        doInHibernate( this::sessionFactory, session -> {
            Coordinate [] coordinates = new Coordinate[] {
                    new Coordinate(1,1), new Coordinate(20,1), new Coordinate(20,20),
                    new Coordinate(1,20), new Coordinate(1,1)
            };
            Polygon window = geometryFactory.createPolygon( coordinates );

            List<Event> events = session.createQuery(
                "select e " +
                "from Event e " +
                "where buffer(:window, 100) is not null", Event.class)
            .setParameter("window", window)
            .getResultList();

            assertEquals(1, events.size());

            List<Geometry> locations = session.createQuery(
                "select buffer(e.location, 10) " +
                "from Event e ", Geometry.class)
            .getResultList();

            assertEquals(1, locations.size());
        });
    }

@Entity(name = "Event")
public static class Event {

    @Id
    private Long id;

    private String name;

    private Point location;

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
