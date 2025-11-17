/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.oracle.hhh15669;


import java.util.List;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.spatial.HibernateSpatialConfigurationSettings;
import org.hibernate.spatial.testing.SpatialSessionFactoryAware;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.geolatte.geom.G2D;
import org.geolatte.geom.Point;
import org.geolatte.geom.Polygon;

import static org.geolatte.geom.builder.DSL.g;
import static org.geolatte.geom.builder.DSL.point;
import static org.geolatte.geom.builder.DSL.polygon;
import static org.geolatte.geom.builder.DSL.ring;
import static org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;

@JiraKey(value = "HHH-15669")
@DomainModel(annotatedClasses = { Foo.class })
@RequiresDialect(value = OracleDialect.class)
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = HibernateSpatialConfigurationSettings.ORACLE_OGC_STRICT, value = "true")
})
public class TestStWithinBug extends SpatialSessionFactoryAware {
	Polygon<G2D> filter = polygon( WGS84, ring(
			g( 0, 0 ),
			g( 10, 0 ),
			g( 10, 10 ),
			g( 0, 10 ),
			g( 0, 0 )
	) );

	@BeforeEach
	public void before() {
		scope.inTransaction(
				session -> {
					session.persist( new Foo( 1, point( WGS84, g( 5, 5 ) ) ) );
					session.persist( new Foo( 2, point( WGS84, g( 12, 12 ) ) ) );
					session.persist( new Foo( 3, point( WGS84, g( -1, -1 ) ) ) );
				}
		);
	}


	@Test
	public void testHql() {
		scope.inTransaction( session -> {

			List<Foo> list = session
					.createQuery( "from Foo where st_within(point, :filter) = true", Foo.class )
					.setParameter( "filter", filter )
					.getResultList();
			assertThat( list, hasSize( 1 ) );
			assertThat( list.get( 0 ).id, equalTo( 1L ) );
		} );
	}


	@AfterEach
	public void cleanup() {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Foo" )
				.executeUpdate() );
	}

}

@Entity(name = "Foo")
@Table(name = "Foo")
class Foo {
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
