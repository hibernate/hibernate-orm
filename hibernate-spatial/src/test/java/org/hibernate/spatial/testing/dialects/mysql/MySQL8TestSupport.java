/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.testing.dialects.mysql;

import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.SQLExpressionTemplate;
import org.hibernate.spatial.testing.TestData;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * Created by Karel Maesen, Geovise BVBA on 2019-03-07.
 */
public class MySQL8TestSupport extends MySQLTestSupport {

	@Override
	public TestData createTestData(BaseCoreFunctionalTestCase testcase) {
		return TestData.fromFile( "mysql/test-mysql8-functions-data-set.xml" );
	}

	@Override
	public SQLExpressionTemplate getSQLExpressionTemplate() {
		return new MySQL8ExpressionTemplate();
	}

	@Override
	public AbstractExpectationsFactory createExpectationsFactory(DataSourceUtils dataSourceUtils) {
		return new MySQL8ExpectationsFactory( dataSourceUtils );
	}
}
