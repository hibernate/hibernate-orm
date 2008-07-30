/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Iterator;

/**
 * Collection of helper methods for dealing with {@link java.util.Properties}
 * objects.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class PropertiesHelper {

	private static final String PLACEHOLDER_START = "${";

	/**
	 * Disallow instantiation
	 */
	private PropertiesHelper() {
	}

	/**
	 * Get a property value as a string.
	 *
	 * @see #extractPropertyValue(String, java.util.Properties)
	 *
	 * @param propertyName The name of the property for which to retrieve value
	 * @param properties The properties object
	 * @param defaultValue The default property value to use.
	 * @return The property value; may be null.
	 */
	public static String getString(String propertyName, Properties properties, String defaultValue) {
		String value = extractPropertyValue( propertyName, properties );
		return value == null ? defaultValue : value;
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
	 * Get a property value as a boolean.  Shorthand for calling
	 * {@link #getBoolean(String, java.util.Properties, boolean)} with <tt>false</tt>
	 * as the default value.
	 *
	 * @param propertyName The name of the property for which to retrieve value
	 * @param properties The properties object
	 * @return The property value.
	 */
	public static boolean getBoolean(String propertyName, Properties properties) {
		return getBoolean( propertyName, properties, false );
	}

	/**
	 * Get a property value as a boolean.
	 * <p/>
	 * First, the string value is extracted, and then {@link Boolean#valueOf(String)} is
	 * used to determine the correct boolean value.
	 *
	 * @see #extractPropertyValue(String, java.util.Properties)
	 *
	 * @param propertyName The name of the property for which to retrieve value
	 * @param properties The properties object
	 * @param defaultValue The default property value to use.
	 * @return The property value.
	 */
	public static boolean getBoolean(String propertyName, Properties properties, boolean defaultValue) {
		String value = extractPropertyValue( propertyName, properties );
		return value == null ? defaultValue : Boolean.valueOf( value ).booleanValue();
	}

	/**
	 * Get a property value as an int.
	 * <p/>
	 * First, the string value is extracted, and then {@link Integer#parseInt(String)} is
	 * used to determine the correct int value for any non-null property values.
	 *
	 * @see #extractPropertyValue(String, java.util.Properties)
	 *
	 * @param propertyName The name of the property for which to retrieve value
	 * @param properties The properties object
	 * @param defaultValue The default property value to use.
	 * @return The property value.
	 */
	public static int getInt(String propertyName, Properties properties, int defaultValue) {
		String value = extractPropertyValue( propertyName, properties );
		return value == null ? defaultValue : Integer.parseInt( value );
	}

	/**
	 * Get a property value as an Integer.
	 * <p/>
	 * First, the string value is extracted, and then {@link Integer#valueOf(String)} is
	 * used to determine the correct boolean value for any non-null property values.
	 *
	 * @see #extractPropertyValue(String, java.util.Properties)
	 *
	 * @param propertyName The name of the property for which to retrieve value
	 * @param properties The properties object
	 * @return The property value; may be null.
	 */
	public static Integer getInteger(String propertyName, Properties properties) {
		String value = extractPropertyValue( propertyName, properties );
		return value == null ? null : Integer.valueOf( value );
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
	 * replace a property by a starred version
	 *
	 * @param props properties to check
	 * @param key proeprty to mask
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
	 * Handles interpolation processing for all entries in a properties object.
	 *
	 * @param properties The properties object.
	 */
	public static void resolvePlaceHolders(Properties properties) {
		Iterator itr = properties.entrySet().iterator();
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
		StringBuffer buff = new StringBuffer();
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
