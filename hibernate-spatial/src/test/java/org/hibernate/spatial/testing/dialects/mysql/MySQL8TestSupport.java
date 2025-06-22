/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.dialects.mysql;

import org.hibernate.spatial.testing.datareader.TestData;

/**
 * Created by Karel Maesen, Geovise BVBA on 2019-03-07.
 */
public class MySQL8TestSupport extends MySQLTestSupport {

	@Override
	public TestData createTestData(TestDataPurpose purpose) {
		return TestData.fromFile( "mysql/test-mysql8-functions-data-set.xml" );
	}
}
