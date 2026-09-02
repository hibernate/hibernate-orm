/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.sql.SQLException;

import org.hibernate.JDBCException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies provider-owned SQLException policies through the standalone fixture.
///
/// @author Steve Ebersole
public class ExampleSQLExceptionSupportTest {
	@Test
	void providerDelegateConvertsOrDeclines() {
		final ExampleDialect dialect = new ExampleDialect();
		final SQLException handled = new SQLException( "handled", "HY000", 60_003 );
		final JDBCException converted = dialect.buildSQLExceptionConversionDelegate()
				.convert( handled, "fixture", "select fixture" );

		assertSame( handled, converted.getSQLException() );
		assertEquals( "select fixture", converted.getSQL() );
		assertNull( dialect.buildSQLExceptionConversionDelegate()
				.convert( new SQLException( "unknown", "HY000", 1 ), "fixture", "select fixture" ) );
	}

	@Test
	void providerExtractorAndRollbackPolicyPreserveBothOutcomes() {
		final ExampleDialect dialect = new ExampleDialect();
		assertEquals(
				"fixture_constraint",
				dialect.getViolatedConstraintNameExtractor()
						.extractConstraintName( new SQLException( "constraint", "23000", 60_004 ) )
		);
		assertNull( dialect.getViolatedConstraintNameExtractor()
				.extractConstraintName( new SQLException( "unknown", "23000", 1 ) ) );
		assertTrue( dialect.causesRollback( new SQLException( "serialization", "40001" ) ) );
		assertFalse( dialect.causesRollback( new SQLException( "ordinary", "42000" ) ) );
	}

}
