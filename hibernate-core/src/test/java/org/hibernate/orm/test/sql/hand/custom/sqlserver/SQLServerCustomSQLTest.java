/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand.custom.sqlserver;

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.orm.test.sql.hand.custom.CustomStoredProcTestSupport;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;

/**
 * Custom SQL tests for SQLServer
 *
 * @author Gail Badner
 */
@RequiresDialect(SQLServerDialect.class)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/sql/hand/custom/sqlserver/Mappings.hbm.xml"
)
public class SQLServerCustomSQLTest extends CustomStoredProcTestSupport {
}
