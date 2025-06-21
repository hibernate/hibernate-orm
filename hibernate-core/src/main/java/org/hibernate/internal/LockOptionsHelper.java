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
import org.hibernate.internal.log.DeprecationLogger;

import java.util.Map;
import java.util.function.Supplier;

import static org.hibernate.cfg.AvailableSettings.JAKARTA_LOCK_SCOPE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_LOCK_TIMEOUT;
import static org.hibernate.cfg.AvailableSettings.JPA_LOCK_SCOPE;
import static org.hibernate.cfg.AvailableSettings.JPA_LOCK_TIMEOUT;
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
	 * @param props The configuration properties
	 * @param lockOptions The reference to the lock to modify
	 */
	public static void applyPropertiesToLockOptions(Map<String, Object> props, Supplier<LockOptions> lockOptions) {
		applyScope( props, lockOptions );
		applyTimeout( props, lockOptions );
		applyFollowOn( props, lockOptions );
	}

	private static void applyScope(Map<String, Object> props, Supplier<LockOptions> lockOptions) {
		String lockScopeHint = JAKARTA_LOCK_SCOPE;
		Object value = props.get( lockScopeHint );
		if ( value == null ) {
			lockScopeHint = JPA_LOCK_SCOPE;
			value = props.get( lockScopeHint );
		}

		if ( value instanceof Locking.Scope scope ) {
			lockOptions.get().setScope( scope );
		}
		else if ( value instanceof PessimisticLockScope pessimisticLockScope ) {
			lockOptions.get().setLockScope( pessimisticLockScope );
		}
		else if ( value instanceof String string ) {
			final Locking.Scope scope = Locking.Scope.interpret( string );
			if ( scope != null ) {
				lockOptions.get().setScope( scope );
			}
		}
		else if ( value != null ) {
			throw new PersistenceException( "Unable to interpret " + lockScopeHint + ": " + value );
		}
	}

	private static void applyTimeout(Map<String, Object> props, Supplier<LockOptions> lockOptions) {
		String timeoutHint = JAKARTA_LOCK_TIMEOUT;
		Object lockTimeout = props.get( timeoutHint );
		if (lockTimeout == null) {
			timeoutHint = JPA_LOCK_TIMEOUT;
			lockTimeout = props.get( timeoutHint );
			if ( lockTimeout != null ) {
				DeprecationLogger.DEPRECATION_LOGGER.deprecatedHint( JPA_LOCK_TIMEOUT, JAKARTA_LOCK_TIMEOUT );
			}
		}

		if ( lockTimeout instanceof Timeout timeout ) {
			lockOptions.get().setTimeout( timeout );
		}
		else if ( lockTimeout instanceof String string ) {
			lockOptions.get().setTimeOut( Integer.parseInt( string ) );
		}
		else if ( lockTimeout instanceof Number number ) {
			int timeout = number.intValue();
			lockOptions.get().setTimeOut( timeout );
		}
		else if ( lockTimeout != null ) {
			throw new PersistenceException( "Unable to interpret " + timeoutHint + ": " + lockTimeout );
		}
	}

	private static void applyFollowOn(Map<String, Object> props, Supplier<LockOptions> lockOptions) {
		final Object strategyValue = props.get( HINT_FOLLOW_ON_STRATEGY );
		if ( strategyValue != null  ) {
			if ( strategyValue instanceof Locking.FollowOn strategy ) {
				lockOptions.get().setFollowOnStrategy( strategy );
			}
			else if ( strategyValue instanceof String name ) {
				lockOptions.get().setFollowOnStrategy( Locking.FollowOn.interpret( name ) );
			}
			else {
				throw new PersistenceException( "Unable to interpret " + HINT_FOLLOW_ON_STRATEGY + ": " + strategyValue );
			}
		}
		else {
			// accounts for manually specifying null...
			if ( props.containsKey( HINT_FOLLOW_ON_LOCKING ) ) {
				final Locking.FollowOn strategyFromLegacy = Locking.FollowOn.fromLegacyValue( getBoolean( HINT_FOLLOW_ON_LOCKING, props ) );
				DeprecationLogger.DEPRECATION_LOGGER.deprecatedHint( HINT_FOLLOW_ON_LOCKING, HINT_FOLLOW_ON_STRATEGY );
				lockOptions.get().setFollowOnStrategy( strategyFromLegacy );
			}
		}
	}

}
