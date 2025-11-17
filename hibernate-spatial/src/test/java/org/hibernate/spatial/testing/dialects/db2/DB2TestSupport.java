/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
