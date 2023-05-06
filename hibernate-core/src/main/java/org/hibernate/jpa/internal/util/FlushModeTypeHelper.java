/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.util;


import java.util.Locale;
import jakarta.persistence.FlushModeType;

import org.hibernate.AssertionFailure;
import org.hibernate.FlushMode;
import org.hibernate.ForcedFlushMode;
import org.hibernate.MappingException;

import org.jboss.logging.Logger;

/**
 * Helper to deal with {@link FlushModeType} <-> {@link FlushMode} conversions.
 *
 * @author Steve Ebersole
 */
public class FlushModeTypeHelper {
	private static final Logger log = Logger.getLogger( FlushModeTypeHelper.class );

	private FlushModeTypeHelper() {
	}

	public static FlushModeType getFlushModeType(FlushMode flushMode) {
		if ( flushMode == null ) {
			return null;
		}
		switch ( flushMode ) {
			case ALWAYS:
				log.debug( "Interpreting Hibernate FlushMode#ALWAYS to JPA FlushModeType#AUTO; may cause problems if relying on FlushMode#ALWAYS-specific behavior" );
				return FlushModeType.AUTO;
			case MANUAL:
				log.debug( "Interpreting Hibernate FlushMode#MANUAL to JPA FlushModeType#COMMIT; may cause problems if relying on FlushMode#MANUAL-specific behavior" );
				return FlushModeType.COMMIT;
			case COMMIT:
				return FlushModeType.COMMIT;
			case AUTO:
				return FlushModeType.AUTO;
			default:
				throw new AssertionFailure( "unhandled FlushMode " + flushMode );
		}
	}

	public static ForcedFlushMode getForcedFlushMode(FlushMode flushMode) {
		if ( flushMode == null ) {
			return ForcedFlushMode.NO_FORCING;
		}
		switch ( flushMode ) {
			case ALWAYS:
				return ForcedFlushMode.FORCE_FLUSH;
			case COMMIT:
			case MANUAL:
				return ForcedFlushMode.FORCE_NO_FLUSH;
			case AUTO:
				// this is not precisely correctly correct, but good enough
				return ForcedFlushMode.NO_FORCING;
			default:
				throw new AssertionFailure( "unhandled FlushMode " + flushMode );
		}
	}

	public static FlushMode getFlushMode(FlushModeType flushModeType) {
		if ( flushModeType == null ) {
			return null;
		}
		switch ( flushModeType ) {
			case AUTO:
				return FlushMode.AUTO;
			case COMMIT:
				return FlushMode.COMMIT;
			default:
				throw new AssertionFailure( "unhandled FlushModeType " + flushModeType );
		}
	}

	public static FlushMode getFlushMode(ForcedFlushMode forcedFlushMode) {
		if ( forcedFlushMode == null ) {
			return null;
		}
		switch ( forcedFlushMode ) {
			case FORCE_FLUSH:
				return FlushMode.ALWAYS;
			case FORCE_NO_FLUSH:
				return FlushMode.MANUAL;
			default:
				return null;
		}
	}

	public static FlushMode interpretFlushMode(Object value) {
		if ( value == null ) {
			return FlushMode.AUTO;
		}
		if (value instanceof FlushMode) {
			return (FlushMode) value;
		}
		else if (value instanceof FlushModeType) {
			return getFlushMode( (FlushModeType) value );
		}
		else if (value instanceof String) {
			return interpretExternalSetting( (String) value );
		}

		throw new IllegalArgumentException( "Unknown FlushMode source : " + value );

	}

	public static FlushMode interpretExternalSetting(String externalName) {
		if ( externalName == null ) {
			return null;
		}

		try {
			log.debugf( "Attempting to interpret external setting [%s] as FlushMode name", externalName );
			return FlushMode.valueOf( externalName.toUpperCase( Locale.ROOT) );
		}
		catch ( IllegalArgumentException e ) {
			log.debugf( "Attempting to interpret external setting [%s] as FlushModeType name", externalName );
		}

		try {
			return getFlushMode( FlushModeType.valueOf( externalName.toLowerCase( Locale.ROOT ) ) );
		}
		catch ( IllegalArgumentException ignore ) {
		}

		throw new MappingException( "unknown FlushMode : " + externalName );
	}
}
