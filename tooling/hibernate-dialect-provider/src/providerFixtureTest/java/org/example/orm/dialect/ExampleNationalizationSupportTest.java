/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.type.spi.NationalizationSupport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/// Verifies independent nationalization semantics in the standalone provider.
///
/// @author Steve Ebersole
public class ExampleNationalizationSupportTest {
	@Test
	void suppliesAProviderOwnedNondefaultCombination() {
		final ExampleDialect dialect = new ExampleDialect();
		assertEquals( NationalizationSupport.IMPLICIT, dialect.getNationalizationSupport() );
		assertFalse( dialect.supportsNationalizedMethods() );
	}
}
