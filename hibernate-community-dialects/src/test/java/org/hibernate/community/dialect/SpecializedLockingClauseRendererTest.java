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
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests the specialized Altibase, SingleStore, and Informix clause profiles.
///
/// @author Steve Ebersole
class SpecializedLockingClauseRendererTest {
	@Test
	void altibaseUsesParameterizedTimeoutRendering() {
		final AltibaseDialect dialect = new AltibaseDialect();
		assertEquals(
				" for update nowait",
				dialect.getLockingSupport().getLockingClauseRenderer().render(
						request( PessimisticLockKind.SHARE, Timeouts.NO_WAIT )
				)
		);
		assertEquals(
				" for update wait 2",
				dialect.getLockingSupport().getLockingClauseRenderer().render(
						request( PessimisticLockKind.UPDATE, Timeout.milliseconds( 1_500 ) )
				)
		);
		assertEquals(
				" for update",
				dialect.getLockingSupport().getLockingClauseRenderer().render(
						request( PessimisticLockKind.UPDATE, Timeouts.SKIP_LOCKED )
				)
		);
		assertDialectRenders(
				dialect,
				LockMode.PESSIMISTIC_WRITE,
				Timeout.seconds( 2 ),
				" for update wait 2"
		);
	}

	@Test
	void singleStoreSettingControlsClauseSelection() {
		final SingleStoreDialect disabled = new SingleStoreDialect(
				DatabaseVersion.make( 8 ),
				null,
				false
		);
		assertEquals(
				PessimisticLockStyle.NONE,
				disabled.getLockingSupport().getMetadata().getPessimisticLockStyle()
		);
		assertDialectRenders(
				disabled,
				LockMode.PESSIMISTIC_WRITE,
				Timeouts.WAIT_FOREVER,
				""
		);

		final SingleStoreDialect enabled = new SingleStoreDialect(
				DatabaseVersion.make( 8 ),
				null,
				true
		);
		assertEquals(
				PessimisticLockStyle.CLAUSE,
				enabled.getLockingSupport().getMetadata().getPessimisticLockStyle()
		);
		assertDialectRenders(
				enabled,
				LockMode.PESSIMISTIC_WRITE,
				Timeouts.NO_WAIT,
				" for update"
		);
	}

	@Test
	void informixKeepsTimeoutsAtConnectionLevel() {
		final InformixDialect dialect = new InformixDialect();
		assertEquals(
				LockTimeoutType.CONNECTION,
				dialect.getLockingSupport().getMetadata().getLockTimeoutType( Timeouts.NO_WAIT )
		);
		assertEquals(
				" for update",
				dialect.getLockingSupport().getLockingClauseRenderer().render(
						new LockingClauseRequest(
								PessimisticLockKind.UPDATE,
								Timeout.seconds( 2 ),
								List.of( new LockingClauseRequest.ColumnTarget( "account", "id" ) )
						)
				)
		);
		assertDialectRenders(
				dialect,
				LockMode.PESSIMISTIC_READ,
				Timeouts.NO_WAIT,
				" for update"
		);
	}

	@Test
	void parameterizedRendererPreservesSybaseAnywhereTargets() {
		assertEquals(
				" for update of account.id",
				StandardLockingSupports.sybaseAnywhere( DatabaseVersion.make( 10 ) )
						.getLockingClauseRenderer()
						.render(
								new LockingClauseRequest(
										PessimisticLockKind.UPDATE,
										Timeouts.WAIT_FOREVER,
										List.of( new LockingClauseRequest.ColumnTarget( "account", "id" ) )
								)
						)
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
