/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal.util;


import java.util.Locale;
import jakarta.persistence.FlushModeType;

import org.hibernate.FlushMode;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.MappingException;

import static org.hibernate.jpa.internal.JpaLogger.JPA_LOGGER;

/**
 * Helper to deal with conversions between {@link FlushModeType} and {@link FlushMode}.
 *
 * @author Steve Ebersole
 */
public class FlushModeTypeHelper {

	private FlushModeTypeHelper() {
	}

	public static FlushModeType getFlushModeType(FlushMode flushMode) {
		if ( flushMode == null ) {
			return null;
		}
		return switch (flushMode) {
			case ALWAYS -> {
				JPA_LOGGER.interpretingFlushModeAlwaysAsJpaAuto();
				yield FlushModeType.AUTO;
			}
			case MANUAL -> {
				JPA_LOGGER.interpretingFlushModeManualAsJpaCommit();
				yield FlushModeType.COMMIT;
			}
			case COMMIT -> FlushModeType.COMMIT;
			case AUTO -> FlushModeType.AUTO;
		};
	}

	public static QueryFlushMode getForcedFlushMode(FlushMode flushMode) {
		if ( flushMode == null ) {
			return QueryFlushMode.DEFAULT;
		}
		return switch (flushMode) {
			case ALWAYS -> QueryFlushMode.FLUSH;
			case COMMIT, MANUAL -> QueryFlushMode.NO_FLUSH;
			case AUTO ->
				// this is not precisely correctly correct, but good enough
					QueryFlushMode.DEFAULT;
		};
	}

	public static FlushMode getFlushMode(FlushModeType flushModeType) {
		if ( flushModeType == null ) {
			return null;
		}
		return switch (flushModeType) {
			case AUTO -> FlushMode.AUTO;
			case COMMIT -> FlushMode.COMMIT;
		};
	}

	public static FlushMode getFlushMode(QueryFlushMode queryFlushMode) {
		if ( queryFlushMode == null ) {
			return null;
		}
		return switch (queryFlushMode) {
			case FLUSH -> FlushMode.ALWAYS;
			case NO_FLUSH -> FlushMode.MANUAL;
			default -> null;
		};
	}

	public static FlushMode interpretFlushMode(Object value) {
		if ( value == null ) {
			return FlushMode.AUTO;
		}
		if ( value instanceof FlushMode flushMode ) {
			return flushMode;
		}
		else if ( value instanceof FlushModeType flushModeType ) {
			return getFlushMode( flushModeType );
		}
		else if ( value instanceof String string ) {
			return interpretExternalSetting( string );
		}
		else {
			throw new IllegalArgumentException( "Unknown FlushMode source : " + value );
		}
	}

	public static FlushMode interpretExternalSetting(String externalName) {
		if ( externalName == null ) {
			return null;
		}

		try {
			JPA_LOGGER.attemptingToInterpretExternalSettingAsFlushModeName( externalName );
			return FlushMode.valueOf( externalName.toUpperCase( Locale.ROOT) );
		}
		catch ( IllegalArgumentException e ) {
			JPA_LOGGER.attemptingToInterpretExternalSettingAsFlushModeTypeName( externalName );
		}

		try {
			return getFlushMode( FlushModeType.valueOf( externalName.toLowerCase( Locale.ROOT ) ) );
		}
		catch ( IllegalArgumentException ignore ) {
		}

		throw new MappingException( "unknown FlushMode : " + externalName );
	}
}
