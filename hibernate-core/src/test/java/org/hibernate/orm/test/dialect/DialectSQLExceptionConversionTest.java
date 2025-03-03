/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectContext;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class DialectSQLExceptionConversionTest {

	private final Dialect dialect = DialectContext.getDialect();

	@Test
	@JiraKey(value = "HHH-15592")
	public void testExceptionConversionDoesntNPE() {
		final SQLExceptionConversionDelegate conversionDelegate = dialect.buildSQLExceptionConversionDelegate();
		Assumptions.assumeTrue( conversionDelegate != null );
		conversionDelegate.convert(
				new SQLException(),
				"test",
				"test"
		);
	}
}
