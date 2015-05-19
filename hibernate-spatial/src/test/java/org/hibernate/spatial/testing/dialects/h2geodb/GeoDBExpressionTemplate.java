/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.h2geodb;

import org.hibernate.spatial.testing.SQLExpressionTemplate;
import org.hibernate.spatial.testing.TestDataElement;
import org.hibernate.spatial.testing.WktUtility;

/**
 * This is the template for insert SQL statements into the geomtest test table
 * for GeoDB.
 *
 * @author Jan Boonen, Geodan IT b.v.
 */
public class GeoDBExpressionTemplate implements SQLExpressionTemplate {

	static final String SQL_TEMPLATE = "insert into GEOMTEST (id, type, geom) values (%d, '%s', ST_GeomFromText('%s', %d))";

	/*
		  * (non-Javadoc)
		  *
		  * @seeorg.hibernatespatial.test.SQLExpressionTemplate#toInsertSql(org.
		  * hibernatespatial.test.TestDataElement)
		  */
	public String toInsertSql(TestDataElement testDataElement) {
		String wkt = WktUtility.getWkt( testDataElement.wkt );
		int srid = WktUtility.getSRID( testDataElement.wkt );
		return String
				.format( SQL_TEMPLATE,
						testDataElement.id,
						testDataElement.type,
						wkt,
						srid
				);
	}

}
