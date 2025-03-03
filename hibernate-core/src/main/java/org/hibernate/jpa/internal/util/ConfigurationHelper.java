/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal.util;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.PersistenceException;

import org.hibernate.AssertionFailure;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;

/**
 * @author Emmanuel Bernard
 */
public abstract class ConfigurationHelper {

	public static void overrideProperties(Properties properties, Map<?,?> overrides) {
		for ( Map.Entry<?,?> entry : overrides.entrySet() ) {
			if ( entry.getKey() != null && entry.getValue() != null ) {
				properties.put( entry.getKey(), entry.getValue() );
			}
		}
	}

	public static FlushMode getFlushMode(Object value, FlushMode defaultFlushMode) {
		if ( value instanceof FlushMode mode ) {
			return mode;
		}
		else if ( value instanceof jakarta.persistence.FlushModeType flushModeType ) {
			return getFlushMode( flushModeType );
		}
		else if ( value instanceof String string ) {
			return getFlushMode( string );
		}
		else {
			if ( defaultFlushMode == null ) {
				throw new PersistenceException( "Unable to parse org.hibernate.flushMode: " + value );
			}
			return defaultFlushMode;
		}
	}

	public static FlushMode getFlushMode(Object value) {
		return getFlushMode( value, null );
	}

	private static FlushMode getFlushMode(String flushMode)  {
		return flushMode == null ? null : FlushMode.valueOf( flushMode.toUpperCase(Locale.ROOT) );
	}

	private static FlushMode getFlushMode(FlushModeType flushMode)  {
		switch ( flushMode ) {
			case AUTO:
				return FlushMode.AUTO;
			case COMMIT:
				return FlushMode.COMMIT;
			default:
				throw new AssertionFailure( "Unknown FlushModeType: " + flushMode );
		}
	}

	public static Integer getInteger(Object value) {
		if ( value instanceof Integer integer ) {
			return integer;
		}
		else if ( value instanceof String string ) {
			return Integer.valueOf( string );
		}
		else {
			throw new IllegalArgumentException( "value must be a string or integer: " + value );
		}
	}

	public static Boolean getBoolean(Object value) {
		if ( value instanceof Boolean bool ) {
			return bool;
		}
		else if ( value instanceof String string ) {
			return Boolean.valueOf( string );
		}
		else {
			throw new IllegalArgumentException( "value must be a string or boolean: " + value );
		}
	}

	public static CacheMode getCacheMode(Object value) {
		if ( value instanceof CacheMode cacheMode ) {
			return cacheMode;
		}
		else if ( value instanceof String string ) {
			return CacheMode.valueOf( string );
		}
		else {
			throw new IllegalArgumentException( "value must be a string or CacheMode: " + value );
		}
	}
}
