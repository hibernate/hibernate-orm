/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.postgis;

import java.util.List;

import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import org.geolatte.geom.C2D;
import org.geolatte.geom.Point;
import org.geolatte.geom.crs.CoordinateReferenceSystem;
import org.geolatte.geom.crs.CoordinateReferenceSystems;

import static org.geolatte.geom.builder.DSL.c;
import static org.geolatte.geom.builder.DSL.point;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the Postgis KNN distance functions (corresponding to the <-> and <<->> operators).
 */
@RequiresDialect(PostgreSQLDialect.class)
@DomainModel(annotatedClasses = { PostgisDistanceOperatorsTest.Neighbor.class })
@SessionFactory(useCollectingStatementInspector = true)
public class PostgisDistanceOperatorsTest {
	public static CoordinateReferenceSystem<C2D> crs = CoordinateReferenceSystems.PROJECTED_2D_METER;

	private final Point<C2D> searchPoint = point( crs, c( 0.0, 0.0 ) );

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for ( int i = 10; i > 0; i-- ) {
						Neighbor neighbor = Neighbor.from( point( crs, c( i, 0.0 ) ), i );
						session.persist( neighbor );
					}
					session.flush();
					session.clear();
				}
		);
	}

	@Test
	public void testDistance2D(SessionFactoryScope scope) {
		SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction(
				session -> {
					TypedQuery<Neighbor> query = session.createQuery(
									"select n from Neighbor n order by distance_2d(n.point, :pnt )", Neighbor.class )
							.setParameter( "pnt", searchPoint );
					List<Neighbor> results = query.getResultList();
					assertFalse( results.isEmpty() );
					String sql = inspector.getSqlQueries().get( 0 );
					assertTrue( sql.matches( ".*order by.*point\\w*<->.*" ), "<-> operator is not rendered correctly" );
				}
		);
	}

	@Test
	public void testDistance2DBBox(SessionFactoryScope scope) {
		SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction(
				session -> {
					TypedQuery<Neighbor> query = session.createQuery(
									"select n from Neighbor n order by distance_2d_bbox(n.point, :pnt )", Neighbor.class )
							.setParameter( "pnt", searchPoint );
					List<Neighbor> results = query.getResultList();
					assertFalse( results.isEmpty() );
					String sql = inspector.getSqlQueries().get( 0 );
					assertTrue( sql.matches( ".*order by.*point\\w*<#>.*" ), "<#> operator is not rendered correctly" );
				}
		);
	}

	@Test
	public void testDistanceNDBBox(SessionFactoryScope scope) {
		SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction(
				session -> {
					TypedQuery<Neighbor> query = session.createQuery(
									"select n from Neighbor n order by distance_centroid_nd(n.point, :pnt )", Neighbor.class )
							.setParameter( "pnt", searchPoint );
					List<Neighbor> results = query.getResultList();
					assertFalse( results.isEmpty() );
					String sql = inspector.getSqlQueries().get( 0 );
					assertTrue(
							sql.matches( ".*order by.*point\\w*<<->>.*" ),
							"<<->>> operator is not rendered correctly"
					);
				}
		);
	}

	@Test
	public void testInvalidArguments(SessionFactoryScope scope) {
		SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		IllegalArgumentException thrown = assertThrows( IllegalArgumentException.class, () ->
				scope.inTransaction(
						session -> {
							TypedQuery<Neighbor> query = session.createQuery(
											"select n from Neighbor n order by distance_2d_bbox(n.point, :pnt )",
											Neighbor.class
									)
									.setParameter( "pnt", 130 );
							List<Neighbor> results = query.getResultList();
							assertFalse( results.isEmpty() );
							String sql = inspector.getSqlQueries().get( 0 );
							assertTrue(
									sql.matches( ".*order by.*point\\w*<#>.*" ),
									"<#> operator is not rendered correctly"
							);
						}
				)
		);
		assertTrue(
				thrown.getMessage()
						.contains("Parameter 1 of function 'distance_2d_bbox()' has type 'SPATIAL', but argument is of type 'java.lang.Integer'")
		);

	}

	@AfterEach
	public void cleanUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from Neighbor" );
				}
		);
	}

	@Entity(name = "Neighbor")
	@Table(name = "neighbor")
	public static class Neighbor {

		static Neighbor from(Point<C2D> pnt, Integer i) {
			Neighbor res = new Neighbor();
			res.point = pnt;
			res.num = i;
			return res;
		}

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		private Integer num;

		Point<C2D> point;
	}
}
