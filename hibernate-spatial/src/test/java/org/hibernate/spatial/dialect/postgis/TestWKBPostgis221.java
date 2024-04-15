/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.postgis;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import org.geolatte.geom.G2D;
import org.geolatte.geom.Point;

import static org.geolatte.geom.builder.DSL.g;
import static org.geolatte.geom.builder.DSL.point;
import static org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84;
import static org.junit.Assert.assertEquals;


@TestForIssue(jiraKey = "HHH-14932")
@RequiresDialect(PostgisPG95Dialect.class)
public class TestWKBPostgis221 extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Foo.class };
	}

	@Before
	public void setup() {
		inTransaction( session -> session.persist( new Foo(
				1,
				point( WGS84 )
		) ) );
	}

	@Test
	public void test() {
		inTransaction( session -> {
			List<Foo> list = session
					.createQuery( "from Foo", Foo.class )
					.getResultList();
			assertEquals( point( WGS84 ), list.get( 0 ).point );
		} );
	}

	@Entity(name = "Foo")
	@Table(name = "Foo")
	public static class Foo {
		@Id
		long id;
		Point<G2D> point;

		public Foo() {
		}

		public Foo(long id, Point<G2D> point) {
			this.id = id;
			this.point = point;
		}

	}
}
