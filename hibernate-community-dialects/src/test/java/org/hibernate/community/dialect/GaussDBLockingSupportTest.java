/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import jakarta.persistence.Timeout;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Timeouts;
import org.hibernate.community.dialect.lock.internal.GaussDBLockingSupport;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/// Tests for the GaussDB locking capability profile.
///
/// @author Steve Ebersole
class GaussDBLockingSupportTest {
	@Test
	void finiteWaitIsConnectionLevel() {
		final var metadata = GaussDBLockingSupport.LOCKING_SUPPORT.getMetadata();

		assertEquals( LockTimeoutType.CONNECTION, metadata.getLockTimeoutType( Timeout.seconds( 1 ) ) );
		assertFalse( metadata.supportsWait() );
	}

	@Test
	void rendersStructuredTargetsAndTimeouts() {
		final var renderer = GaussDBLockingSupport.LOCKING_SUPPORT.getLockingClauseRenderer();

		assertEquals(
				" for share of p,p.id skip locked",
				renderer.render( new LockingClauseRequest(
						PessimisticLockKind.SHARE,
						Timeouts.SKIP_LOCKED,
						List.of(
								new LockingClauseRequest.TableTarget( "p" ),
								new LockingClauseRequest.ColumnTarget( "p", "id" )
						)
				) )
		);
		assertEquals(
				" for update",
				renderer.render( new LockingClauseRequest(
						PessimisticLockKind.UPDATE,
						Timeout.seconds( 1 ),
						List.of()
				) )
		);
	}

	@Test
	void dialectStrategyUsesSuppliedRenderer() {
		final var strategy = new GaussDBDialect().getLockingClauseStrategy(
				new QuerySpec( true ),
				new LockOptions( LockMode.PESSIMISTIC_WRITE, Timeouts.NO_WAIT )
		);
		final StringBuilderSqlAppender appender = new StringBuilderSqlAppender();

		strategy.render( appender );

		assertEquals( " for update nowait", appender.toString() );
	}
}
