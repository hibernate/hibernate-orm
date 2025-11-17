/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.datareader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.GeomCodec;
import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;
import org.hibernate.spatial.testing.dialects.PredicateRegexes;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.GeometryEquality;
import org.geolatte.geom.GeometryPositionEquality;

import static org.geolatte.geom.builder.DSL.g;
import static org.geolatte.geom.builder.DSL.polygon;
import static org.geolatte.geom.builder.DSL.ring;
import static org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84;


/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: Sep 30, 2010
 */
public abstract class TestSupport {

	//TODO -- make this abstract
	public NativeSQLTemplates templates() {
		return null;
	}

	//TODO -- make this abstract
	public PredicateRegexes predicateRegexes() {
		return null;
	}

	public Map<CommonSpatialFunction, String> hqlOverrides() {
		return new HashMap<>();
	}

	public List<CommonSpatialFunction> getExcludeFromTests() {
		return new ArrayList<>();
	}

	public GeometryEquality getGeometryEquality() {
		return new GeometryPositionEquality();
	}

	public enum TestDataPurpose {
		SpatialFunctionsData,
		StoreRetrieveData
	}

	public abstract TestData createTestData(TestDataPurpose purpose);

	public GeomCodec codec() {
		throw new UnsupportedOperationException();
	}

	public Geometry<?> getFilterGeometry() {
		return polygon(
				WGS84,
				ring( g( 0, 0 ), g( 0, 10 ), g( 10, 10 ), g( 10, 0 ), g( 0, 0 ) )
		);
	}

}
