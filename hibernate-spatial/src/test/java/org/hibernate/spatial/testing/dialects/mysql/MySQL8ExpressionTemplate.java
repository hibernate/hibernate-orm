/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.testing.dialects.mysql;

import org.hibernate.spatial.testing.TestDataElement;
import org.hibernate.spatial.testing.WktUtility;

/**
 * Created by Karel Maesen, Geovise BVBA on 2019-03-07.
 */
public class MySQL8ExpressionTemplate extends MySQLExpressionTemplate {

	static final String SQL_TEMPLATE = "insert into geomtest (id, type, geom) values (%d, '%s', ST_GeomFromText('%s', %d))";

	public String toInsertSql(TestDataElement testDataElement) {
		String wkt = WktUtility.getWkt( testDataElement.wkt );
		int srid = WktUtility.getSRID( testDataElement.wkt );
		return String.format(
				SQL_TEMPLATE,
				testDataElement.id,
				testDataElement.type,
				wkt,
				srid
		);
	}
}
