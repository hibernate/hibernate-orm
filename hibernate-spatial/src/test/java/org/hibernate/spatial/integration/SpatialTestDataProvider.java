/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.integration;

import java.sql.BatchUpdateException;
import java.sql.SQLException;

import javax.persistence.Query;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.JTSGeometryEquality;
import org.hibernate.spatial.testing.TestSupportFactories;
import org.hibernate.spatial.testing.datareader.TestData;
import org.hibernate.spatial.testing.datareader.TestSupport;

import org.hibernate.testing.orm.junit.DialectContext;

public class SpatialTestDataProvider {
	protected final static String JTS = "jts";

	protected TestData testData;
	protected JTSGeometryEquality geometryEquality;
	protected AbstractExpectationsFactory expectationsFactory;


	public SpatialTestDataProvider() {
		try {
			TestSupport support = TestSupportFactories.instance().getTestSupportFactory( DialectContext.getDialect() );
			testData = support.createTestData( TestSupport.TestDataPurpose.StoreRetrieveData );
			geometryEquality = support.createGeometryEquality();
		}
		catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * Inserts the test data via a direct route (JDBC).
	 */
	public void prepareTest(SessionImplementor session) {
		throw new NotYetImplementedException();
	}




	protected String entityName(String pckg) {
		if ( JTS.equalsIgnoreCase( pckg ) ) {
			return "org.hibernate.spatial.testing.domain.JtsGeomEntity";
		}
		else {
			return "org.hibernate.spatial.testing.domain.GeomEntity";
		}
	}
}
