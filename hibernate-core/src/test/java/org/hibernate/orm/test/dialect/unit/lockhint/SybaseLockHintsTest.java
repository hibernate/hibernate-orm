/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.lockhint;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(SybaseDialect.class)
public class SybaseLockHintsTest extends AbstractLockHintTest {
	public static final Dialect DIALECT = new SybaseDialect();

	protected String getLockHintUsed() {
		return "holdlock";
	}

	protected Dialect getDialectUnderTest() {
		return DIALECT;
	}
}
