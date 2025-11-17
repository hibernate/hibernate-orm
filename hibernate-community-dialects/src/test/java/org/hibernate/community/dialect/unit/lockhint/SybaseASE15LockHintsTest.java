/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.unit.lockhint;

import org.hibernate.community.dialect.SybaseASELegacyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.orm.test.dialect.unit.lockhint.AbstractLockHintTest;

/**
 * @author Gail Badner
 */
public class SybaseASE15LockHintsTest extends AbstractLockHintTest {
	public static final Dialect DIALECT = new SybaseASELegacyDialect();

	protected String getLockHintUsed() {
		return "holdlock";
	}

	protected Dialect getDialectUnderTest() {
		return DIALECT;
	}
}
