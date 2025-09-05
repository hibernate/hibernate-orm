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

import org.jboss.logging.Logger;

/**
 * Helper to deal with conversions between {@link FlushModeType} and {@link FlushMode}.
 *
 * @author Steve Ebersole
 */
public class FlushModeTypeHelper {
	private static final Logger LOG = Logger.getLogger( FlushModeTypeHelper.class );

	private FlushModeTypeHelper() {
	}

	public static FlushModeType getFlushModeType(FlushMode flushMode) {
		if ( flushMode == null ) {
			return null;
		}
		return switch (flushMode) {
			case ALWAYS -> {
				LOG.debug("Interpreting Hibernate FlushMode.ALWAYS as JPA FlushModeType.AUTO (may cause problems if relying on FlushMode.ALWAYS-specific behavior)");
				yield FlushModeType.AUTO;
			}
			case MANUAL -> {
				LOG.debug("Interpreting Hibernate FlushMode.MANUAL as JPA FlushModeType.COMMIT (may cause problems if relying on FlushMode.MANUAL-specific behavior)");
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
			LOG.tracef( "Attempting to interpret external setting [%s] as FlushMode name", externalName );
			return FlushMode.valueOf( externalName.toUpperCase( Locale.ROOT) );
		}
		catch ( IllegalArgumentException e ) {
			LOG.tracef( "Attempting to interpret external setting [%s] as FlushModeType name", externalName );
		}

		try {
			return getFlushMode( FlushModeType.valueOf( externalName.toLowerCase( Locale.ROOT ) ) );
		}
		catch ( IllegalArgumentException ignore ) {
		}

		throw new MappingException( "unknown FlushMode : " + externalName );
	}
}
