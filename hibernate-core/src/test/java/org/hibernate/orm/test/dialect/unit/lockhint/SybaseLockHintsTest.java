/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.lockhint;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseDialect;

/**
 * @author Steve Ebersole
 */
public class SybaseLockHintsTest extends AbstractLockHintTest {
	public static final Dialect DIALECT = new SybaseDialect();

	protected String getLockHintUsed() {
		return "holdlock";
	}

	protected Dialect getDialectUnderTest() {
		return DIALECT;
	}
}
