/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal.util;

import jakarta.persistence.LockModeType;

import org.hibernate.LockMode;
import org.hibernate.internal.util.LockModeConverter;

/**
 * Helper to deal with conversions between {@link LockModeType} and {@link LockMode}.
 *
 * @author Steve Ebersole
 */
public final class LockModeTypeHelper {
	private LockModeTypeHelper() {
	}

	public static LockModeType getLockModeType(LockMode lockMode) {
		return LockModeConverter.convertToLockModeType( lockMode );
	}

	public static LockMode getLockMode(LockModeType lockModeType) {
		return LockModeConverter.convertToLockMode( lockModeType );
	}

	public static LockMode interpretLockMode(Object value) {
		if ( value == null ) {
			return LockMode.NONE;
		}
		else if ( value instanceof LockMode lockMode ) {
			return lockMode;
		}
		else if ( value instanceof LockModeType lockModeType ) {
			return getLockMode( lockModeType );
		}
		else if ( value instanceof String string ) {
			return LockMode.fromExternalForm( string );
		}
		else {
			throw new IllegalArgumentException( "Could not interpret '" + value.getClass().getName() + "' as a LockMode" );
		}
	}

}
