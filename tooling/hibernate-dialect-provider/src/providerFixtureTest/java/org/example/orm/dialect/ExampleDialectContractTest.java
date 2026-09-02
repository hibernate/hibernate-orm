/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.testing.DialectTestContext;
import org.hibernate.dialect.testing.DialectTestKit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// Exercises the generic contract suite and one provider-specific assertion.
///
/// @author Steve Ebersole
public class ExampleDialectContractTest {
	// tag::provider-assertion[]
	@Test
	void providerSpecificLiteralRendering() {
		try ( DialectTestContext context = DialectTestKit.openContext(
				new ExampleDialectContractProfile() ) ) {
			assertTrue( context.translate(
					"select e.id from ContractEntity e where e.name = 'fixture'"
			).statements().get( 0 ).sql().contains( "fixture('fixture')" ) );
		}
	}
	// end::provider-assertion[]
}
