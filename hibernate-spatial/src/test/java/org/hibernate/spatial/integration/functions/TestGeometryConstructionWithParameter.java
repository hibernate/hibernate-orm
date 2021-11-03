/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.integration.functions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.testing.IsSupportedBySpatial;
import org.hibernate.spatial.testing.SpatialTestBase;
import org.hibernate.spatial.testing.datareader.TestSupport;

import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

import org.geolatte.geom.G2D;
import org.geolatte.geom.Geometry;

import static org.geolatte.geom.builder.DSL.g;
import static org.geolatte.geom.builder.DSL.polygon;
import static org.geolatte.geom.builder.DSL.ring;
import static org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84;

@RequiresDialectFeature(feature = IsSupportedBySpatial.class)
@SessionFactory
public class TestGeometryConstructionWithParameter extends SpatialTestBase {

	final private Map<CommonSpatialFunction, String> templates = new HashMap<>();

	TestGeometryConstructionWithParameter() {
		templates.put(
				CommonSpatialFunction.ST_ENVELOPE,
				"select g from GeomEntity g where st_intersects(g.geom, st_envelope(:poly) ) = true"
		);
		templates.put(
				CommonSpatialFunction.ST_BOUNDARY,
				"select g from GeomEntity g where st_intersects(g.geom, st_boundary(:poly) ) = true"
		);
		templates.put(
				CommonSpatialFunction.ST_BUFFER,
				"select g from GeomEntity g where st_intersects(g.geom, st_buffer(:poly, 1.0) ) = true"
		);
		templates.put(
				CommonSpatialFunction.ST_CONVEXHULL,
				//TODO -- this is a degenerate case of convexhull
				"select g from GeomEntity g where st_intersects(g.geom, st_convexhull(:poly) ) = true"
		);
	}

	@Override
	public TestSupport.TestDataPurpose purpose() {
		return TestSupport.TestDataPurpose.SpatialFunctionsData;
	}

	@TestFactory
	public Stream<DynamicTest> testFunctions() {
		return Arrays.stream( CommonSpatialFunction.values() )
				.filter( f ->
								 f.getType() == CommonSpatialFunction.Type.CONSTRUCTION &&
										 isSupported( f ) &&
										 templateAvailable( f ) )
				.map( this::buildTestFunction );
	}


	private DynamicTest buildTestFunction(CommonSpatialFunction func) {
		return DynamicTest.dynamicTest( buildName( func ), buildExec( func ) );
	}

	private Executable buildExec(final CommonSpatialFunction func) {
		return () -> {
			scope.inSession( session -> {
				String hql = templates.get( func );
				session.createQuery( hql )
						.setParameter( "poly", filterGeometry )
						.getResultList();
				//we just check that this parses for now.
			} );
		};
	}

	private String buildName(CommonSpatialFunction func) {
		return func.getKey().getName();
	}

	private boolean templateAvailable(CommonSpatialFunction f) {
		return templates.keySet().contains( f );
	}
}
