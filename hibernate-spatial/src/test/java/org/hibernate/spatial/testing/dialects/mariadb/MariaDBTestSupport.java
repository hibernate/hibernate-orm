/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.mariadb;

import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.SQLExpressionTemplate;
import org.hibernate.spatial.testing.TestData;
import org.hibernate.spatial.testing.TestSupport;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class MariaDBTestSupport extends TestSupport  {
	@Override
	public TestData createTestData(BaseCoreFunctionalTestCase testcase) {
		return TestData.fromFile( "mariadb/test-mariadb-functions-data-set.xml" );

	}

	@Override
	public AbstractExpectationsFactory createExpectationsFactory(DataSourceUtils dataSourceUtils) {
		return new MariaDBExpectationsFactory( dataSourceUtils );
	}

	@Override
	public SQLExpressionTemplate getSQLExpressionTemplate() {
		return new MariaDBExpressionTemplate();
	}
}
