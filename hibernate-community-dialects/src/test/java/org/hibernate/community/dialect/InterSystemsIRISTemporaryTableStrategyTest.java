/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.temptable.spi.TemporaryTableKind;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.BeforeUseAction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the temporary-table behavior supplied by the IRIS Dialect.
///
/// @author Steve Ebersole
class InterSystemsIRISTemporaryTableStrategyTest {
	@Test
	void preservesGlobalTemporaryTableBehavior() {
		final var dialect = new InterSystemsIRISDialect();
		final var strategy = dialect.getGlobalTemporaryTableStrategy();

		assertNotNull( strategy );
		assertEquals( TemporaryTableKind.GLOBAL, strategy.getTemporaryTableKind() );
		assertEquals( "create global temporary table if not exists", strategy.getTemporaryTableCreateCommand() );
		assertNull( strategy.getTemporaryTableCreateOptions() );
		assertEquals( "drop table", strategy.getTemporaryTableDropCommand() );
		assertEquals( "delete from", strategy.getTemporaryTableTruncateCommand() );
		assertEquals( BeforeUseAction.CREATE, strategy.getTemporaryTableBeforeUseAction() );
		assertEquals( AfterUseAction.CLEAN, strategy.getTemporaryTableAfterUseAction() );
		assertTrue( strategy.supportsTemporaryTablePrimaryKey() );

		assertNull( dialect.getLocalTemporaryTableStrategy() );
		assertNotNull( dialect.getPersistentTemporaryTableStrategy() );
	}
}
