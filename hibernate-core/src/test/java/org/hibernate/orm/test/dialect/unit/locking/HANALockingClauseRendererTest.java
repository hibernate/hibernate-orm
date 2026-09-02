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
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.lock.internal.HANALockingSupport;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests the HANA-family locking-clause renderer.
///
/// @author Steve Ebersole
class HANALockingClauseRendererTest {
	@Test
	void rendersQualifiedColumnTargetsAndIgnoreLocked() {
		assertEquals(
				" for update of p.id,p.tenant_id ignore locked",
				HANALockingSupport.HANA_LOCKING_SUPPORT.render( new LockingClauseRequest(
						PessimisticLockKind.UPDATE,
						Timeouts.SKIP_LOCKED,
						List.of(
								new LockingClauseRequest.ColumnTarget( "p", "id" ),
								new LockingClauseRequest.ColumnTarget( "p", "tenant_id" )
						)
				) )
		);
	}

	@Test
	void pessimisticReadUsesForUpdateSyntax() {
		assertEquals(
				" for update wait 2",
				HANALockingSupport.HANA_LOCKING_SUPPORT.render( new LockingClauseRequest(
						PessimisticLockKind.SHARE,
						Timeout.milliseconds( 1_500 ),
						List.of()
				) )
		);
	}

	@Test
	void dialectStrategyUsesSuppliedRenderer() {
		final var strategy = new HANADialect().getLockingClauseStrategy(
				new QuerySpec( true ),
				new LockOptions( LockMode.PESSIMISTIC_WRITE, Timeouts.SKIP_LOCKED )
		);
		final StringBuilderSqlAppender appender = new StringBuilderSqlAppender();

		strategy.render( appender );

		assertEquals( " for update ignore locked", appender.toString() );
	}
}
