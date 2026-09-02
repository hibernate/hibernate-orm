/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Verifies the standalone provider's custom sequence grammar.
///
/// @author Steve Ebersole
public class ExampleSequenceSupportTest {
	@Test
	void suppliesTheProviderOwnedStrategy() {
		assertSame( ExampleSequenceSupport.INSTANCE, new ExampleDialect().getSequenceSupport() );
	}

	@Test
	void rendersTheProviderGrammar() {
		final ExampleSequenceSupport support = ExampleSequenceSupport.INSTANCE;
		assertEquals( "fixture_next('seq')", support.getSelectSequenceNextValString( "seq" ) );
		assertEquals( "fixture_current('seq')", support.getSelectSequencePreviousValString( "seq" ) );
		assertEquals( "select fixture_next('seq')", support.getSequenceNextValString( "seq" ) );
		assertEquals(
				"create fixture sequence seq start 3 step -4",
				support.getCreateSequenceString( "seq", 3, -4 )
		);
		assertEquals( "drop fixture sequence seq", support.getDropSequenceString( "seq" ) );
	}
}
