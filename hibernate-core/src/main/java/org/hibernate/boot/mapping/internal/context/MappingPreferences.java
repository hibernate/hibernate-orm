/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import org.hibernate.Incubating;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.util.config.ConfigurationHelper;


import static org.hibernate.cfg.MappingSettings.JAVA_TIME_USE_DIRECT_JDBC;
import static org.hibernate.cfg.MappingSettings.PREFER_LOCALE_LANGUAGE_TAG;
import static org.hibernate.cfg.MappingSettings.PREFER_NATIVE_ENUM_TYPES;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;

/// Mapping interpretation preferences resolved from configuration and dialect
/// defaults.
///
/// @since 9.0
/// @author Steve Ebersole
@Incubating
public interface MappingPreferences {
	/// The preferred JDBC type for storing boolean values.
	///
	/// @see org.hibernate.cfg.MappingSettings#PREFERRED_BOOLEAN_JDBC_TYPE
	int getPreferredSqlTypeCodeForBoolean();

	/// The preferred JDBC type for storing Duration values.
	///
	/// @see MappingSettings#PREFERRED_DURATION_JDBC_TYPE
	int getPreferredSqlTypeCodeForDuration();

	/// The preferred JDBC type for storing UUID values.
	///
	/// @see MappingSettings#PREFERRED_UUID_JDBC_TYPE
	int getPreferredSqlTypeCodeForUuid();

	/// The preferred JDBC type for storing Instant values.
	///
	/// @see MappingSettings#PREFERRED_INSTANT_JDBC_TYPE
	int getPreferredSqlTypeCodeForInstant();

	/// The preferred JDBC type for storing basic plural (i.e. array/collection) values.
	///
	/// @see MappingSettings#PREFERRED_ARRAY_JDBC_TYPE
	int getPreferredSqlTypeCodeForArray();

	/// Whether to use {@linkplain java.time Java Time} references directly at the JDBC boundary
	/// for binding and extracting values.
	///
	/// Note that this relies on the JDBC driver properly supporting implicit handling of these
	/// values.  This is support added in JDBC 4.2, though "hidden" behind `ResultSet#getObject`
	/// and `PreparedStatement#setObject`.
	///
	/// @see MappingSettings#JAVA_TIME_USE_DIRECT_JDBC
	boolean isPreferJavaTimeJdbcTypesEnabled();

	/// Whether named SQL `enum` types should be used for storing named [Enum] types.
	///
	/// @see MappingSettings#PREFER_NATIVE_ENUM_TYPES
	boolean isPreferNativeEnumTypesEnabled();

	/// Whether [java.util.Locale#toLanguageTag()] should be preferred over [java.util.Locale#toString()] when converting
	/// a [java.util.Locale] to a [String].
	///
	/// @see MappingSettings#PREFER_LOCALE_LANGUAGE_TAG
	boolean isPreferLocaleLanguageTagEnabled();

	static MappingPreferences from(StandardServiceRegistry serviceRegistry) {
		final var configurationService = serviceRegistry.requireService( ConfigurationService.class );
		final var dialect = serviceRegistry.requireService( JdbcServices.class ).getDialect();
		return from( configurationService, dialect );
	}

	static MappingPreferences from(ConfigurationService configurationService, Dialect dialect) {
		return new MappingPreferences() {
			@Override
			public int getPreferredSqlTypeCodeForBoolean() {
				return ConfigurationHelper.getPreferredSqlTypeCodeForBoolean( configurationService, dialect );
			}

			@Override
			public int getPreferredSqlTypeCodeForDuration() {
				return ConfigurationHelper.getPreferredSqlTypeCodeForDuration( configurationService );
			}

			@Override
			public int getPreferredSqlTypeCodeForUuid() {
				return ConfigurationHelper.getPreferredSqlTypeCodeForUuid( configurationService );
			}

			@Override
			public int getPreferredSqlTypeCodeForInstant() {
				return ConfigurationHelper.getPreferredSqlTypeCodeForInstant( configurationService );
			}

			@Override
			public int getPreferredSqlTypeCodeForArray() {
				return ConfigurationHelper.getPreferredSqlTypeCodeForArray( configurationService, dialect );
			}

			@Override
			public boolean isPreferJavaTimeJdbcTypesEnabled() {
				return getBoolean( JAVA_TIME_USE_DIRECT_JDBC, configurationService.getSettings() );
			}

			@Override
			public boolean isPreferNativeEnumTypesEnabled() {
				//TODO: HHH-17905 proposes to switch this default to true
				return getBoolean( PREFER_NATIVE_ENUM_TYPES, configurationService.getSettings() );
			}

			@Override
			public boolean isPreferLocaleLanguageTagEnabled() {
				return getBoolean( PREFER_LOCALE_LANGUAGE_TAG, configurationService.getSettings() );
			}
		};
	}
}
