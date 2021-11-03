/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.integration.functions;

import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;

import org.geolatte.geom.G2D;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.Polygon;

import static org.geolatte.geom.builder.DSL.g;
import static org.geolatte.geom.builder.DSL.polygon;
import static org.geolatte.geom.builder.DSL.ring;
import static org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84;

/**
 * Makes available all the builders for FunctionTestTemplate
 */
public abstract class TestTemplates {

	static FunctionTestTemplate.Builder builder(CommonSpatialFunction function) {
		return new FunctionTestTemplate.Builder( function );
	}


	public static Stream<FunctionTestTemplate.Builder> all(
			NativeSQLTemplates sqlTemplates,
			Map<CommonSpatialFunction, String> hqlOverrides,
			Geometry<?>filter) {

		Map<CommonSpatialFunction, String> templates = sqlTemplates.all();
		return templates
				.keySet()
				.stream()
				.map( function -> builder( function )
						.hql( hqlOverrides.get( function ) )
						.sql( templates.get( function ) )
						.geometry( setFilter( function ) ? filter : null ) );

	}

	private static boolean setFilter(CommonSpatialFunction function) {
		return function.getType() == CommonSpatialFunction.Type.ANALYSIS ||
				function.getType() == CommonSpatialFunction.Type.OVERLAY ||
				function == CommonSpatialFunction.ST_DISTANCE;
	}

}
