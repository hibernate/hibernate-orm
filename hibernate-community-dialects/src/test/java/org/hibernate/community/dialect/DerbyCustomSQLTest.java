/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.orm.test.sql.hand.custom.CustomStoredProcTestSupport;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;


/**
 * @author Andrea Boriero
 */
@RequiresDialect(DerbyDialect.class)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/derby/Mappings.hbm.xml"
)
public class DerbyCustomSQLTest extends CustomStoredProcTestSupport {
}
