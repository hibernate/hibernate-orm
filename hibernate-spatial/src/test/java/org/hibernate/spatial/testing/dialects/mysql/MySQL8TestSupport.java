/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
