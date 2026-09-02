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
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.lock.internal.H2LockingSupport;
import org.hibernate.dialect.lock.internal.HSQLLockingSupport;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests the H2 and HSQLDB locking-clause renderer profiles.
///
/// @author Steve Ebersole
class H2HSQLLockingClauseRendererTest {
	@Test
	void currentH2RendersAllQueryLevelTimeoutForms() {
		assertEquals(
				" for update nowait",
				H2LockingSupport.INSTANCE.render( request( PessimisticLockKind.UPDATE, Timeouts.NO_WAIT ) )
		);
		assertEquals(
				" for update skip locked",
				H2LockingSupport.INSTANCE.render( request( PessimisticLockKind.SHARE, Timeouts.SKIP_LOCKED ) )
		);
		assertEquals(
				" for update wait 2",
				H2LockingSupport.INSTANCE.render( request( PessimisticLockKind.UPDATE, Timeout.milliseconds( 1_500 ) ) )
		);
		assertEquals(
				LockTimeoutType.QUERY,
				H2LockingSupport.INSTANCE.getLockTimeoutType( Timeout.seconds( 1 ) )
		);
	}

	@Test
	void legacyH2OmitsUnsupportedTimeoutForms() {
		assertEquals(
				" for update",
				H2LockingSupport.LEGACY_INSTANCE.render( request( PessimisticLockKind.UPDATE, Timeouts.NO_WAIT ) )
		);
		assertEquals(
				" for update",
				H2LockingSupport.LEGACY_INSTANCE.render( request( PessimisticLockKind.UPDATE, Timeout.seconds( 1 ) ) )
		);
	}

	@Test
	void hsqldbProfilesRenderOnlyTheBaseClause() {
		assertEquals(
				" for update",
				HSQLLockingSupport.LOCKING_SUPPORT.render(
						request( PessimisticLockKind.SHARE, Timeouts.SKIP_LOCKED )
				)
		);
		assertEquals(
				"",
				HSQLLockingSupport.NO_CLAUSE_LOCKING_SUPPORT.render(
						request( PessimisticLockKind.UPDATE, Timeouts.WAIT_FOREVER )
				)
		);
	}

	@Test
	void currentDialectStrategiesUseSuppliedProfiles() {
		assertDialectRenders(
				new H2Dialect( DatabaseVersion.make( 2, 2, 220 ) ),
				LockMode.PESSIMISTIC_WRITE,
				Timeout.seconds( 2 ),
				" for update wait 2"
		);
		assertDialectRenders(
				new HSQLDialect(),
				LockMode.PESSIMISTIC_READ,
				Timeouts.SKIP_LOCKED,
				" for update"
		);
	}

	private static LockingClauseRequest request(PessimisticLockKind lockKind, Timeout timeout) {
		return new LockingClauseRequest( lockKind, timeout, List.of() );
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
