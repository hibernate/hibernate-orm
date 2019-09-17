/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.util;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceException;

import org.hibernate.AssertionFailure;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;

/**
 * @author Emmanuel Bernard
 */
public abstract class ConfigurationHelper {
	public static void overrideProperties(Properties properties, Map<?,?> overrides) {
		for ( Map.Entry entry : overrides.entrySet() ) {
			if ( entry.getKey() != null && entry.getValue() != null ) {
				properties.put( entry.getKey(), entry.getValue() );
			}
		}
	}

	public static FlushMode getFlushMode(Object value, FlushMode defaultFlushMode) {
		final FlushMode flushMode;
		if ( value instanceof FlushMode ) {
			flushMode = (FlushMode) value;
		}
		else if ( value instanceof javax.persistence.FlushModeType ) {
			flushMode = ConfigurationHelper.getFlushMode( (javax.persistence.FlushModeType) value );
		}
		else if ( value instanceof String ) {
			flushMode = ConfigurationHelper.getFlushMode( (String) value );
		}
		else {
			flushMode = defaultFlushMode;
		}

		if ( flushMode == null ) {
			throw new PersistenceException( "Unable to parse org.hibernate.flushMode: " + value );
		}

		return flushMode;
	}

	public static FlushMode getFlushMode(Object value) {
		return getFlushMode( value, null );
	}

	private static FlushMode getFlushMode(String flushMode)  {
		if ( flushMode == null ) {
			return null;
		}
		flushMode = flushMode.toUpperCase( Locale.ROOT );
		return FlushMode.valueOf( flushMode );
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
		if ( value instanceof Integer ) {
			return (Integer) value;
		}
		else {
			return Integer.valueOf( (String) value );
		}
	}

	public static Boolean getBoolean(Object value) {
		if ( value instanceof Boolean ) {
			return (Boolean) value;
		}
		else {
			return Boolean.valueOf( (String) value );
		}
	}

	public static CacheMode getCacheMode(Object value) {
		if ( value instanceof CacheMode ) {
			return (CacheMode) value;
		}
		else {
			return CacheMode.valueOf( (String) value );
		}
	}
}
