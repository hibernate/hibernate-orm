/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import jakarta.persistence.EnumType;
import jakarta.persistence.TemporalType;

import org.hibernate.Incubating;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.Types;

/**
 * A parameter object that helps determine the {@link java.sql.Types SQL/JDBC type}
 * recommended by the JDBC spec (explicitly or implicitly) for a given Java type.
 *
 * @see BasicJavaType#getRecommendedJdbcType
 *
 * @author Steve Ebersole
 */
public interface JdbcTypeIndicators {
	int NO_COLUMN_LENGTH = -1;
	int NO_COLUMN_PRECISION = -1;
	int NO_COLUMN_SCALE = -1;

	/**
	 * Was nationalized character datatype requested for the given Java type?
	 *
	 * @return {@code true} if nationalized character datatype should be used;
	 *         {@code false} otherwise.
	 */
	default boolean isNationalized() {
		return false;
	}

	/**
	 * Was LOB datatype requested for the given Java type?
	 *
	 * @return {@code true} if LOB datatype should be used;
	 *         {@code false} otherwise.
	 */
	default boolean isLob() {
		return false;
	}

	/**
	 * For enum mappings, what style of storage was requested (name vs. ordinal)?
	 *
	 * @return The enum type.
	 */
	default EnumType getEnumeratedType() {
		return EnumType.ORDINAL;
	}

	/**
	 * For temporal type mappings, what precision was requested?
	 */
	default TemporalType getTemporalPrecision() {
		return null;
	}

	/**
	 * @see org.hibernate.cfg.MappingSettings#JAVA_TIME_USE_DIRECT_JDBC
	 */
	default boolean isPreferJavaTimeJdbcTypesEnabled() {
		return getCurrentBaseSqlTypeIndicators().isPreferJavaTimeJdbcTypesEnabled();
	}

	/**
	 * @see org.hibernate.cfg.MappingSettings#PREFER_NATIVE_ENUM_TYPES
	 */
	default boolean isPreferNativeEnumTypesEnabled() {
		return getCurrentBaseSqlTypeIndicators().isPreferNativeEnumTypesEnabled();
	}

	/**
	 * When mapping a boolean type to the database what is the preferred SQL type code to use?
	 * <p>
	 * Returns a key into the {@link JdbcTypeRegistry}.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PREFERRED_BOOLEAN_JDBC_TYPE
	 * @see org.hibernate.dialect.Dialect#getPreferredSqlTypeCodeForBoolean()
	 */
	default int getPreferredSqlTypeCodeForBoolean() {
		return resolveJdbcTypeCode( getCurrentBaseSqlTypeIndicators().getPreferredSqlTypeCodeForBoolean() );
	}

	/**
	 * When mapping a duration type to the database what is the preferred SQL type code to use?
	 * <p>
	 * Returns a key into the {@link JdbcTypeRegistry}.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PREFERRED_DURATION_JDBC_TYPE
	 */
	default int getPreferredSqlTypeCodeForDuration() {
		return resolveJdbcTypeCode( getCurrentBaseSqlTypeIndicators().getPreferredSqlTypeCodeForDuration() );
	}

	/**
	 * When mapping an uuid type to the database what is the preferred SQL type code to use?
	 * <p>
	 * Returns a key into the {@link JdbcTypeRegistry}.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PREFERRED_UUID_JDBC_TYPE
	 */
	default int getPreferredSqlTypeCodeForUuid() {
		return resolveJdbcTypeCode( getCurrentBaseSqlTypeIndicators().getPreferredSqlTypeCodeForUuid() );
	}

	/**
	 * When mapping an instant type to the database what is the preferred SQL type code to use?
	 * <p>
	 * Returns a key into the {@link JdbcTypeRegistry}.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PREFERRED_INSTANT_JDBC_TYPE
	 */
	default int getPreferredSqlTypeCodeForInstant() {
		return resolveJdbcTypeCode( getCurrentBaseSqlTypeIndicators().getPreferredSqlTypeCodeForInstant() );
	}

