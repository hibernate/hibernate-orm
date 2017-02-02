/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.hand.custom.oracle;

import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.test.sql.hand.custom.CustomStoredProcTestSupport;
import org.hibernate.testing.RequiresDialect;

/**
 * Custom SQL tests for Oracle
 * 
 * @author Gavin King
 */
@RequiresDialect( Oracle9iDialect.class )
public class OracleCustomSQLTest extends CustomStoredProcTestSupport {
	public String[] getMappings() {
		return new String[] { "sql/hand/custom/oracle/Mappings.hbm.xml", "sql/hand/custom/oracle/StoredProcedures.hbm.xml" };
	}
}

