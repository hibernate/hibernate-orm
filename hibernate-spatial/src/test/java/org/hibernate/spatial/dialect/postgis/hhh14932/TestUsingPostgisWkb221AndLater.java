/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.postgis.hhh14932;

import java.util.List;

import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.geolatte.geom.G2D;
import org.geolatte.geom.Point;

import static org.geolatte.geom.builder.DSL.point;
import static org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84;
import static org.junit.Assert.assertEquals;


@JiraKey(value = "HHH-14932")

@RequiresDialect(PostgreSQLDialect.class)
public class TestUsingPostgisWkb221AndLater extends BaseCoreFunctionalTestCase {

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
