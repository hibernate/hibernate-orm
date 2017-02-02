/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.oracle;

import org.hibernate.spatial.testing.SQLExpressionTemplate;
import org.hibernate.spatial.testing.TestDataElement;

public class SDOGeometryExpressionTemplate implements SQLExpressionTemplate {

	static final String SQL_TEMPLATE = "insert into geomtest (id, type, geom) values (%d, '%s', %s)";

	@SuppressWarnings( "unchecked" )
	public String toInsertSql(TestDataElement testDataElement) {
		if(! (testDataElement instanceof SDOTestDataElement)) {
			throw new IllegalArgumentException( "Require SDOTestDataElements" );
		}
		SDOTestDataElement sdoTestDataElement = (SDOTestDataElement) testDataElement;
		return String.format( SQL_TEMPLATE, sdoTestDataElement.id, sdoTestDataElement.type, sdoTestDataElement.sdo );
	}
}
