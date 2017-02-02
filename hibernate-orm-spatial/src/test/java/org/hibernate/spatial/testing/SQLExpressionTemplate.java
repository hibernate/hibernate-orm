/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing;

/**
 * <code>SQLExpressoinTemplate</code>s generate database-specific
 * SQL statements for a given <code>TestDataElement</code> instance.
 *
 * @author Karel Maesen, Geovise BVBA
 */
public interface SQLExpressionTemplate {

	/**
	 * Returns an insert SQL statement for the specified <code>TestDataElement</code>
	 *
	 * @param testDataElement
	 *
	 * @return an insert SQL for testDataElement
	 */
	public String toInsertSql(TestDataElement testDataElement);

}
