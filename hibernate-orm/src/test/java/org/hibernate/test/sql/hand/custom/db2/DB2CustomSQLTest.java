/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.hand.custom.db2;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;

import org.hibernate.test.sql.hand.custom.CustomStoredProcTestSupport;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.SkipForDialect;

/**
 * Custom SQL tests for DB2
 * 
 * @author Max Rydahl Andersen
 */
@RequiresDialect( DB2Dialect.class )
@SkipForDialect( DerbyDialect.class )
public class DB2CustomSQLTest extends CustomStoredProcTestSupport {
	public String[] getMappings() {
		return new String[] { "sql/hand/custom/db2/Mappings.hbm.xml" };
	}
}

