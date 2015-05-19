/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.hand.custom.sqlserver;

import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.test.sql.hand.custom.CustomStoredProcTestSupport;

/**
 * Custom SQL tests for SQLServer
 *
 * @author Gail Badner
 */
@RequiresDialect( SQLServerDialect.class )
public class SQLServerCustomSQLTest extends CustomStoredProcTestSupport {
	public String[] getMappings() {
		return new String[] { "sql/hand/custom/sqlserver/Mappings.hbm.xml" };
	}
}
