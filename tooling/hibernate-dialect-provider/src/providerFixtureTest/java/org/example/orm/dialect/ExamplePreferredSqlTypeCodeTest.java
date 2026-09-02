/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.type.SqlTypes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies preferred type codes supplied by the standalone provider.
///
/// @author Steve Ebersole
public class ExamplePreferredSqlTypeCodeTest {
	@Test
	void suppliesProviderOwnedNondefaultTypeCodes() {
		final ExampleDialect dialect = new ExampleDialect();
		assertEquals( SqlTypes.JSON_ARRAY, dialect.getPreferredSqlTypeCodeForArray() );
		assertEquals( SqlTypes.SMALLINT, dialect.getPreferredSqlTypeCodeForBoolean() );
	}
}
