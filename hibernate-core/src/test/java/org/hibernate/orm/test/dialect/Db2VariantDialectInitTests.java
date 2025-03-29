/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
@JiraKey(value = "HHH-15046")
public class Db2VariantDialectInitTests {
	@Test
	public void testDB2zDialectInit() {
		final var db2zDialect = new DB2zDialect();
		assertNotNull( db2zDialect );
	}

	@Test
	public void testDB2iDialectInit() {
		final var db2iDialect = new DB2iDialect();
		assertNotNull( db2iDialect );
	}
}