	/**
	 * When mapping a basic array or collection type to the database what is the preferred SQL type code to use?
	 * <p>
	 * Returns a key into the {@link JdbcTypeRegistry}.
	 *
	 * @see org.hibernate.dialect.Dialect#getPreferredSqlTypeCodeForArray()
	 *
	 * @since 6.1
	 */
	default int getPreferredSqlTypeCodeForArray() {
		return resolveJdbcTypeCode( getCurrentBaseSqlTypeIndicators().getPreferredSqlTypeCodeForArray() );
	}

	/**
	 * When mapping a basic array or collection type to the database what is the preferred SQL type code to use,
	 * given the element SQL type code?
	 * <p>
	 * Returns a key into the {@link JdbcTypeRegistry}.
	 *
	 * @see org.hibernate.dialect.Dialect#getPreferredSqlTypeCodeForArray()
	 *
	 * @since 7.0
	 */
	default int getPreferredSqlTypeCodeForArray(int elementSqlTypeCode) {
		return resolveJdbcTypeCode(
				switch ( elementSqlTypeCode ) {
					case SqlTypes.JSON -> SqlTypes.JSON_ARRAY;
					case SqlTypes.SQLXML -> SqlTypes.XML_ARRAY;
					default -> getExplicitJdbcTypeCode();
				}
		);
	}

	/**
	 * Useful for resolutions based on column length.
	 * <p>
	 * E.g. for choosing between a {@code VARCHAR} ({@code String}) and {@code CHAR(1)} ({@code Character}/{@code char}).
	 */
	default long getColumnLength() {
		return NO_COLUMN_LENGTH;
	}

	/**
	 * Useful for resolutions based on column precision.
	 */
	default int getColumnPrecision() {
		return NO_COLUMN_PRECISION;
	}

	/**
	 * Useful for resolutions based on column scale.
	 * <p>
	 * E.g. for choosing between a {@code NUMERIC} and {@code INTERVAL SECOND}.
	 */
	default int getColumnScale() {
		return NO_COLUMN_SCALE;
	}

	/**
	 * Used (for now) only to choose a container {@link JdbcType} for
	 * SQL arrays.
	 *
	 * @since 6.3
	 */
	@Incubating
	default Integer getExplicitJdbcTypeCode() {
		return getPreferredSqlTypeCodeForArray();
	}

	/**
	 * The default {@link TimeZoneStorageStrategy}.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#TIMEZONE_DEFAULT_STORAGE
	 * @see org.hibernate.dialect.Dialect#getTimeZoneSupport()
	 */
	default TimeZoneStorageStrategy getDefaultTimeZoneStorageStrategy() {
		return getCurrentBaseSqlTypeIndicators().getDefaultTimeZoneStorageStrategy();
	}

	/**
	 * The {@link JdbcType} registered under the given type code with the associated {@link JdbcTypeRegistry}.
	 *
	 * @param jdbcTypeCode a type code from {@link org.hibernate.type.SqlTypes}
	 * @return the {@link JdbcType} registered under that type code
	 */
	default JdbcType getJdbcType(int jdbcTypeCode) {
		return getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( jdbcTypeCode );
	}

	/**
	 * Resolves the given type code to a possibly different type code, based on context.
	 * <p>
	 * A database might not support a certain type code in certain scenarios like within a UDT
	 * and has to resolve to a different type code in such a scenario.
	 *
	 * @param jdbcTypeCode a type code from {@link org.hibernate.type.SqlTypes}
	 * @return The jdbc type code to use
	 */
	default int resolveJdbcTypeCode(int jdbcTypeCode) {
		return jdbcTypeCode;
	}

	/**
	 * Should native queries return JDBC datetime types
	 * instead of using {@code java.time} types.
	 *
	 * @since 7.0
	 *
	 * @see org.hibernate.cfg.QuerySettings#NATIVE_PREFER_JDBC_DATETIME_TYPES
	 */
	default boolean preferJdbcDatetimeTypes() {
		return false;
	}

