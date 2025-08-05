/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.options;

import jakarta.persistence.Timeout;
import org.hibernate.Timeouts;
import org.hibernate.community.dialect.GaussDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

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
				expectedInitialValue = 50;
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
			reason = "The docs claim 0 is a valid value as 'no wait'; but in my testing, after setting to 0 we get back 1"
	)
	void testNoWait(SessionFactoryScope factoryScope) {
		// this is dependent on the Dialect's ConnectionLockTimeoutType
		factoryScope.inTransaction( (session) -> session.doWork( (conn) -> {
			final LockingSupport lockingSupport = session.getDialect().getLockingSupport();

			final ConnectionLockTimeoutStrategy connectionStrategy = lockingSupport.getConnectionLockTimeoutStrategy();
			final ConnectionLockTimeoutStrategy.Level lockTimeoutType = connectionStrategy.getSupportedLevel();
			final int initialValue;
			if ( session.getDialect() instanceof MySQLDialect ) {
				initialValue = 50;
			}
			else {
				initialValue = Timeouts.WAIT_FOREVER_MILLI;
			}

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
