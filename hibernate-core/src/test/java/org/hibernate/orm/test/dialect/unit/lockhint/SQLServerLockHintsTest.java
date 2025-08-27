/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.lockhint;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(SQLServerDialect.class)
public class SQLServerLockHintsTest extends AbstractLockHintTest {
	public static final Dialect DIALECT = new SQLServerDialect();

	protected String getLockHintUsed() {
		return "with (updlock,holdlock,rowlock)";
	}

	protected Dialect getDialectUnderTest() {
		return DIALECT;
	}
}