	/**
	 * Whether to use the legacy format for serializing/deserializing XML data.
	 *
	 * @since 7.0
	 * @see org.hibernate.cfg.MappingSettings#XML_FORMAT_MAPPER_LEGACY_FORMAT
	 */
	@Incubating
	default boolean isXmlFormatMapperLegacyFormatEnabled() {
		return getCurrentBaseSqlTypeIndicators().isXmlFormatMapperLegacyFormatEnabled();
	}

	/**
	 * Provides access to the {@link TypeConfiguration} for access to various type system related registries.
	 */
	TypeConfiguration getTypeConfiguration();

	private JdbcTypeIndicators getCurrentBaseSqlTypeIndicators() {
		return getTypeConfiguration().getCurrentBaseSqlTypeIndicators();
	}

	/**
	 * @return the SQL column type used for storing times under the
	 *         given {@linkplain TimeZoneStorageStrategy storage strategy}
	 *
	 * @see SqlTypes#TIME_WITH_TIMEZONE
	 * @see SqlTypes#TIME
	 * @see SqlTypes#TIME_UTC
	 */
	static int getZonedTimeSqlType(TimeZoneStorageStrategy storageStrategy) {
		return switch (storageStrategy) {
			case NATIVE -> SqlTypes.TIME_WITH_TIMEZONE;
			case COLUMN, NORMALIZE -> SqlTypes.TIME;
			case NORMALIZE_UTC -> SqlTypes.TIME_UTC;
		};
	}

	/**
	 * @return the SQL column type used for storing datetimes under the
	 *         given {@linkplain TimeZoneStorageStrategy storage strategy}
	 *
	 * @see SqlTypes#TIME_WITH_TIMEZONE
	 * @see SqlTypes#TIMESTAMP
	 * @see SqlTypes#TIMESTAMP_UTC
	 */
	static int getZonedTimestampSqlType(TimeZoneStorageStrategy storageStrategy) {
		return switch (storageStrategy) {
			case NATIVE -> SqlTypes.TIMESTAMP_WITH_TIMEZONE;
			case COLUMN, NORMALIZE -> SqlTypes.TIMESTAMP;
			case NORMALIZE_UTC ->
				// sensitive to hibernate.type.preferred_instant_jdbc_type
					SqlTypes.TIMESTAMP_UTC;
		};
	}

	/**
	 * @return the SQL column type used for storing datetimes under the
	 *         default {@linkplain  TimeZoneStorageStrategy storage strategy}
	 *
	 * @see SqlTypes#TIME_WITH_TIMEZONE
	 * @see SqlTypes#TIME
	 * @see SqlTypes#TIME_UTC
	 */
	default int getDefaultZonedTimeSqlType() {
		return getZonedTimeSqlType( getDefaultTimeZoneStorageStrategy() );
	}

	/**
	 * @return the SQL column type used for storing datetimes under the
	 *         default {@linkplain  TimeZoneStorageStrategy storage strategy}
	 *
	 * @see SqlTypes#TIME_WITH_TIMEZONE
	 * @see SqlTypes#TIMESTAMP
	 * @see SqlTypes#TIMESTAMP_UTC
	 */
	default int getDefaultZonedTimestampSqlType() {
		final TemporalType temporalPrecision = getTemporalPrecision();
		return switch (temporalPrecision == null ? TemporalType.TIMESTAMP : temporalPrecision) {
			case TIME -> getZonedTimeSqlType( getDefaultTimeZoneStorageStrategy() );
			case DATE -> Types.DATE;
			case TIMESTAMP ->
				// sensitive to hibernate.timezone.default_storage
					getZonedTimestampSqlType( getDefaultTimeZoneStorageStrategy() );
		};
	}

	Dialect getDialect();
}
