/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.locking;

import java.util.List;

import jakarta.persistence.Timeout;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Timeouts;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.internal.DB2LockingSupport;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests the DB2-family locking-clause renderer profiles.
///
/// @author Steve Ebersole
class DB2LockingClauseRendererTest {
	@Test
	void luwVersionGatesSkipLockedData() {
		final var request = new LockingClauseRequest(
				PessimisticLockKind.SHARE,
				Timeouts.SKIP_LOCKED,
				List.of()
		);
		assertEquals(
				" for read only with rs use and keep share locks",
				DB2LockingSupport.forDB2( false ).render( request )
		);
		assertEquals(
				" for read only with rs use and keep share locks skip locked data",
				DB2LockingSupport.forDB2( true ).render( request )
		);
	}

	@Test
	void db2iUsesUpdateWithRsForBothLockKinds() {
		assertEquals(
				" for update with rs skip locked data",
				DB2LockingSupport.forDB2i().render( new LockingClauseRequest(
						PessimisticLockKind.SHARE,
						Timeouts.SKIP_LOCKED,
						List.of()
				) )
		);
	}

	@Test
	void db2zPreservesInheritedClauseAndIgnoresColumnTargets() {
		assertEquals(
				" for read only with rs use and keep update locks skip locked data",
				DB2LockingSupport.forDB2z().render( new LockingClauseRequest(
						PessimisticLockKind.UPDATE,
						Timeouts.SKIP_LOCKED,
						List.of( new LockingClauseRequest.ColumnTarget( "p", "id" ) )
				) )
		);
	}

	@Test
	void currentDialectStrategiesUseSuppliedProfiles() {
		assertDialectRenders(
				new DB2Dialect( DatabaseVersion.make( 11, 5 ) ),
				LockMode.PESSIMISTIC_READ,
				Timeouts.SKIP_LOCKED,
				" for read only with rs use and keep share locks skip locked data"
		);
		assertDialectRenders(
				new DB2iDialect(),
				LockMode.PESSIMISTIC_READ,
				Timeouts.SKIP_LOCKED,
				" for update with rs skip locked data"
		);
		assertDialectRenders(
				new DB2zDialect(),
				LockMode.PESSIMISTIC_WRITE,
				Timeouts.SKIP_LOCKED,
				" for read only with rs use and keep update locks skip locked data"
		);
	}

	private static void assertDialectRenders(
			Dialect dialect,
			LockMode lockMode,
			Timeout timeout,
			String expected) {
		final var strategy = dialect.getLockingClauseStrategy(
				new QuerySpec( true ),
				new LockOptions( lockMode, timeout )
		);
		final StringBuilderSqlAppender appender = new StringBuilderSqlAppender();

		strategy.render( appender );

		assertEquals( expected, appender.toString() );
	}
}
