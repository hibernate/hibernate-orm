/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.GeomCodec;
import org.hibernate.spatial.testing.TestSupportFactories;
import org.hibernate.spatial.testing.datareader.TestData;
import org.hibernate.spatial.testing.datareader.TestDataElement;
import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;
import org.hibernate.spatial.testing.domain.GeomEntityLike;

import org.hibernate.testing.orm.junit.DialectContext;

import static org.hibernate.spatial.testing.datareader.TestSupport.TestDataPurpose.SpatialFunctionsData;
import static org.hibernate.spatial.testing.datareader.TestSupport.TestDataPurpose.StoreRetrieveData;

@Deprecated
public class SpatialTestDataProvider {
	protected final static String JTS = "jts";
	protected final NativeSQLTemplates templates;
	protected final Map<CommonSpatialFunction, String> hqlOverrides;
	private final TestData funcTestData;
	protected TestData testData;
	protected GeomCodec codec;

	public SpatialTestDataProvider() {
		try {
			TestSupport support = TestSupportFactories.instance().getTestSupportFactory( DialectContext.getDialect() );
			templates = support.templates();
			hqlOverrides = support.hqlOverrides();
			codec = support.codec();
			testData = support.createTestData( StoreRetrieveData );
			funcTestData = support.createTestData( SpatialFunctionsData );
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
