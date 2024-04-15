/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.hand.custom.sybase;

import org.hibernate.dialect.Sybase11Dialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.SybaseAnywhereDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.test.sql.hand.custom.CustomStoredProcTestSupport;
import org.hibernate.testing.RequiresDialect;
import org.junit.Ignore;

/**
 * Custom SQL tests for Sybase dialects
 * 
 * @author Gavin King
 */
@RequiresDialect( { SybaseDialect.class, SybaseASE15Dialect.class, Sybase11Dialect.class, SybaseAnywhereDialect.class })
@Ignore("The jTDS driver doesn't detect callable prepared statements and can't unwrap from a PreparedStatement to a CallableStatement")
public class SybaseCustomSQLTest extends CustomStoredProcTestSupport {
	public String[] getMappings() {
		return new String[] { "sql/hand/custom/sybase/Mappings.hbm.xml" };
	}
}

