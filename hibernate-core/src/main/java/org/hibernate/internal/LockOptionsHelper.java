/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.hibernate.LockOptions;
import org.hibernate.Locking;

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
		final Object lockScope = properties.get( lockScopeHint );
		if ( lockScope != null ) {
			final var options = lockOptions.get();
			if ( lockScope instanceof Locking.Scope scope ) {
				options.setScope( scope );
			}
			else if ( lockScope instanceof PessimisticLockScope pessimisticLockScope ) {
				options.setLockScope( pessimisticLockScope );
			}
			else if ( lockScope instanceof String string ) {
				final var scope = Locking.Scope.interpret( string );
				if ( scope != null ) {
					options.setScope( scope );
				}
			}
			else {
				throw new PersistenceException( "Unable to interpret " + lockScopeHint + ": " + lockScope );
			}
		}
	}

	private static void applyTimeout(Map<String, Object> properties, Supplier<LockOptions> lockOptions) {
		final String lockTimeoutHint =
				properties.containsKey( JAKARTA_LOCK_TIMEOUT )
						? JAKARTA_LOCK_TIMEOUT
						: JPA_LOCK_TIMEOUT;
		final Object lockTimeout = properties.get( lockTimeoutHint );
		if ( lockTimeout != null ) {
			if ( lockTimeoutHint.equals( JPA_LOCK_TIMEOUT ) ) {
				DEPRECATION_LOGGER.deprecatedHint( JPA_LOCK_TIMEOUT, JAKARTA_LOCK_TIMEOUT );
			}
			final var options = lockOptions.get();
			if ( lockTimeout instanceof Timeout timeout ) {
				options.setTimeout( timeout );
			}
			else if ( lockTimeout instanceof String string ) {
				options.setTimeOut( Integer.parseInt( string ) );
			}
			else if ( lockTimeout instanceof Number number ) {
				int timeout = number.intValue();
				options.setTimeOut( timeout );
			}
			else {
				throw new PersistenceException( "Unable to interpret " + lockTimeoutHint + ": " + lockTimeout );
			}
		}
	}

	private static void applyFollowOn(Map<String, Object> properties, Supplier<LockOptions> lockOptions) {
		final Object strategyValue = properties.get( HINT_FOLLOW_ON_STRATEGY );
		if ( strategyValue != null ) {
			final var options = lockOptions.get();
			if ( strategyValue instanceof Locking.FollowOn strategy ) {
				options.setFollowOnStrategy( strategy );
			}
			else if ( strategyValue instanceof String name ) {
				options.setFollowOnStrategy( Locking.FollowOn.interpret( name ) );
			}
			else {
				throw new PersistenceException( "Unable to interpret " + HINT_FOLLOW_ON_STRATEGY + ": " + strategyValue );
			}
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
