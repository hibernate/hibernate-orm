/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.util;


import java.util.Locale;
import javax.persistence.FlushModeType;

import org.hibernate.AssertionFailure;
import org.hibernate.FlushMode;
import org.hibernate.MappingException;

import org.jboss.logging.Logger;

/**
 * Helper to deal with {@link FlushModeType} <-> {@link org.hibernate.FlushMode} conversions.
 *
 * @author Steve Ebersole
 */
public class FlushModeTypeHelper {
	private static final Logger log = Logger.getLogger( FlushModeTypeHelper.class );

	private FlushModeTypeHelper() {
	}

	public static FlushModeType getFlushModeType(FlushMode flushMode) {
		if ( flushMode == FlushMode.ALWAYS ) {
			log.debug( "Interpreting Hibernate FlushMode#ALWAYS to JPA FlushModeType#AUTO; may cause problems if relying on FlushMode#ALWAYS-specific behavior" );
			return FlushModeType.AUTO;
		}
		else if ( flushMode == FlushMode.MANUAL ) {
			log.debug( "Interpreting Hibernate FlushMode#MANUAL to JPA FlushModeType#COMMIT; may cause problems if relying on FlushMode#MANUAL-specific behavior" );
			return FlushModeType.COMMIT;
		}
		else if ( flushMode == FlushMode.COMMIT ) {
			return FlushModeType.COMMIT;
		}
		else if ( flushMode == FlushMode.AUTO ) {
			return FlushModeType.AUTO;
		}

		throw new AssertionFailure( "unhandled FlushMode " + flushMode );
	}

	public static FlushMode getFlushMode(FlushModeType flushModeType) {
		if ( flushModeType == FlushModeType.AUTO ) {
			return FlushMode.AUTO;
		}
		else if ( flushModeType == FlushModeType.COMMIT ) {
			return FlushMode.COMMIT;
		}

		throw new AssertionFailure( "unhandled FlushModeType " + flushModeType );
	}

	public static FlushMode interpretFlushMode(Object value) {
		if ( value == null ) {
			return FlushMode.AUTO;
		}
		if ( FlushMode.class.isInstance( value ) ) {
			return (FlushMode) value;
		}
		else if ( FlushModeType.class.isInstance( value ) ) {
			return getFlushMode( (FlushModeType) value );
		}
		else if ( String.class.isInstance( value ) ) {
			return interpretExternalSetting( (String) value );
		}

		throw new IllegalArgumentException( "Unknown FlushMode source : " + value );

	}

	public static FlushMode interpretExternalSetting(String externalName) {
		if ( externalName == null ) {
			return null;
		}

		try {
			log.debug( "Attempting to interpret external setting [" + externalName + "] as FlushMode name" );
			return FlushMode.valueOf( externalName.toUpperCase( Locale.ROOT) );
		}
		catch ( IllegalArgumentException e ) {
			log.debug( "Attempting to interpret external setting [" + externalName + "] as FlushModeType name" );
		}

		try {
			return getFlushMode( FlushModeType.valueOf( externalName.toLowerCase( Locale.ROOT ) ) );
		}
		catch ( IllegalArgumentException ignore ) {
		}

		throw new MappingException( "unknown FlushMode : " + externalName );
	}
}
