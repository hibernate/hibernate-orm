/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.Timeouts;

import java.util.Map;
import java.util.function.Supplier;

import static org.hibernate.cfg.AvailableSettings.JAKARTA_LOCK_SCOPE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_LOCK_TIMEOUT;
import static org.hibernate.cfg.AvailableSettings.JPA_LOCK_SCOPE;
import static org.hibernate.cfg.AvailableSettings.JPA_LOCK_TIMEOUT;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.jpa.HibernateHints.HINT_FOLLOW_ON_LOCKING;
import static org.hibernate.jpa.HibernateHints.HINT_FOLLOW_ON_STRATEGY;

public final class LockOptionsHelper {

	private LockOptionsHelper() {
		//utility class, not to be constructed
	}

	/**
	 * Applies configuration properties on a {@link LockOptions} instance, passed as a supplier
	 * so to make it possible to skip allocating the {@link LockOptions} instance if there's
	 * nothing to set.
	 *
	 * @param properties The configuration properties
	 * @param lockOptions The reference to the lock to modify
	 */
	public static void applyPropertiesToLockOptions(Map<String, Object> properties, Supplier<LockOptions> lockOptions) {
		applyScope( properties, lockOptions );
		applyTimeout( properties, lockOptions );
		applyFollowOn( properties, lockOptions );
	}

	private static void applyScope(Map<String, Object> properties, Supplier<LockOptions> lockOptions) {
		final String lockScopeHint =
				properties.containsKey( JAKARTA_LOCK_SCOPE )
						? JAKARTA_LOCK_SCOPE
						: JPA_LOCK_SCOPE;
		final Object value = properties.get( lockScopeHint );
		if ( value != null ) {
			final var options = lockOptions.get();
			options.setLockScope( Locking.scopeFromHint( value ) );
		}
	}

	private static void applyTimeout(Map<String, Object> properties, Supplier<LockOptions> lockOptions) {
		final String lockTimeoutHint =
				properties.containsKey( JAKARTA_LOCK_TIMEOUT )
						? JAKARTA_LOCK_TIMEOUT
						: JPA_LOCK_TIMEOUT;
		final Object value = properties.get( lockTimeoutHint );
		if ( value != null ) {
			if ( lockTimeoutHint.equals( JPA_LOCK_TIMEOUT ) ) {
				DEPRECATION_LOGGER.deprecatedHint( JPA_LOCK_TIMEOUT, JAKARTA_LOCK_TIMEOUT );
			}
			final var options = lockOptions.get();
			options.setTimeout( Timeouts.fromJpaHint( value ) );
		}
	}

	private static void applyFollowOn(Map<String, Object> properties, Supplier<LockOptions> lockOptions) {
		final Object strategyValue = properties.get( HINT_FOLLOW_ON_STRATEGY );
		if ( strategyValue != null ) {
			final var options = lockOptions.get();
			options.setFollowOnStrategy( Locking.FollowOn.fromHint( strategyValue ) );
		}
		else {
			// accounts for manually specifying null...
			if ( properties.containsKey( HINT_FOLLOW_ON_LOCKING ) ) {
				final var strategyFromLegacy =
						Locking.FollowOn.fromLegacyValue( getBoolean( HINT_FOLLOW_ON_LOCKING, properties ) );
				DEPRECATION_LOGGER.deprecatedHint( HINT_FOLLOW_ON_LOCKING, HINT_FOLLOW_ON_STRATEGY );
				lockOptions.get().setFollowOnStrategy( strategyFromLegacy );
			}
		}
	}

}
