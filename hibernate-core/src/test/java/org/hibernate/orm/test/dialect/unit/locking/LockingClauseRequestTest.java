/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.locking;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Timeouts;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.lock.internal.StandardLockingClauseStrategy;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.LockingSqlRewriteRequest;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.lock.spi.RowLockStrategy;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Tests the structured locking-clause renderer boundary.
///
/// @author Steve Ebersole
class LockingClauseRequestTest {
	@Test
	void requestIsImmutable() {
		final List<LockingClauseRequest.Target> targets = new ArrayList<>();
		targets.add( new LockingClauseRequest.TableTarget( "p" ) );

		final LockingClauseRequest request = new LockingClauseRequest(
				PessimisticLockKind.UPDATE,
				Timeouts.NO_WAIT,
				targets
		);
		targets.add( new LockingClauseRequest.ColumnTarget( "p", "id" ) );

		assertEquals( List.of( new LockingClauseRequest.TableTarget( "p" ) ), request.targets() );
		assertThrows(
				UnsupportedOperationException.class,
				() -> request.targets().add( new LockingClauseRequest.TableTarget( "o" ) )
		);
	}

	@Test
	void requestRejectsNoLockKind() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new LockingClauseRequest( PessimisticLockKind.NONE, Timeouts.WAIT_FOREVER, List.of() )
		);
	}

	@Test
	void standardStrategyInvokesFocusedRenderer() {
		final AtomicReference<LockingClauseRequest> capturedRequest = new AtomicReference<>();
		final StandardLockingClauseStrategy strategy = new StandardLockingClauseStrategy(
				request -> {
					capturedRequest.set( request );
					return " for update nowait";
				},
				PessimisticLockKind.UPDATE,
				RowLockStrategy.NONE,
				new LockOptions( LockMode.PESSIMISTIC_WRITE, Timeouts.NO_WAIT ),
				Set.of()
		);
		final StringBuilderSqlAppender appender = new StringBuilderSqlAppender();

		strategy.render( appender );

		assertEquals( " for update nowait", appender.toString() );
		assertEquals( PessimisticLockKind.UPDATE, capturedRequest.get().lockKind() );
		assertEquals( Timeouts.NO_WAIT, capturedRequest.get().timeout() );
		assertEquals( List.of(), capturedRequest.get().targets() );
	}

	@Test
	void completedSqlRewriterPreservesStructuredColumnTargets() {
		final var lockingSupport = new HANADialect().getLockingSupport();
		final var result = lockingSupport.getLockingSqlRewriter().rewrite(
				new LockingSqlRewriteRequest(
						"select * from person p",
						PessimisticLockKind.UPDATE,
						Timeouts.SKIP_LOCKED,
						List.of(
								new LockingClauseRequest.ColumnTarget( "p", "id" ),
								new LockingClauseRequest.ColumnTarget( "p", "tenant_id" )
						)
				)
		);

		assertEquals(
				"select * from person p for update of p.id,p.tenant_id ignore locked",
				result.sql()
		);
	}
}
