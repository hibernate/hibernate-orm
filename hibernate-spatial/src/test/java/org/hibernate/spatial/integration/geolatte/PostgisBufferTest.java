/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.integration.geolatte;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;

import org.geolatte.geom.C2D;
import org.geolatte.geom.Point;
import org.geolatte.geom.Polygon;
import org.geolatte.geom.crs.CoordinateReferenceSystem;
import org.geolatte.geom.crs.CoordinateReferenceSystems;

import org.hibernate.spatial.dialect.postgis.PostgisPG95Dialect;

import org.junit.Test;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.geolatte.geom.builder.DSL.c;
import static org.geolatte.geom.builder.DSL.point;
import static org.geolatte.geom.builder.DSL.polygon;
import static org.geolatte.geom.builder.DSL.ring;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea, Karel Maesen
 */
@RequiresDialect(PostgisPG95Dialect.class)
public class PostgisBufferTest extends BaseCoreFunctionalTestCase {

	public static CoordinateReferenceSystem<C2D> crs = CoordinateReferenceSystems.PROJECTED_2D_METER;

	private final Polygon<C2D> window = polygon( crs, ring( c( 1, 1 ), c( 1, 20 ),
															c( 20, 20 ), c( 20, 1 ), c( 1, 1 )
	) );


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
			event.setId( 1L );
			event.setName( "Hibernate ORM presentation" );
			Point<C2D> pnt = point( crs, c( 10, 5 ) );
			event.setLocation( pnt );
			session.persist( event );
			return event.getId();
		} );

		doInHibernate( this::sessionFactory, session -> {
			List<Event> events = session.createQuery(
					"select e " +
							"from Event e " +
							"where buffer(:window, 100) is not null", Event.class )
					.setParameter( "window", window )
					.getResultList();

			assertEquals( 1, events.size() );

		} );
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
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
