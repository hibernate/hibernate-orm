/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.testing.dialects.db2;

import org.hibernate.spatial.testing.datareader.TestData;
import org.hibernate.spatial.testing.datareader.TestSupport;

/**
 * @author David Adler, Adtech Geospatial
 * creation-date: 5/22/2014
 */
public class DB2TestSupport extends TestSupport {

	public TestData createTestData(TestDataPurpose purpose) {
		switch ( purpose ) {
			case SpatialFunctionsData:
				return TestData.fromFile( "db2/test-db2nozm-only-polygon.xml" );
			default:
				return TestData.fromFile( "db2/test-db2nozm-data-set.xml" );
		}
	}


	public DB2ExpectationsFactory createExpectationsFactory() {
		return new DB2ExpectationsFactory();
	}

}
