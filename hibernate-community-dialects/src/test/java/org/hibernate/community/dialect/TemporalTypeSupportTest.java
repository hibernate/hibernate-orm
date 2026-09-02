/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;
import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Verifies representative community and legacy temporal-type strategies.
///
/// @author Steve Ebersole
public class TemporalTypeSupportTest {
	@Test
	void legacyStrategiesPreserveSelectionAndSemantics() {
		final var h2 = new H2LegacyDialect();
		assertSame( h2, h2.getCurrentTemporalSupport() );
		assertSame( h2, h2.getTemporalFormatSupport() );
		assertSame( h2, h2.getTemporalOperationSupport() );
		assertEquals(
				CurrentTimestampSelection.prepared( "call current_timestamp()" ),
				h2.getCurrentTemporalSupport().getCurrentTimestampSelection()
		);
		assertSame( TemporalValueSemantics.OFFSET_LITERALS, h2.getTemporalValueSemantics() );

		final var db2 = new DB2LegacyDialect();
		assertEquals( "values current timestamp", db2.getCurrentTemporalSupport()
				.getCurrentTimestampSelection().command() );
		assertSame( TemporalValueSemantics.TRUNCATING, db2.getTemporalValueSemantics() );
	}

	@Test
	void versionedSemanticsAndUnsupportedSelectionRemainExplicit() {
		assertSame(
				TemporalValueSemantics.TRUNCATING,
				new FirebirdDialect( DatabaseVersion.make( 3 ) ).getTemporalValueSemantics()
		);
		assertSame(
				TemporalValueSemantics.TRUNCATING_WITH_OFFSET_LITERALS,
				new FirebirdDialect( DatabaseVersion.make( 4 ) ).getTemporalValueSemantics()
		);
		assertNull( new IngresDialect().getCurrentTemporalSupport().getCurrentTimestampSelection() );
	}
}
