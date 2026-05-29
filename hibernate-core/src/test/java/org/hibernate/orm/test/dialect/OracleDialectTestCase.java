/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.JDBCException;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.exception.TransactionSerializationException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test case for Oracle specific things.
 */
@RequiresDialect(OracleDialect.class)
public class OracleDialectTestCase {

	@Test
	@JiraKey(value = "HHH-12235")
	public void testSerializationFailureConversion() {
		final OracleDialect dialect = new OracleDialect();
		final SQLExceptionConversionDelegate delegate = dialect.buildSQLExceptionConversionDelegate();
		assertNotNull( delegate );

		// ORA-08177: can't serialize access for this transaction
		final JDBCException exception = delegate.convert(
				new SQLException( "ORA-08177: can't serialize access for this transaction", "72000", 8177 ),
				"",
				""
		);
		assertInstanceOf( TransactionSerializationException.class, exception );
	}
}
