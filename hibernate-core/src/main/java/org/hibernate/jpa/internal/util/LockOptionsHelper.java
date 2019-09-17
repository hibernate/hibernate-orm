/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.util;

import java.util.Map;
import java.util.function.Supplier;
import javax.persistence.PersistenceException;
import javax.persistence.PessimisticLockScope;

import org.hibernate.LockOptions;

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
	 * @param lockOptionsSupplier The reference to the lock to modify
	 */
	public static void applyPropertiesToLockOptions(final Map<String, Object> props, final Supplier<LockOptions> lockOptionsSupplier) {
		Object lockScope = props.get( JPA_LOCK_SCOPE );
		if ( lockScope instanceof String && PessimisticLockScope.valueOf( (String) lockScope ) == PessimisticLockScope.EXTENDED ) {
			lockOptionsSupplier.get().setScope( true );
		}
		else if ( lockScope instanceof PessimisticLockScope ) {
			boolean extended = PessimisticLockScope.EXTENDED.equals( lockScope );
			lockOptionsSupplier.get().setScope( extended );
		}
		else if ( lockScope != null ) {
			throw new PersistenceException( "Unable to parse " + JPA_LOCK_SCOPE + ": " + lockScope );
		}

		Object lockTimeout = props.get( JPA_LOCK_TIMEOUT );
		int timeout = 0;
		boolean timeoutSet = false;
		if ( lockTimeout instanceof String ) {
			timeout = Integer.parseInt( (String) lockTimeout );
			timeoutSet = true;
		}
		else if ( lockTimeout instanceof Number ) {
			timeout = ( (Number) lockTimeout ).intValue();
			timeoutSet = true;
		}
		else if ( lockTimeout != null ) {
			throw new PersistenceException( "Unable to parse " + JPA_LOCK_TIMEOUT + ": " + lockTimeout );
		}

		if ( timeoutSet ) {
			if ( timeout == LockOptions.SKIP_LOCKED ) {
				lockOptionsSupplier.get().setTimeOut( LockOptions.SKIP_LOCKED );
			}
			else if ( timeout < 0 ) {
				lockOptionsSupplier.get().setTimeOut( LockOptions.WAIT_FOREVER );
			}
			else if ( timeout == 0 ) {
				lockOptionsSupplier.get().setTimeOut( LockOptions.NO_WAIT );
			}
			else {
				lockOptionsSupplier.get().setTimeOut( timeout );
			}
		}
	}

}
