/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectContext;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

public class DialectSQLExceptionConversionTest {

	private final Dialect dialect = DialectContext.getDialect();

	@Test
	public void testSybaseDeadlock() {
		final var sqlException = new SQLException( "Deadlock victim", "S1000", 1205 );
		final String sql = "select version from Doctor where id = ?";
		final var converted = new SybaseASEDialect().buildSQLExceptionConversionDelegate()
				.convert( sqlException, "Could not acquire lock", sql );
		assertInstanceOf( LockAcquisitionException.class, converted );
		assertSame( sqlException, converted.getSQLException() );
		assertEquals( sql, converted.getSQL() );
	}

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
