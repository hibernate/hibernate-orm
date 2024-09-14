/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.sql.hand.custom.mysql;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.TiDBDialect;
import org.hibernate.orm.test.sql.hand.custom.CustomStoredProcTestSupport;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.SkipForDialect;

/**
 * Custom SQL tests for MySQL
 *
 * @author Gavin King
 */
@RequiresDialect( MySQLDialect.class )
@SkipForDialect(value = TiDBDialect.class, comment = "TiDB doesn't support stored procedures")
public class MySQLCustomSQLTest extends CustomStoredProcTestSupport {
	public String[] getMappings() {
		return new String[] { "sql/hand/custom/mysql/Mappings.hbm.xml" };
	}
}
