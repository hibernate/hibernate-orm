/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.integration.functions;

import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.GeometryEquality;

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
			GeometryEquality geomEq,
			Geometry<?> filter) {

		Map<CommonSpatialFunction, String> templates = sqlTemplates.all();
		return templates
				.keySet()
				.stream()
				.map( function -> builder( function )
						.hql( hqlOverrides.get( function ) )
						.sql( templates.get( function ) )
						.equalityTest( geomEq )
						.geometry( setFilter( function ) ? filter : null ) );

	}

	private static boolean setFilter(CommonSpatialFunction function) {
		return function.getType() == CommonSpatialFunction.Type.ANALYSIS ||
				function.getType() == CommonSpatialFunction.Type.OVERLAY ||
				function == CommonSpatialFunction.ST_DISTANCE;
	}

}
