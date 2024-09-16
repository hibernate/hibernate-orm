/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
