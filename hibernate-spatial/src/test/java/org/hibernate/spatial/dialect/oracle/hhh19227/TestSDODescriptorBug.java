/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.oracle.hhh19227;


import org.hibernate.dialect.OracleDialect;
import org.hibernate.query.Query;
import org.hibernate.spatial.testing.SpatialSessionFactoryAware;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@JiraKey(value = "HHH-19227")
@RequiresDialect(value = OracleDialect.class)
@DomainModel(standardModels = StandardDomainModel.ANIMAL)
@SessionFactory
public class TestSDODescriptorBug extends SpatialSessionFactoryAware {

	private static Geometry parse(String wkt) {
		try {
			return new WKTReader().read( wkt );
		}
		catch (ParseException e) {
			throw new RuntimeException( e );
		}
	}

	private static final Geometry REFERENCE =
			parse( "POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))" );
	private static final Geometry EQUALS_REFERENCE =
			parse( "POLYGON ((10 20, 20 20, 20 10, 10 10, 10 20))" );
	private static final Geometry DISJOINT_WITH_REFERENCE =
			parse( "POLYGON ((25 20, 35 20, 35 10, 25 10, 25 20))" );
	private static final Geometry TOUCHES_REFERENCE =
			parse( "POLYGON ((20 25, 30 25, 30 15, 20 15, 20 25))" );
	private static final Geometry OVERLAPS_REFERENCE =
			parse( "POLYGON ((15 15, 15 25, 25 25, 25 15, 15 15))" );
	private static final Geometry CROSSES_REFERENCE =
			parse( "LINESTRING (25 15, 15 15)" );
	private static final Geometry CONTAINED_BY_REFERENCE =
			parse( "POLYGON ((15 15, 15 20, 20 20, 20 15, 15 15))" );
	private static final Geometry CONTAINS_REFERENCE =
			parse( "POLYGON ((10 10, 10 25, 25 25, 25 10, 10 10))" );
	private static final Geometry INTERSECTS_BOUNDARY =
			parse( "POLYGON ((20 25, 30 25, 30 15, 20 15, 20 25))" );
	private static final Geometry INTERSECTS_INTERIOR =
			parse( "POLYGON ((15 15, 15 25, 25 25, 25 15, 15 15))" );


	@Test
	void ogcEquals() {
		scope.inSession( session -> {
			Query<Boolean> query = session.createQuery( "select equals(:a, :b)", Boolean.class );
			query.setParameter( "a", REFERENCE );
			query.setParameter( "b", EQUALS_REFERENCE );
			Object result = query.getSingleResult();
			assertThat( result, is( true ) );
		} );

	}

	@Test
	void ogcDisjoint() {
		scope.inSession( session -> {
			Query<Boolean> query = session.createQuery( "select disjoint(:a, :b)", Boolean.class );
			query.setParameter( "a", REFERENCE );
			query.setParameter( "b", DISJOINT_WITH_REFERENCE );
			Object result = query.getSingleResult();
			assertThat( result, is( true ) );
		} );
	}

	@Test
	void ogcTouches() {
		scope.inSession( session -> {
			Query<Boolean> query = session.createQuery( "select touches(:a, :b)", Boolean.class );
			query.setParameter( "a", REFERENCE );
			query.setParameter( "b", TOUCHES_REFERENCE );
			Object result = query.getSingleResult();
			assertThat( result, is( true ) );
		} );
	}

	@Test
	void ogcOverlaps() {
		scope.inSession( session -> {
			Query<Boolean> query = session.createQuery( "select overlaps(:a, :b)", Boolean.class );
			query.setParameter( "a", REFERENCE );
			query.setParameter( "b", OVERLAPS_REFERENCE );
			Object result = query.getSingleResult();
			assertThat( result, is( true ) );
		} );
	}

	@Test
	void ogcCrosses() {
		scope.inSession( session -> {
			Query<Boolean> query = session.createQuery( "select crosses(:a, :b)", Boolean.class );
			query.setParameter( "a", REFERENCE );
			query.setParameter( "b", CROSSES_REFERENCE );
			Object result = query.getSingleResult();
			assertThat( result, is( true ) );
		} );
	}

	@Test
	void ogcContains() {
		scope.inSession( session -> {
			Query<Boolean> query = session.createQuery( "select contains(:a, :b)", Boolean.class );
			query.setParameter( "a", REFERENCE );
			query.setParameter( "b", CONTAINED_BY_REFERENCE );
			Object result = query.getSingleResult();
			assertThat( result, is( true ) );
		} );
	}

	@Test
	void ogcWithin() {
		scope.inSession( session -> {
			Query<Boolean> query = session.createQuery( "select within(:a, :b)", Boolean.class );
			query.setParameter( "a", REFERENCE );
			query.setParameter( "b", CONTAINS_REFERENCE );
			Object result = query.getSingleResult();
			assertThat( result, is( true ) );
		} );
	}

	@Test
	void ogcIntersects_polygonal() {
		scope.inSession( session -> {
			Query<Boolean> query = session.createQuery( "select intersects(:a, :b)", Boolean.class );
			query.setParameter( "a", REFERENCE );
			query.setParameter( "b", INTERSECTS_BOUNDARY );
			Object result = query.getSingleResult();
			assertThat( result, is( true ) );
		} );
	}

	@Test
	void ogcIntersects_lineal() {
		scope.inSession( session -> {
			Query<Boolean> query = session.createQuery( "select intersects(:a, :b)", Boolean.class );
			query.setParameter( "a", REFERENCE );
			query.setParameter( "b", INTERSECTS_INTERIOR );
			Object result = query.getSingleResult();
			assertThat( result, is( true ) );
		} );
	}


}
