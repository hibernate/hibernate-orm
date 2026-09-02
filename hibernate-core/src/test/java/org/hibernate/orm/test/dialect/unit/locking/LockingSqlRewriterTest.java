/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.locking;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Timeouts;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.LockingSqlRewriteRequest;
import org.hibernate.dialect.lock.spi.LockingSqlRewriteResult;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.lock.spi.StandardLockingSqlRewriters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Tests standard completed-SQL locking composition and outcome reporting.
///
/// @author Steve Ebersole
class LockingSqlRewriterTest {
	@Test
	void requestIsImmutable() {
		final List<LockingClauseRequest.Target> targets = new ArrayList<>();
		targets.add( new LockingClauseRequest.TableTarget( "p" ) );
		final LockingSqlRewriteRequest request = request(
				"select * from person p",
				PessimisticLockKind.UPDATE,
				targets
		);

		targets.add( new LockingClauseRequest.TableTarget( "a" ) );

		assertEquals( List.of( new LockingClauseRequest.TableTarget( "p" ) ), request.targets() );
		assertThrows(
				UnsupportedOperationException.class,
				() -> request.targets().add( new LockingClauseRequest.TableTarget( "o" ) )
		);
	}

	@Test
	void clauseStrategiesComposeTheRenderer() {
		final var suffix = StandardLockingSqlRewriters.statementSuffix( request -> " for update" );
		final var prefix = StandardLockingSqlRewriters.statementPrefix( request -> "locking row" );
		final LockingSqlRewriteRequest request = request(
				"select * from person",
				PessimisticLockKind.UPDATE,
				List.of()
		);

		assertEquals(
				new LockingSqlRewriteResult(
						"select * from person for update",
						LockingSqlRewriteResult.Outcome.APPLIED
				),
				suffix.rewrite( request )
		);
		assertEquals(
				new LockingSqlRewriteResult(
						"locking row select * from person",
						LockingSqlRewriteResult.Outcome.APPLIED
				),
				prefix.rewrite( request )
		);
	}

	@Test
	void tableHintStrategyRequiresKnownTargetsAndRewritesAtomically() {
		final var rewriter = StandardLockingSqlRewriters.tableHints( request -> " with (updlock)" );

		assertEquals(
				LockingSqlRewriteResult.Outcome.UNSUPPORTED,
				rewriter.rewrite( request(
						"select * from person p",
						PessimisticLockKind.UPDATE,
						List.of()
				) ).outcome()
		);
		assertEquals(
				new LockingSqlRewriteResult(
						"select * from person p with (updlock) join address a with (updlock) on a.id=p.address_id",
						LockingSqlRewriteResult.Outcome.APPLIED
				),
				rewriter.rewrite( request(
						"select * from person p join address a on a.id=p.address_id",
						PessimisticLockKind.UPDATE,
						List.of(
								new LockingClauseRequest.TableTarget( "p" ),
								new LockingClauseRequest.ColumnTarget( "a", "id" )
						)
				) )
		);
		assertEquals(
				new LockingSqlRewriteResult(
						"select * from person p",
						LockingSqlRewriteResult.Outcome.UNSUPPORTED
				),
				rewriter.rewrite( request(
						"select * from person p",
						PessimisticLockKind.UPDATE,
						List.of(
								new LockingClauseRequest.TableTarget( "p" ),
								new LockingClauseRequest.TableTarget( "missing" )
						)
				) )
		);
	}

	@Test
	void noLockKindIsNotApplicable() {
		final var rewriter = StandardLockingSqlRewriters.statementSuffix( request -> " should not render" );
		assertEquals(
				LockingSqlRewriteResult.Outcome.NOT_APPLICABLE,
				rewriter.rewrite( request(
						"select * from person",
						PessimisticLockKind.NONE,
						List.of()
				) ).outcome()
		);
	}

	private static LockingSqlRewriteRequest request(
			String sql,
			PessimisticLockKind lockKind,
			List<LockingClauseRequest.Target> targets) {
		return new LockingSqlRewriteRequest( sql, lockKind, Timeouts.WAIT_FOREVER, targets );
	}
}
