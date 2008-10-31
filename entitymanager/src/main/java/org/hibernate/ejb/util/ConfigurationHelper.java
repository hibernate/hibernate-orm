//$Id: $
package org.hibernate.ejb.util;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.persistence.FlushModeType;
import javax.persistence.PersistenceException;

import org.hibernate.FlushMode;
import org.hibernate.AssertionFailure;
import org.hibernate.CacheMode;

/**
 * @author Emmanuel Bernard
 */
public abstract class ConfigurationHelper {
	public static void overrideProperties(Properties properties, Map overrides) {
		for ( Map.Entry entry : (Set<Map.Entry>) overrides.entrySet() ) {
			if ( entry.getKey() instanceof String && entry.getValue() instanceof String ) {
				properties.setProperty( (String) entry.getKey(), (String) entry.getValue() );
			}
		}
	}

	public static FlushMode getFlushMode(Object value) {
		FlushMode flushMode = null;
		if (value instanceof FlushMode) {
			flushMode = (FlushMode) value;
		}
		else if (value instanceof javax.persistence.FlushModeType) {
			flushMode = ConfigurationHelper.getFlushMode( (javax.persistence.FlushModeType) value);
		}
		else if (value instanceof String) {
			flushMode = ConfigurationHelper.getFlushMode( (String) value);
		}
		if (flushMode == null) {
			throw new PersistenceException("Unable to parse org.hibernate.flushMode: " + value);
		}
		return flushMode;
	}

	private static FlushMode getFlushMode(String flushMode)  {
		if (flushMode == null) return null;
		flushMode = flushMode.toUpperCase();
		return FlushMode.parse( flushMode );
	}

	private static FlushMode getFlushMode(FlushModeType flushMode)  {
		switch(flushMode) {
			case AUTO:
				return FlushMode.AUTO;
			case COMMIT:
				return FlushMode.COMMIT;
			default:
				throw new AssertionFailure("Unknown FlushModeType: " + flushMode);
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
			return CacheMode.parse( (String) value );
		}
	}
}
