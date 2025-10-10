/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand.custom.oracle;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.orm.test.sql.hand.custom.CustomStoredProcTestSupport;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;

/**
 * Custom SQL tests for Oracle
 *
 * @author Gavin King
 */
@RequiresDialect(OracleDialect.class)
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/sql/hand/custom/oracle/Mappings.hbm.xml",
				"org/hibernate/orm/test/sql/hand/custom/oracle/StoredProcedures.hbm.xml"
		}
)
public class OracleCustomSQLTest extends CustomStoredProcTestSupport {
}
