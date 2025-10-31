/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand.custom.db2;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.orm.test.sql.hand.custom.CustomStoredProcTestSupport;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;

/**
 * Custom SQL tests for DB2
 *
 * @author Max Rydahl Andersen
 */
@RequiresDialect(DB2Dialect.class)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/sql/hand/custom/db2/Mappings.hbm.xml"
)
public class DB2CustomSQLTest extends CustomStoredProcTestSupport {
}
