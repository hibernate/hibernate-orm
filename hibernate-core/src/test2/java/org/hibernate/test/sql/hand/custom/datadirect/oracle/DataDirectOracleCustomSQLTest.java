/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.hand.custom.datadirect.oracle;

import org.hibernate.dialect.DataDirectOracle9Dialect;
import org.hibernate.test.sql.hand.custom.CustomStoredProcTestSupport;
import org.hibernate.testing.RequiresDialect;

/**
 * Custom SQL tests for Oracle via the DataDirect drivers.
 * 
 * @author Max Rydahl Andersen
 */
@RequiresDialect( DataDirectOracle9Dialect.class )
public class DataDirectOracleCustomSQLTest extends CustomStoredProcTestSupport {
	public String[] getMappings() {
		return new String[] { "sql/hand/custom/oracle/Mappings.hbm.xml", "sql/hand/custom/datadirect/oracle/StoredProcedures.hbm.xml" };
	}
}

