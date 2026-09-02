/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies community SQLException conversion and constraint extraction.
///
/// @author Steve Ebersole
public class SQLExceptionSupportTest {
	@Test
	void communityDelegateMayConvertOrDeclineAndExtractorMayNameOrDecline() {
		final DerbyDialect dialect = new DerbyDialect();
		final SQLException violation = new SQLException( "Duplicate key 'uk_fixture'", "23505" );

		assertThat( dialect.buildSQLExceptionConversionDelegate().convert( violation, "constraint", "insert" ) )
				.isInstanceOf( ConstraintViolationException.class );
		assertThat( dialect.getViolatedConstraintNameExtractor().extractConstraintName( violation ) )
				.isEqualTo( "uk_fixture" );
		assertThat( dialect.buildSQLExceptionConversionDelegate()
				.convert( new SQLException( "unknown", "ZZ999" ), "unknown", "select" ) ).isNull();
		assertThat( dialect.getViolatedConstraintNameExtractor()
				.extractConstraintName( new SQLException( "unknown", "ZZ999" ) ) ).isNull();
	}
}
