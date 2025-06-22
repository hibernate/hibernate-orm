/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import java.util.Map;
import java.util.function.Supplier;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockScope;

import org.hibernate.LockOptions;

import static jakarta.persistence.PessimisticLockScope.EXTENDED;
import static jakarta.persistence.PessimisticLockScope.NORMAL;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_LOCK_SCOPE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_LOCK_TIMEOUT;
import static org.hibernate.cfg.AvailableSettings.JPA_LOCK_SCOPE;
import static org.hibernate.cfg.AvailableSettings.JPA_LOCK_TIMEOUT;

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
	}

	private static void applyScope(Map<String, Object> props, Supplier<LockOptions> lockOptions) {
		String lockScopeHint = JPA_LOCK_SCOPE;
		Object lockScope = props.get( lockScopeHint );
		if ( lockScope == null ) {
			lockScopeHint = JAKARTA_LOCK_SCOPE;
			lockScope = props.get( lockScopeHint );
		}

		if ( lockScope instanceof String string ) {
			lockOptions.get().setLockScope( EXTENDED.name().equalsIgnoreCase( string ) ? EXTENDED : NORMAL );
		}
		else if ( lockScope instanceof PessimisticLockScope pessimisticLockScope ) {
			lockOptions.get().setLockScope( pessimisticLockScope );
		}
		else if ( lockScope != null ) {
			throw new PersistenceException( "Unable to parse " + lockScopeHint + ": " + lockScope );
		}
	}

	private static void applyTimeout(Map<String, Object> props, Supplier<LockOptions> lockOptions) {
		String timeoutHint = JPA_LOCK_TIMEOUT;
		Object lockTimeout = props.get( timeoutHint );
		if (lockTimeout == null) {
			timeoutHint = JAKARTA_LOCK_TIMEOUT;
			lockTimeout = props.get( timeoutHint );
		}

		if ( lockTimeout instanceof String string ) {
			lockOptions.get().setTimeOut( Integer.parseInt( string ) );
		}
		else if ( lockTimeout instanceof Number number ) {
			int timeout = number.intValue();
			lockOptions.get().setTimeOut( timeout );
		}
		else if ( lockTimeout != null ) {
			throw new PersistenceException( "Unable to parse " + timeoutHint + ": " + lockTimeout );
		}
	}

}
