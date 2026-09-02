/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.locking;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Timeouts;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.lock.internal.LockingSupportSimple;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests the plain `FOR UPDATE` renderer used by simple locking profiles.
///
/// @author Steve Ebersole
class SimpleLockingClauseRendererTest {
	@Test
	void simpleSupportRendersTheStandardClause() {
		assertEquals(
				" for update",
				LockingSupportSimple.STANDARD_SUPPORT.getLockingClauseRenderer().render(
						new LockingClauseRequest(
								PessimisticLockKind.SHARE,
								Timeouts.NO_WAIT,
								List.of( new LockingClauseRequest.TableTarget( "ignored" ) )
						)
				)
		);
	}

	@Test
	void defaultAndSpannerDialectsUseTheSimpleRenderer() {
		assertDialectRenders( new Dialect( DatabaseVersion.make( 1 ) ) {}, " for update" );
		assertDialectRenders( new SpannerDialect(), " for update" );
		assertDialectRenders( new SpannerPostgreSQLDialect(), " for update" );
	}

	private static void assertDialectRenders(Dialect dialect, String expected) {
		final var strategy = dialect.getLockingClauseStrategy(
				new QuerySpec( true ),
				new LockOptions( LockMode.PESSIMISTIC_WRITE, Timeouts.WAIT_FOREVER )
		);
		final StringBuilderSqlAppender appender = new StringBuilderSqlAppender();

		strategy.render( appender );

		assertEquals( expected, appender.toString() );
	}
}
