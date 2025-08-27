/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.check;

import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.RequiresDialect;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( value = OracleDialect.class )
public class OracleCheckStyleTest extends ResultCheckStyleTest {
	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public String[] getMappings() {
		return new String[] { "sql/check/oracle-mappings.hbm.xml" };
	}
}
