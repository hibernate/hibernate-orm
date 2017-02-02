/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.postgis;

import org.hibernate.spatial.testing.SQLExpressionTemplate;
import org.hibernate.spatial.testing.TestDataElement;

/**
 * The template for postgis insert SQL
 *
 * @Author Karel Maesen, Geovise BVBA
 */
public class PostgisExpressionTemplate implements SQLExpressionTemplate {

	static final String SQL_TEMPLATE = "insert into geomtest (id, type, geom) values (%d, '%s', ST_GeomFromText('%s'))";

	public String toInsertSql(TestDataElement testDataElement) {
		return String.format(
				SQL_TEMPLATE,
				testDataElement.id,
				testDataElement.type,
				testDataElement.wkt
		);
	}
}
