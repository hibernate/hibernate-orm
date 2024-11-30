/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand.custom.oracle;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.orm.test.sql.hand.custom.CustomStoredProcTestSupport;

import org.hibernate.testing.RequiresDialect;

/**
 * Custom SQL tests for Oracle
 *
 * @author Gavin King
 */
@RequiresDialect( OracleDialect.class )
public class OracleCustomSQLTest extends CustomStoredProcTestSupport {
	public String[] getMappings() {
		return new String[] { "sql/hand/custom/oracle/Mappings.hbm.xml", "sql/hand/custom/oracle/StoredProcedures.hbm.xml" };
	}
}
