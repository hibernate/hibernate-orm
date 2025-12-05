/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand.custom.mysql;

import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.orm.test.sql.hand.custom.CustomStoredProcTestSupport;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SkipForDialect;

/**
 * Custom SQL tests for MySQL
 *
 * @author Gavin King
 */
@RequiresDialect(MySQLDialect.class)
@SkipForDialect(dialectClass = TiDBDialect.class, reason = "TiDB doesn't support stored procedures",
		matchSubTypes = true)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/sql/hand/custom/mysql/Mappings.hbm.xml"
)
public class MySQLCustomSQLTest extends CustomStoredProcTestSupport {
}
