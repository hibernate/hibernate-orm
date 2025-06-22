/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.Incubating;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;

import static org.hibernate.internal.log.IncubationLogger.INCUBATION_LOGGER;

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
	public static String getString(String name, Map<?,?> values) {
		final Object value = values.get( name );
		return value == null ? null : value.toString();
	}

	/**
	 * Get the config value as a {@link String}
	 *
	 * @param preferred The preferred config setting name.
	 * @param fallback The fallback config setting name, when the preferred
	 *                 configuration is not set.
	 * @param values The map of config values
	 *
	 * @return The value, or null if not found
	 */
	public static String getString(String preferred, String fallback, Map<?,?> values) {
		final String preferredValue = getString( preferred, values );
		return preferredValue == null ? getString( fallback, values ) : preferredValue;
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
	public static String getString(String name, Map<?,?> values, String defaultValue) {
		return getString( name, values, () -> defaultValue );
	}

	/**
	 * Get the config value as a {@link String}
	 *
	 * @param name The config setting name.
	 * @param values The map of config values
	 *
	 * @return The value, or null if not found
	 */
	public static String getString(String name, Map<?,?> values, Supplier<String> defaultValueSupplier) {
		final Object value = values.get( name );
		return value != null ? value.toString() : defaultValueSupplier.get();
	}

	/**
	 * Get the config value as a boolean (default of false)
	 *
	 * @param name The config setting name.
	 * @param values The map of config values
	 *
	 * @return The value.
	 */
	public static boolean getBoolean(String name, Map<?,?> values) {
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
	public static boolean getBoolean(String name, Map<?,?> values, boolean defaultValue) {
		final Object raw = values.get( name );
		final Boolean value = toBoolean( raw, defaultValue );
		if ( value == null ) {
			throw new ConfigurationException(
					"Could not determine how to handle configuration raw [name=" + name + ", value=" + raw + "] as boolean"
			);
		}
		else {
			return value;
		}
	}

	public static Boolean toBoolean(Object value, boolean defaultValue) {
		if ( value == null ) {
			return defaultValue;
		}
		else if (value instanceof Boolean bool) {
			return bool;
		}
		else if (value instanceof String string) {
			return Boolean.parseBoolean(string);
		}
		else {
			return null;
		}
	}

	/**
	 * Get the config value as a boolean (default of false)
	 *
	 * @param name The config setting name.
	 * @param values The map of config values
	 *
	 * @return The value.
	 */
	public static Boolean getBooleanWrapper(String name, Map<?,?> values, Boolean defaultValue) {
		final Object value = values.get( name );
		if ( value == null ) {
			return defaultValue;
		}
		else if (value instanceof Boolean bool) {
			return bool;
		}
		else if (value instanceof String string) {
			return Boolean.valueOf(string);
		}
		else {
			throw new ConfigurationException(
					"Could not determine how to handle configuration value [name=" + name + ", value=" + value + "] as boolean"
			);
		}
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
	public static int getInt(String name, Map<?,?> values, int defaultValue) {
		final Object value = values.get( name );
		if ( value == null ) {
			return defaultValue;
		}
		if (value instanceof Integer integer) {
			return integer;
		}
		if (value instanceof String string) {
			return Integer.parseInt(string);
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
	public static Integer getInteger(String name, Map<?,?> values) {
		final Object value = values.get( name );
		if ( value == null ) {
			return null;
		}
		else if (value instanceof Integer integer) {
			return integer;
		}
		else if (value instanceof String string) {
			//empty values are ignored
			final String trimmed = string.trim();
			return trimmed.isEmpty() ? null : Integer.valueOf( trimmed );
		}
		throw new ConfigurationException(
				"Could not determine how to handle configuration value [name=" + name +
						", value=" + value + "(" + value.getClass().getName() + ")] as Integer"
		);
	}

	public static long getLong(String name, Map<?,?> values, int defaultValue) {
		final Object value = values.get( name );
		if ( value == null ) {
			return defaultValue;
		}
		else if (value instanceof Long number) {
			return number;
		}
		else if (value instanceof String string) {
			return Long.parseLong(string);
		}
		else {
			throw new ConfigurationException(
					"Could not determine how to handle configuration value [name=" + name +
							", value=" + value + "(" + value.getClass().getName() + ")] as long"
			);
		}
	}

	/**
	 * Make a clone of the configuration values.
	 *
	 * @param configurationValues The config values to clone
	 *
	 * @return The clone
	 *
	 * @deprecated No longer used
	 */
	@SuppressWarnings("rawtypes")
	@Deprecated(since = "7", forRemoval = true)
	public static Map clone(Map<?,?> configurationValues) {
		if ( configurationValues == null ) {
			return null;
		}
		else if ( configurationValues instanceof Properties properties ) {
			// If a Properties object, leverage its clone() impl
			return (Properties) properties.clone();
		}
		else {
			// Otherwise make a manual copy
			return new HashMap<>( configurationValues );
		}
	}

	/**
	 * replace a property by a starred version
	 *
	 * @param props properties to check
	 * @param key property to mask
	 *
	 * @return cloned and masked properties
	 */
	public static Properties maskOut(Properties props, String key) {
		final Properties clone = (Properties) props.clone();
		if ( clone.get( key ) != null ) {
			clone.setProperty( key, "****" );
		}
		return clone;
	}

	/**
	 * Extract a property value by name from the given properties object.
	 * <p>
	 * Both {@code null} and {@code empty string} are viewed as the same, and return null.
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
		if ( value.isEmpty() ) {
			return null;
		}
		return value;
	}
	/**
	 * Extract a property value by name from the given properties object.
	 * <p>
	 * Both {@code null} and {@code empty string} are viewed as the same, and return null.
	 *
	 * @param propertyName The name of the property for which to extract value
	 * @param properties The properties object
	 * @return The property value; may be null.
	 */
	public static String extractPropertyValue(String propertyName, Map<?,?> properties) {
		String value = (String) properties.get( propertyName );
		if ( value == null ) {
			return null;
		}
		value = value.trim();
		if ( value.isEmpty() ) {
			return null;
		}
		return value;
	}

	public static String extractValue(
			String name,
			Map<?,?> values,
			Supplier<String> fallbackValueFactory) {
		final String value = extractPropertyValue( name, values );
		if ( value != null ) {
			return value;
		}

		return fallbackValueFactory.get();
	}

	/**
	 * Constructs a map from a property value.
	 * <p>
	 * The exact behavior here is largely dependant upon what is passed in as
	 * the delimiter.
	 *
	 * @see #extractPropertyValue(String, Properties)
	 *
	 * @param propertyName The name of the property for which to retrieve value
	 * @param delim The string defining tokens used as both entry and key/value delimiters.
	 * @param properties The properties object
	 * @return The resulting map; never null, though perhaps empty.
	 *
	 * @deprecated No longer used
	 */
	@SuppressWarnings("rawtypes")
	@Deprecated(since = "7", forRemoval = true)
	public static Map toMap(String propertyName, String delim, Properties properties) {
		Map<String,String> map = new HashMap<>();
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
	 * <p>
	 * The exact behavior here is largely dependant upon what is passed in as
	 * the delimiter.
	 *
	 * @see #extractPropertyValue(String, Properties)
	 *
	 * @param propertyName The name of the property for which to retrieve value
	 * @param delim The string defining tokens used as both entry and key/value delimiters.
	 * @param properties The properties object
	 * @return The resulting map; never null, though perhaps empty.
	 *
	 * @deprecated No longer used
	 */
	@SuppressWarnings("rawtypes")
	@Deprecated(since = "7", forRemoval = true)
	public static Map toMap(String propertyName, String delim, Map<?,?> properties) {
		Map<String,String> map = new HashMap<>();
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
	 * @see #extractPropertyValue(String, Properties)
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
	public static void resolvePlaceHolders(Map<?,Object> configurationValues) {
		final Iterator<? extends Map.Entry<?,Object>> itr = configurationValues.entrySet().iterator();
		while ( itr.hasNext() ) {
			final Map.Entry<?,Object> entry = itr.next();
			final Object value = entry.getValue();
			if ( value instanceof String string ) {
				final String resolved = resolvePlaceHolder( string );
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
		if ( !property.contains( PLACEHOLDER_START ) ) {
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
					final String systemProperty = extractFromSystem( systemPropertyName );
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
		final String result = buff.toString();
		return result.isEmpty() ? null : result;
	}

	private static String extractFromSystem(String systemPropertyName) {
		try {
			return System.getProperty( systemPropertyName );
		}
		catch( Throwable t ) {
			return null;
		}
	}

	@Incubating
	public static synchronized int getPreferredSqlTypeCodeForBoolean(StandardServiceRegistry serviceRegistry) {
		final Integer typeCode = serviceRegistry.requireService( ConfigurationService.class ).getSetting(
				AvailableSettings.PREFERRED_BOOLEAN_JDBC_TYPE,
				TypeCodeConverter.INSTANCE
		);
		if ( typeCode != null ) {
			INCUBATION_LOGGER.incubatingSetting( AvailableSettings.PREFERRED_BOOLEAN_JDBC_TYPE );
			return typeCode;
		}

		// default to the Dialect answer
		return serviceRegistry.requireService( JdbcServices.class )
				.getJdbcEnvironment()
				.getDialect()
				.getPreferredSqlTypeCodeForBoolean();
	}

	@Incubating
	public static synchronized int getPreferredSqlTypeCodeForBoolean(ServiceRegistry serviceRegistry, Dialect dialect) {
		final Integer typeCode = serviceRegistry.requireService( ConfigurationService.class ).getSetting(
				AvailableSettings.PREFERRED_BOOLEAN_JDBC_TYPE,
				TypeCodeConverter.INSTANCE
		);
		if ( typeCode != null ) {
			INCUBATION_LOGGER.incubatingSetting( AvailableSettings.PREFERRED_BOOLEAN_JDBC_TYPE );
			return typeCode;
		}

		// default to the Dialect answer
		return dialect.getPreferredSqlTypeCodeForBoolean();
	}

	@Incubating
	public static synchronized int getPreferredSqlTypeCodeForDuration(StandardServiceRegistry serviceRegistry) {
		final Integer explicitSetting = serviceRegistry.requireService( ConfigurationService.class ).getSetting(
				AvailableSettings.PREFERRED_DURATION_JDBC_TYPE,
				TypeCodeConverter.INSTANCE
		);
		if ( explicitSetting != null ) {
			INCUBATION_LOGGER.incubatingSetting( AvailableSettings.PREFERRED_DURATION_JDBC_TYPE );
			return explicitSetting;
		}

		return SqlTypes.DURATION;
	}

	@Incubating
	public static synchronized int getPreferredSqlTypeCodeForUuid(StandardServiceRegistry serviceRegistry) {
		final Integer explicitSetting = serviceRegistry.requireService( ConfigurationService.class ).getSetting(
				AvailableSettings.PREFERRED_UUID_JDBC_TYPE,
				TypeCodeConverter.INSTANCE
		);
		if ( explicitSetting != null ) {
			INCUBATION_LOGGER.incubatingSetting( AvailableSettings.PREFERRED_UUID_JDBC_TYPE );
			return explicitSetting;
		}

		return SqlTypes.UUID;
	}

	@Incubating
	public static synchronized int getPreferredSqlTypeCodeForInstant(StandardServiceRegistry serviceRegistry) {
		final Integer explicitSetting = serviceRegistry.requireService( ConfigurationService.class ).getSetting(
				AvailableSettings.PREFERRED_INSTANT_JDBC_TYPE,
				TypeCodeConverter.INSTANCE
		);
		if ( explicitSetting != null ) {
			INCUBATION_LOGGER.incubatingSetting( AvailableSettings.PREFERRED_INSTANT_JDBC_TYPE );
			return explicitSetting;
		}

		return SqlTypes.TIMESTAMP_UTC;
	}

	@Incubating
	public static synchronized int getPreferredSqlTypeCodeForArray(StandardServiceRegistry serviceRegistry) {
		final Integer explicitSetting = serviceRegistry.requireService( ConfigurationService.class ).getSetting(
				AvailableSettings.PREFERRED_ARRAY_JDBC_TYPE,
				TypeCodeConverter.INSTANCE
		);
		if ( explicitSetting != null ) {
			INCUBATION_LOGGER.incubatingSetting( AvailableSettings.PREFERRED_ARRAY_JDBC_TYPE );
			return explicitSetting;
		}
		// default to the Dialect answer
		return serviceRegistry.requireService( JdbcServices.class )
				.getJdbcEnvironment()
				.getDialect()
				.getPreferredSqlTypeCodeForArray();
	}

	public static void setIfNotEmpty(String value, String settingName, Map<String, String> configuration) {
		if ( StringHelper.isNotEmpty( value ) ) {
			configuration.put( settingName, value );
		}
	}

	@Deprecated(since = "7", forRemoval = true)
	public static void setIfNotNull(Object value, String settingName, Map<String, Object> configuration) {
		if ( value != null ) {
			configuration.put( settingName, value );
		}
	}

	private static class TypeCodeConverter implements ConfigurationService.Converter<Integer> {

		public static final TypeCodeConverter INSTANCE = new TypeCodeConverter();

		@Override
		@NonNull
		public Integer convert(Object value) {
			if ( value instanceof Number number ) {
				return number.intValue();
			}

			final String string = value.toString().toUpperCase( Locale.ROOT );
			final Integer typeCode = JdbcTypeNameMapper.getTypeCode( string );
			if ( typeCode != null ) {
				return typeCode;
			}
			try {
				return Integer.parseInt( string );
			}
			catch (NumberFormatException ex) {
				throw new IllegalArgumentException( String.format( "Couldn't interpret '%s' as JDBC type code or type code name", string ) );
			}
		}
	}
}
