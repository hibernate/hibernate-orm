/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;

/**
 * Collection of helper methods for dealing with configuration settings.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class ConfigurationHelper {

	private static final String PLACEHOLDER_START = "${";

	/**
	 * Disallow instantiation
	 */
	private ConfigurationHelper() {
	}

	/**
	 * Get the config value as a {@link String}
	 *
	 * @param name The config setting name.
	 * @param values The map of config values
	 *
	 * @return The value, or null if not found
	 */
	public static String getString(String name, Map values) {
		Object value = values.get( name );
		if ( value == null ) {
			return null;
		}
		if ( String.class.isInstance( value ) ) {
			return (String) value;
		}
		return value.toString();
	}

	/**
	 * Get the config value as a {@link String}
	 *
	 * @param name The config setting name.
	 * @param values The map of config values
	 * @param defaultValue The default value to use if not found
	 *
	 * @return The value.
	 */
	public static String getString(String name, Map values, String defaultValue) {
		final String value = getString( name, values );
		return value == null ? defaultValue : value;
	}

	/**
	 * Get the config value as a {@link String}.
	 *
	 * @param name The config setting name.
	 * @param values The map of config parameters.
	 * @param defaultValue The default value to use if not found.
	 * @param otherSupportedValues List of other supported values. Does not need to contain the default one.
	 *
	 * @return The value.
	 *
	 * @throws ConfigurationException Unsupported value provided.
	 *
	 */
	public static String getString(String name, Map values, String defaultValue, String ... otherSupportedValues) {
		final String value = getString( name, values, defaultValue );
		if ( !defaultValue.equals( value ) && ArrayHelper.indexOf( otherSupportedValues, value ) == -1 ) {
			throw new ConfigurationException(
					"Unsupported configuration [name=" + name + ", value=" + value + "]. " +
							"Choose value between: '" + defaultValue + "', '" + StringHelper.join( "', '", otherSupportedValues ) + "'."
			);
		}
		return value;
	}

	/**
	 * Get the config value as a boolean (default of false)
	 *
	 * @param name The config setting name.
	 * @param values The map of config values
	 *
	 * @return The value.
	 */
	public static boolean getBoolean(String name, Map values) {
		return getBoolean( name, values, false );
	}

	/**
	 * Get the config value as a boolean.
	 *
	 * @param name The config setting name.
	 * @param values The map of config values
	 * @param defaultValue The default value to use if not found
	 *
	 * @return The value.
	 */
	public static boolean getBoolean(String name, Map values, boolean defaultValue) {
		Object value = values.get( name );
		if ( value == null ) {
			return defaultValue;
		}
		if ( Boolean.class.isInstance( value ) ) {
			return ( (Boolean) value ).booleanValue();
		}
		if ( String.class.isInstance( value ) ) {
			return Boolean.parseBoolean( (String) value );
		}
		throw new ConfigurationException(
				"Could not determine how to handle configuration value [name=" + name + ", value=" + value + "] as boolean"
		);
	}

	/**
	 * Get the config value as a boolean (default of false)
	 *
	 * @param name The config setting name.
	 * @param values The map of config values
	 *
	 * @return The value.
	 */
	public static Boolean getBooleanWrapper(String name, Map values, Boolean defaultValue) {
		Object value = values.get( name );
		if ( value == null ) {
			return defaultValue;
		}
		if ( Boolean.class.isInstance( value ) ) {
			return (Boolean) value;
		}
		if ( String.class.isInstance( value ) ) {
			return Boolean.valueOf( (String) value );
		}
		throw new ConfigurationException(
				"Could not determine how to handle configuration value [name=" + name + ", value=" + value + "] as boolean"
		);
	}

	/**
	 * Get the config value as an int
	 *
	 * @param name The config setting name.
	 * @param values The map of config values
	 * @param defaultValue The default value to use if not found
	 *
	 * @return The value.
	 */
	public static int getInt(String name, Map values, int defaultValue) {
		Object value = values.get( name );
		if ( value == null ) {
			return defaultValue;
		}
		if ( Integer.class.isInstance( value ) ) {
			return (Integer) value;
		}
		if ( String.class.isInstance( value ) ) {
			return Integer.parseInt( (String) value );
		}
		throw new ConfigurationException(
				"Could not determine how to handle configuration value [name=" + name +
						", value=" + value + "(" + value.getClass().getName() + ")] as int"
		);
	}

	/**
	 * Get the config value as an {@link Integer}
	 *
	 * @param name The config setting name.
	 * @param values The map of config values
	 *
	 * @return The value, or null if not found
	 */
	public static Integer getInteger(String name, Map values) {
		Object value = values.get( name );
		if ( value == null ) {
			return null;
		}
		if ( Integer.class.isInstance( value ) ) {
			return (Integer) value;
		}
		if ( String.class.isInstance( value ) ) {
			//empty values are ignored
			final String trimmed = value.toString().trim();
			if ( trimmed.isEmpty() ) {
				return null;
			}
			return Integer.valueOf( trimmed );
		}
		throw new ConfigurationException(
				"Could not determine how to handle configuration value [name=" + name +
						", value=" + value + "(" + value.getClass().getName() + ")] as Integer"
		);
	}

	public static long getLong(String name, Map values, int defaultValue) {
		Object value = values.get( name );
		if ( value == null ) {
			return defaultValue;
		}
		if ( Long.class.isInstance( value ) ) {
			return (Long) value;
		}
		if ( String.class.isInstance( value ) ) {
			return Long.parseLong( (String) value );
		}
		throw new ConfigurationException(
				"Could not determine how to handle configuration value [name=" + name +
						", value=" + value + "(" + value.getClass().getName() + ")] as long"
		);
	}

	/**
	 * Make a clone of the configuration values.
	 *
	 * @param configurationValues The config values to clone
	 *
	 * @return The clone
	 */
	@SuppressWarnings({ "unchecked" })
	public static Map clone(Map<?,?> configurationValues) {
		if ( configurationValues == null ) {
			return null;
		}
		// If a Properties object, leverage its clone() impl
		if ( Properties.class.isInstance( configurationValues ) ) {
			return (Properties) ( (Properties) configurationValues ).clone();
		}
		// Otherwise make a manual copy
		HashMap clone = new HashMap();
		for ( Map.Entry entry : configurationValues.entrySet() ) {
			clone.put( entry.getKey(), entry.getValue() );
		}
		return clone;
	}



	/**
	 * replace a property by a starred version
	 *
	 * @param props properties to check
	 * @param key proeprty to mask
	 *
	 * @return cloned and masked properties
	 */
	public static Properties maskOut(Properties props, String key) {
		Properties clone = ( Properties ) props.clone();
		if ( clone.get( key ) != null ) {
			clone.setProperty( key, "****" );
		}
		return clone;
	}





	/**
	 * Extract a property value by name from the given properties object.
	 * <p/>
	 * Both <tt>null</tt> and <tt>empty string</tt> are viewed as the same, and return null.
	 *
	 * @param propertyName The name of the property for which to extract value
	 * @param properties The properties object
	 * @return The property value; may be null.
	 */
	public static String extractPropertyValue(String propertyName, Properties properties) {
		String value = properties.getProperty( propertyName );
		if ( value == null ) {
			return null;
		}
		value = value.trim();
		if ( StringHelper.isEmpty( value ) ) {
			return null;
		}
		return value;
	}
	/**
	 * Extract a property value by name from the given properties object.
	 * <p/>
	 * Both <tt>null</tt> and <tt>empty string</tt> are viewed as the same, and return null.
	 *
	 * @param propertyName The name of the property for which to extract value
	 * @param properties The properties object
	 * @return The property value; may be null.
	 */
	public static String extractPropertyValue(String propertyName, Map properties) {
		String value = (String) properties.get( propertyName );
		if ( value == null ) {
			return null;
		}
		value = value.trim();
		if ( StringHelper.isEmpty( value ) ) {
			return null;
		}
		return value;
	}

	/**
	 * Constructs a map from a property value.
	 * <p/>
	 * The exact behavior here is largely dependant upon what is passed in as
	 * the delimiter.
	 *
	 * @see #extractPropertyValue(String, java.util.Properties)
	 *
	 * @param propertyName The name of the property for which to retrieve value
	 * @param delim The string defining tokens used as both entry and key/value delimiters.
	 * @param properties The properties object
	 * @return The resulting map; never null, though perhaps empty.
	 */
	public static Map toMap(String propertyName, String delim, Properties properties) {
		Map map = new HashMap();
		String value = extractPropertyValue( propertyName, properties );
		if ( value != null ) {
			StringTokenizer tokens = new StringTokenizer( value, delim );
			while ( tokens.hasMoreTokens() ) {
				map.put( tokens.nextToken(), tokens.hasMoreElements() ? tokens.nextToken() : "" );
			}
		}
		return map;
	}

	/**
	 * Constructs a map from a property value.
	 * <p/>
	 * The exact behavior here is largely dependant upon what is passed in as
	 * the delimiter.
	 *
	 * @see #extractPropertyValue(String, java.util.Properties)
	 *
	 * @param propertyName The name of the property for which to retrieve value
	 * @param delim The string defining tokens used as both entry and key/value delimiters.
	 * @param properties The properties object
	 * @return The resulting map; never null, though perhaps empty.
	 */
	public static Map toMap(String propertyName, String delim, Map properties) {
		Map map = new HashMap();
		String value = extractPropertyValue( propertyName, properties );
		if ( value != null ) {
			StringTokenizer tokens = new StringTokenizer( value, delim );
			while ( tokens.hasMoreTokens() ) {
				map.put( tokens.nextToken(), tokens.hasMoreElements() ? tokens.nextToken() : "" );
			}
		}
		return map;
	}

	/**
	 * Get a property value as a string array.
	 *
	 * @see #extractPropertyValue(String, java.util.Properties)
	 * @see #toStringArray(String, String)
	 *
	 * @param propertyName The name of the property for which to retrieve value
	 * @param delim The delimiter used to separate individual array elements.
	 * @param properties The properties object
	 * @return The array; never null, though may be empty.
	 */
	public static String[] toStringArray(String propertyName, String delim, Properties properties) {
		return toStringArray( extractPropertyValue( propertyName, properties ), delim );
	}

	/**
	 * Convert a string to an array of strings.  The assumption is that
	 * the individual array elements are delimited in the source stringForm
	 * param by the delim param.
	 *
	 * @param stringForm The string form of the string array.
	 * @param delim The delimiter used to separate individual array elements.
	 * @return The array; never null, though may be empty.
	 */
	public static String[] toStringArray(String stringForm, String delim) {
		// todo : move to StringHelper?
		if ( stringForm != null ) {
			return StringHelper.split( delim, stringForm );
		}
		else {
			return ArrayHelper.EMPTY_STRING_ARRAY;
		}
	}

	/**
	 * Handles interpolation processing for all entries in a properties object.
	 *
	 * @param configurationValues The configuration map.
	 */
	public static void resolvePlaceHolders(Map<?,?> configurationValues) {
		Iterator itr = configurationValues.entrySet().iterator();
		while ( itr.hasNext() ) {
			final Map.Entry entry = ( Map.Entry ) itr.next();
			final Object value = entry.getValue();
			if ( value != null && String.class.isInstance( value ) ) {
				final String resolved = resolvePlaceHolder( ( String ) value );
				if ( !value.equals( resolved ) ) {
					if ( resolved == null ) {
						itr.remove();
					}
					else {
						entry.setValue( resolved );
					}
				}
			}
		}
	}

	/**
	 * Handles interpolation processing for a single property.
	 *
	 * @param property The property value to be processed for interpolation.
	 * @return The (possibly) interpolated property value.
	 */
	public static String resolvePlaceHolder(String property) {
		if ( property.indexOf( PLACEHOLDER_START ) < 0 ) {
			return property;
		}
		StringBuilder buff = new StringBuilder();
		char[] chars = property.toCharArray();
		for ( int pos = 0; pos < chars.length; pos++ ) {
			if ( chars[pos] == '$' ) {
				// peek ahead
				if ( chars[pos+1] == '{' ) {
					// we have a placeholder, spin forward till we find the end
					String systemPropertyName = "";
					int x = pos + 2;
					for (  ; x < chars.length && chars[x] != '}'; x++ ) {
						systemPropertyName += chars[x];
						// if we reach the end of the string w/o finding the
						// matching end, that is an exception
						if ( x == chars.length - 1 ) {
							throw new IllegalArgumentException( "unmatched placeholder start [" + property + "]" );
						}
					}
					String systemProperty = extractFromSystem( systemPropertyName );
					buff.append( systemProperty == null ? "" : systemProperty );
					pos = x + 1;
					// make sure spinning forward did not put us past the end of the buffer...
					if ( pos >= chars.length ) {
						break;
					}
				}
			}
			buff.append( chars[pos] );
		}
		String rtn = buff.toString();
		return StringHelper.isEmpty( rtn ) ? null : rtn;
	}

	private static String extractFromSystem(String systemPropertyName) {
		try {
			return System.getProperty( systemPropertyName );
		}
		catch( Throwable t ) {
			return null;
		}
	}
}
