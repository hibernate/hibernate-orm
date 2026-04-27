/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal.util;


import java.util.Locale;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.QueryFlushMode;

import org.hibernate.FlushMode;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

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
			case EXPLICIT ->  FlushMode.MANUAL;
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

	public static FlushMode interpretFlushMode(QueryFlushMode queryFlushMode) {
		if ( queryFlushMode == QueryFlushMode.DEFAULT ) {
			return null;
		}
		else if ( queryFlushMode == QueryFlushMode.NO_FLUSH ) {
			return FlushMode.MANUAL;
		}
		else {
			return FlushMode.AUTO;
		}
	}

	public static FlushMode interpretFlushMode(Object value) {
		if ( value == null ) {
			return FlushMode.AUTO;
		}
		if ( value instanceof FlushMode flushMode ) {
			return flushMode;
		}
		if ( value instanceof QueryFlushMode queryFlushMode ) {
			return interpretFlushMode( queryFlushMode );
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

	public static QueryFlushMode queryFlushModeFromHint(Object value) {
		if ( value == null ) {
			return QueryFlushMode.DEFAULT;
		}
		else if ( value instanceof QueryFlushMode qfm ) {
			return qfm;
		}
		else if ( value instanceof FlushMode fm ) {
			return queryFlushModeFromFlushMode( fm );
		}
		else if ( value instanceof FlushModeType fmt ) {
			return queryFlushModeFromFlushModeType( fmt );
		}
		else {
			return interpretQueryFlushMode( value.toString() );
		}
	}

	public static FlushMode toHibernateFlushMode(QueryFlushMode queryFlushMode, SharedSessionContractImplementor session) {
		if ( queryFlushMode == QueryFlushMode.DEFAULT ) {
			return session.getHibernateFlushMode();
		}
		else if ( queryFlushMode == QueryFlushMode.FLUSH ) {
			return FlushMode.AUTO;
		}
		else {
			return FlushMode.MANUAL;
		}
	}

	public static FlushModeType getFlushModeType(QueryFlushMode queryFlushMode) {
		if ( queryFlushMode == QueryFlushMode.DEFAULT ) {
			return FlushModeType.AUTO;
		}
		else {
			return FlushModeType.COMMIT;
		}
	}

	public static QueryFlushMode queryFlushModeFromFlushModeType(FlushModeType jpaMode) {
		if ( jpaMode == null ) {
			return QueryFlushMode.DEFAULT;
		}
		else if ( jpaMode == FlushModeType.COMMIT ) {
			return QueryFlushMode.NO_FLUSH;
		}
		else {
			return QueryFlushMode.FLUSH;
		}
	}

	public static QueryFlushMode queryFlushModeFromFlushMode(FlushMode hibernateMode) {
		if ( hibernateMode == null ) {
			return QueryFlushMode.DEFAULT;
		}
		else if ( hibernateMode == FlushMode.COMMIT || hibernateMode == FlushMode.MANUAL ) {
			return QueryFlushMode.NO_FLUSH;
		}
		else {
			return QueryFlushMode.FLUSH;
		}
	}

	public static QueryFlushMode interpretQueryFlushMode(String value) {
		if ( value == null ) {
			return QueryFlushMode.DEFAULT;
		}

		final String capitalizedValue = value.toUpperCase( Locale.ROOT );
		try {
			return QueryFlushMode.valueOf( capitalizedValue );
		}
		catch (IllegalArgumentException iae) {
			// fall through
		}

		try {
			return queryFlushModeFromFlushMode( FlushMode.valueOf( capitalizedValue ) );
		}
		catch (IllegalArgumentException iae) {
			// fall through
		}

		try {
			return queryFlushModeFromFlushModeType( FlushModeType.valueOf( capitalizedValue ) );
		}
		catch (IllegalArgumentException iae) {
			// fall through
		}

		throw new IllegalArgumentException( "Incoming value could not be interpreted as a QueryFlushMode : " + value );
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
