/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Steve Ebersole
 */
@JiraKey(value = "HHH-15046")
@RequiresDialect(DB2iDialect.class)
@RequiresDialect(DB2zDialect.class)
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
