/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.mysql;


import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.JTSGeometryEquality;
import org.hibernate.spatial.testing.datareader.TestData;
import org.hibernate.spatial.testing.datareader.TestSupport;

/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: Oct 18, 2010
 */
public class MySQLTestSupport extends TestSupport {

	@Override
	public TestData createTestData(TestDataPurpose purpose) {
		return TestData.fromFile( "mysql/test-mysql-functions-data-set.xml" );
	}

}
