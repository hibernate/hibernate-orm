/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.orm.test.sql.hand.custom.CustomStoredProcTestSupport;

import org.hibernate.testing.orm.junit.RequiresDialect;


/**
 * @author Andrea Boriero
 */
@RequiresDialect(DerbyDialect.class)
public class DerbyCustomSQLTest extends CustomStoredProcTestSupport {
	public String[] getMappings() {
		return new String[] {"derby/Mappings.hbm.xml"};
	}
}
