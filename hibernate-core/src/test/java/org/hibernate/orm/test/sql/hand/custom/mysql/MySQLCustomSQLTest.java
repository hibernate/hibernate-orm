/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand.custom.mysql;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.community.dialect.TiDBDialect;
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
