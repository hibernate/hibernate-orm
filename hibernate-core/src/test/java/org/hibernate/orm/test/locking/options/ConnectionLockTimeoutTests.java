/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.options;

import jakarta.persistence.Timeout;
import org.hibernate.Timeouts;
import org.hibernate.community.dialect.GaussDBDialect;
import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConnectionLockTimeouts.class)
public class ConnectionLockTimeoutTests {
	@Test
	void testSimpleUsage(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> session.doWork( (conn) -> {
			final int expectedInitialValue;
			if ( session.getDialect() instanceof MySQLDialect ) {
				expectedInitialValue = 50_000;
			}
			else if ( session.getDialect() instanceof GaussDBDialect ) {
				expectedInitialValue = 20 * 60 * 1000;
			}
			else {
				expectedInitialValue = Timeouts.WAIT_FOREVER_MILLI;
			}

			final LockingSupport lockingSupport = session.getDialect().getLockingSupport();
			final ConnectionLockTimeoutStrategy connectionStrategy = lockingSupport.getConnectionLockTimeoutStrategy();
			final Timeout initialLockTimeout = connectionStrategy.getLockTimeout( conn, session.getFactory() );
			assertThat( initialLockTimeout.milliseconds() ).isEqualTo( expectedInitialValue );

			try {
				final Timeout timeout = Timeout.milliseconds( 2000 );
				connectionStrategy.setLockTimeout( timeout, conn, session.getFactory() );

				final Timeout adjustedLockTimeout = connectionStrategy.getLockTimeout( conn, session.getFactory() );
				assertThat( adjustedLockTimeout.milliseconds() ).isEqualTo( 2000 );

				connectionStrategy.setLockTimeout( Timeouts.WAIT_FOREVER, conn, session.getFactory() );

				final Timeout resetLockTimeout = connectionStrategy.getLockTimeout( conn, session.getFactory() );
				assertThat( resetLockTimeout.milliseconds() ).isEqualTo( Timeouts.WAIT_FOREVER_MILLI );
			}
			finally {
				connectionStrategy.setLockTimeout( Timeout.milliseconds( expectedInitialValue ), conn, session.getFactory() );
			}
		} ) );
	}

	/**
	 * Tests that lock_timeout values are correctly handled despite PostgreSQL
	 * canonical formatting (e.g., "60s" displayed as "1min").
	 */
	@Test
	void testCanonicalLockTimeoutFormat(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> session.doWork( (conn) -> {
			final int expectedInitialValue;
			if ( session.getDialect() instanceof MySQLDialect ) {
				expectedInitialValue = 50_000;
			}
			else if ( session.getDialect() instanceof GaussDBDialect ) {
				expectedInitialValue = 20 * 60 * 1000;
			}
			else {
				expectedInitialValue = Timeouts.WAIT_FOREVER_MILLI;
			}

			final LockingSupport lockingSupport = session.getDialect().getLockingSupport();
			final ConnectionLockTimeoutStrategy connectionStrategy = lockingSupport.getConnectionLockTimeoutStrategy();
			final Timeout initialLockTimeout = connectionStrategy.getLockTimeout( conn, session.getFactory() );
			assertThat( initialLockTimeout.milliseconds() ).isEqualTo( expectedInitialValue );

			List<Duration> durs;
			if ( session.getDialect() instanceof TiDBDialect ) {
				// The supported values are between 1 and 3600 seconds
				// 3600 means infinite, so it is special
				durs = List.of(
					Duration.ofSeconds(1),
					Duration.ofSeconds(2),
					Duration.ofSeconds(59),
					Duration.ofMinutes(1),
					Duration.ofMinutes(2),
					Duration.ofMinutes(59)
				);
			}
			else if ( session.getDialect() instanceof MySQLDialect ) {
				// The minimum value of innodb_lock_wait_timeout in MySQL is 1 second.
				durs = List.of(
					Duration.ofSeconds(1),
					Duration.ofSeconds(2),
					Duration.ofSeconds(59),
					Duration.ofMinutes(1),
					Duration.ofMinutes(2),
					Duration.ofMinutes(59),
					Duration.ofHours(1),
					Duration.ofHours(2),
					Duration.ofHours(23),
					Duration.ofDays(1)
				);
			} else {
				durs = List.of(
					Duration.ofMillis(1),
					Duration.ofMillis(2),
					Duration.ofMillis(999),
					Duration.ofSeconds(1),
					Duration.ofSeconds(2),
					Duration.ofSeconds(59),
					Duration.ofMinutes(1),
					Duration.ofMinutes(2),
					Duration.ofMinutes(59),
					Duration.ofHours(1),
					Duration.ofHours(2),
					Duration.ofHours(23),
					Duration.ofDays(1)
				);
			}

			try {
				for ( Duration dur : durs ) {
					int timeoutInMillis = (int) dur.toMillis();
					Timeout timeout = Timeout.milliseconds( timeoutInMillis );
					connectionStrategy.setLockTimeout( timeout, conn, session.getFactory() );

					Timeout adjustedLockTimeout = connectionStrategy.getLockTimeout( conn, session.getFactory() );
					assertThat( adjustedLockTimeout.milliseconds() ).isEqualTo( timeoutInMillis );
				}
			}
			finally {
				connectionStrategy.setLockTimeout( Timeout.milliseconds( expectedInitialValue ), conn, session.getFactory() );
			}
		} ) );
	}

	@Test
	void testSkipLocked(SessionFactoryScope factoryScope) {
		// this is never supported
		factoryScope.inTransaction( (session) -> session.doWork( (conn) -> {
			final LockingSupport lockingSupport = session.getDialect().getLockingSupport();
			final ConnectionLockTimeoutStrategy connectionStrategy = lockingSupport.getConnectionLockTimeoutStrategy();

			try {
				connectionStrategy.setLockTimeout( Timeouts.SKIP_LOCKED, conn, session.getFactory() );
				fail( "Expecting a failure with SKIP_LOCKED" );
			}
			catch (Exception expected) {
			}
		} ) );
	}

	@Test
	@SkipForDialect(
			dialectClass = MySQLDialect.class,
			matchSubTypes = true,
			reason = "The innodb_lock_wait_timeout variable can't be set to zero."
	)
	@SkipForDialect(
			dialectClass = SybaseDialect.class,
			matchSubTypes = true,
			reason = "Sybase docs say no-wait is supported, but after setting no-wait -1 is returned.  And it unfortunately does not fail setting as no-wait."
	)
	void testNoWait(SessionFactoryScope factoryScope) {
		// this is dependent on the Dialect's ConnectionLockTimeoutType
		factoryScope.inTransaction( (session) -> session.doWork( (conn) -> {
			final LockingSupport lockingSupport = session.getDialect().getLockingSupport();

			final ConnectionLockTimeoutStrategy connectionStrategy = lockingSupport.getConnectionLockTimeoutStrategy();
			final ConnectionLockTimeoutStrategy.Level lockTimeoutType = connectionStrategy.getSupportedLevel();
			final int initialValue;
			initialValue = Timeouts.WAIT_FOREVER_MILLI;

			try {
				connectionStrategy.setLockTimeout( Timeouts.NO_WAIT, conn, session.getFactory() );
				if ( lockTimeoutType != ConnectionLockTimeoutStrategy.Level.EXTENDED ) {
					fail( "Expecting a failure with NO_WAIT" );
				}

				// if we get here, it should be EXTENDED (and the set op succeeded)
				final Timeout lockTimeout = connectionStrategy.getLockTimeout( conn, session.getFactory() );
				assertThat( lockTimeout.milliseconds() ).isEqualTo( Timeouts.NO_WAIT_MILLI );
			}
			catch (Exception e) {
				if ( lockTimeoutType == ConnectionLockTimeoutStrategy.Level.EXTENDED ) {
					throw e;
				}
			}
			finally {
				connectionStrategy.setLockTimeout( Timeout.milliseconds( initialValue ), conn, session.getFactory() );
			}
		} ) );
	}
}
