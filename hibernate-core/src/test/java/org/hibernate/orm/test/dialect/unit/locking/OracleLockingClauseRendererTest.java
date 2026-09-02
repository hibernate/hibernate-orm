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
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.lock.internal.OracleLockingSupport;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests the Oracle-family locking-clause renderer.
///
/// @author Steve Ebersole
class OracleLockingClauseRendererTest {
	@Test
	void rendersQualifiedColumnTargetsAndFiniteWait() {
		assertEquals(
				" for update of p.id,p.tenant_id wait 2",
				OracleLockingSupport.ORACLE_LOCKING_SUPPORT.render( new LockingClauseRequest(
						PessimisticLockKind.UPDATE,
						Timeout.milliseconds( 1_500 ),
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
				" for update skip locked",
				OracleLockingSupport.ORACLE_LOCKING_SUPPORT.render( new LockingClauseRequest(
						PessimisticLockKind.SHARE,
						Timeouts.SKIP_LOCKED,
						List.of()
				) )
		);
	}

	@Test
	void dialectStrategyUsesSuppliedRenderer() {
		final var strategy = new OracleDialect().getLockingClauseStrategy(
				new QuerySpec( true ),
				new LockOptions( LockMode.PESSIMISTIC_WRITE, Timeout.seconds( 1 ) )
		);
		final StringBuilderSqlAppender appender = new StringBuilderSqlAppender();

		strategy.render( appender );

		assertEquals( " for update wait 1", appender.toString() );
	}
}
