/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.hibernate.spatial.testing.dialects.h2geodb;

import java.io.IOException;
import java.sql.SQLException;


import org.hibernate.boot.spi.MetadataImplementor;
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
		catch ( SQLException e ) {
			throw new RuntimeException( e );
		}
		catch ( IOException e ) {
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
		if ( dataSourceUtils instanceof GeoDBDataSourceUtils) {
			return new GeoDBExpectationsFactory( (GeoDBDataSourceUtils) dataSourceUtils );
		} else {
			throw new IllegalArgumentException("Requires a GeoDBDataSourceUtils instance");
		}
	}

	public SQLExpressionTemplate getSQLExpressionTemplate() {
		return new GeoDBExpressionTemplate();
	}


}

