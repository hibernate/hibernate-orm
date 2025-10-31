/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand.custom.sybase;

import org.hibernate.dialect.SybaseDialect;
import org.hibernate.orm.test.sql.hand.custom.CustomStoredProcTestSupport;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;

/**
 * Custom SQL tests for Sybase dialects
 *
 * @author Gavin King
 */
@RequiresDialect(SybaseDialect.class)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/sql/hand/custom/sybase/Mappings.hbm.xml"
)
public class SybaseCustomSQLTest extends CustomStoredProcTestSupport {
}
