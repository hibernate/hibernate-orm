/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geolatte.geom.GeometryEquality;

import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.GeomCodec;
import org.hibernate.spatial.testing.TestSupportFactories;
import org.hibernate.spatial.testing.datareader.TestData;
import org.hibernate.spatial.testing.datareader.TestDataElement;
import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;
import org.hibernate.spatial.testing.dialects.PredicateRegexes;
import org.hibernate.spatial.testing.domain.GeomEntityLike;

import org.hibernate.testing.orm.junit.DialectContext;

import org.geolatte.geom.Geometry;

import static org.hibernate.spatial.testing.datareader.TestSupport.TestDataPurpose.SpatialFunctionsData;
import static org.hibernate.spatial.testing.datareader.TestSupport.TestDataPurpose.StoreRetrieveData;

@Deprecated
public class SpatialTestDataProvider {
	protected final static String JTS = "jts";
	protected NativeSQLTemplates templates;
	protected final PredicateRegexes predicateRegexes;
	protected final Map<CommonSpatialFunction, String> hqlOverrides;
	protected final Geometry<?> filterGeometry;
	private final TestData funcTestData;
	protected TestData testData;
	protected GeomCodec codec;

	protected GeometryEquality geometryEquality;
	protected List<CommonSpatialFunction> exludeFromTest;

	public SpatialTestDataProvider() {
		try {
			TestSupport support = TestSupportFactories.instance().getTestSupportFactory( DialectContext.getDialect() );
			templates = support.templates();
			predicateRegexes = support.predicateRegexes();
			hqlOverrides = support.hqlOverrides();
			codec = support.codec();
			testData = support.createTestData( StoreRetrieveData );
			exludeFromTest = support.getExcludeFromTests();
			funcTestData = support.createTestData( SpatialFunctionsData );
			filterGeometry = support.getFilterGeometry();
			geometryEquality = support.getGeometryEquality();
		}
		catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException( e );
		}
	}

	protected <T extends GeomEntityLike<?>> List<T> entities(Class<T> clazz) {
		return entities( clazz, StoreRetrieveData );
	}

	protected <T extends GeomEntityLike<?>> List<T> entities(Class<T> clazz, TestSupport.TestDataPurpose purpose) {
		try {
			List<T> entities = new ArrayList<>();
			for ( TestDataElement testDataElement : purpose == StoreRetrieveData ? testData : funcTestData ) {
				T entity = clazz.getDeclaredConstructor().newInstance();
				entity.setGeomFromWkt( testDataElement.wkt );
				entity.setId( testDataElement.id );
				entity.setType( testDataElement.type );
				entities.add( entity );
			}
			return entities;
		}
		catch (Throwable ex) {
			throw new RuntimeException( ex );
		}
	}

}
