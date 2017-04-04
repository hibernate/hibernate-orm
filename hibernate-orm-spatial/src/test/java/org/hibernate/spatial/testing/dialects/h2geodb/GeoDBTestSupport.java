/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.h2geodb;

import java.io.IOException;
import java.sql.SQLException;

import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.GeometryEquality;
import org.hibernate.spatial.testing.SQLExpressionTemplate;
import org.hibernate.spatial.testing.TestData;
import org.hibernate.spatial.testing.TestSupport;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Oct 2, 2010
 */
public class GeoDBTestSupport extends TestSupport {

	public DataSourceUtils createDataSourceUtil(ServiceRegistry serviceRegistry) {
		super.createDataSourceUtil( serviceRegistry );
		try {
			return new GeoDBDataSourceUtils( driver(), url(), user(), passwd(), getSQLExpressionTemplate() );
		}
		catch (SQLException e) {
			throw new RuntimeException( e );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	public TestData createTestData(BaseCoreFunctionalTestCase testcase) {
		return TestData.fromFile( "h2geodb/test-geodb-data-set.xml" );
	}

	public GeometryEquality createGeometryEquality() {
		return new GeoDBGeometryEquality();
	}

	public AbstractExpectationsFactory createExpectationsFactory(DataSourceUtils dataSourceUtils) {
		if ( dataSourceUtils instanceof GeoDBDataSourceUtils ) {
			return new GeoDBExpectationsFactory( (GeoDBDataSourceUtils) dataSourceUtils );
		}
		else {
			throw new IllegalArgumentException( "Requires a GeoDBDataSourceUtils instance" );
		}
	}

	public SQLExpressionTemplate getSQLExpressionTemplate() {
		return new GeoDBExpressionTemplate();
	}

}
