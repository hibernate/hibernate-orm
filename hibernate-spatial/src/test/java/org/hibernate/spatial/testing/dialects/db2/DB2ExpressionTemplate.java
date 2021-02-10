/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.testing.dialects.db2;

import org.hibernate.spatial.testing.SQLExpressionTemplate;
import org.hibernate.spatial.testing.TestDataElement;
import org.hibernate.spatial.testing.WktUtility;

/**
 * @author David Adler
 * creation-date: 5/22/2014
 */
public class DB2ExpressionTemplate implements SQLExpressionTemplate {

	final String SQL_TEMPLATE = "insert into geomtest (id, type, geom) values (%d, '%s', DB2GSE.ST_GEOMETRY('%s', %d))";

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
