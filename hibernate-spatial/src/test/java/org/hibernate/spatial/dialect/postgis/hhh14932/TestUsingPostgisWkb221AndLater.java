/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.postgis.hhh14932;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.geolatte.geom.G2D;
import org.geolatte.geom.Point;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.geolatte.geom.builder.DSL.point;
import static org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84;


@JiraKey(value = "HHH-14932")
@RequiresDialect(PostgreSQLDialect.class)
@DomainModel(
		annotatedClasses = {
				TestUsingPostgisWkb221AndLater.Foo.class
		}
)
@SessionFactory
public class TestUsingPostgisWkb221AndLater {


	@BeforeAll
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Foo(
				1,
				point( WGS84 )
		) ) );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Foo> list = session
					.createQuery( "from Foo", Foo.class )
					.getResultList();
			assertThat( list.get( 0 ).point ).isEqualTo( point( WGS84 ) );
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
