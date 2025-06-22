/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.integration.functions;

import java.util.List;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.Query;
import org.hibernate.spatial.testing.SpatialTestBase;
import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.domain.GeomEntity;
import org.hibernate.spatial.testing.domain.JtsGeomEntity;
import org.hibernate.spatial.testing.domain.SpatialDomainModel;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialects;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DomainModel(modelDescriptorClasses = SpatialDomainModel.class)
@SessionFactory
@RequiresDialects({
		@RequiresDialect(PostgreSQLDialect.class),
		@RequiresDialect(H2Dialect.class)
})
public class BasicFunctionTest extends SpatialTestBase {

	@Test
	public void testJTS() {
		scope.inTransaction( (session) -> {
			Query<?> query = session.createQuery(
					"select st_convexhull(geom) from JtsGeomEntity",
					org.locationtech.jts.geom.Geometry.class
			);
			List<?> results = query.getResultList();
			assertFalse( results.isEmpty() );
			assertInstanceOf( org.locationtech.jts.geom.Geometry.class, results.get( 0 ) );
		} );
	}

	@Override
	public TestSupport.TestDataPurpose purpose() {
		return TestSupport.TestDataPurpose.SpatialFunctionsData;
	}


	@Test
	public void testGeolatte() {
		scope.inTransaction( (session) -> {
			Query<?> query = session.createQuery(
					"select st_convexhull(e.geom) from GeomEntity e",
					org.geolatte.geom.Geometry.class
			);
			List<?> results = query.getResultList();
			assertFalse( results.isEmpty() );
			assertInstanceOf( org.geolatte.geom.Geometry.class, results.get( 0 ) );
		} );
	}

	@Test
	public void testJtsIntersectsParam() {
		scope.inTransaction( (session) -> {
			Query<JtsGeomEntity> query = session.createQuery(
					"select g from JtsGeomEntity g where st_intersects(g.geom, st_boundary(:poly) ) = true",
					JtsGeomEntity.class
			);
			query.setParameter( "poly", org.geolatte.geom.jts.JTS.to( filterGeometry ) );
			List<?> results = query.getResultList();
			assertFalse( results.isEmpty() );
		} );
	}

	@Test
	public void testGeolatteIntersectsParam() {
		scope.inTransaction( (session) -> {
			Query<GeomEntity> query = session.createQuery(
					"select g from GeomEntity g where st_intersects(g.geom, st_boundary(:poly) ) = true",
					GeomEntity.class
			);
			query.setParameter( "poly", filterGeometry );
			List<?> results = query.getResultList();
			assertFalse( results.isEmpty() );
		} );
	}


}
