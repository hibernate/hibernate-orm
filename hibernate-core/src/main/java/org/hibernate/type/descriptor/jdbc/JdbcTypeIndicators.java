/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.jdbc;

import jakarta.persistence.EnumType;
import jakarta.persistence.TemporalType;

import org.hibernate.AssertionFailure;
import org.hibernate.Incubating;
import org.hibernate.TimeZoneStorageStrategy;
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
	 *
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
	 * Provides access to the {@link TypeConfiguration} for access to various type system related registries.
	 */
	TypeConfiguration getTypeConfiguration();

	private JdbcTypeIndicators getCurrentBaseSqlTypeIndicators() {
		return getTypeConfiguration().getCurrentBaseSqlTypeIndicators();
	}

	/**
	 * @return the SQL column type used for storing times under the
	 *         given {@linkplain  TimeZoneStorageStrategy storage strategy}
	 *
	 * @see SqlTypes#TIME_WITH_TIMEZONE
	 * @see SqlTypes#TIME
	 * @see SqlTypes#TIME_UTC
	 */
	static int getZonedTimeSqlType(TimeZoneStorageStrategy storageStrategy) {
		switch ( storageStrategy ) {
			case NATIVE:
				return SqlTypes.TIME_WITH_TIMEZONE;
			case COLUMN:
			case NORMALIZE:
				return SqlTypes.TIME;
			case NORMALIZE_UTC:
				return SqlTypes.TIME_UTC;
			default:
				throw new AssertionFailure( "unknown time zone storage strategy" );
		}
	}

	/**
	 * @return the SQL column type used for storing datetimes under the
	 *         given {@linkplain  TimeZoneStorageStrategy storage strategy}
	 *
	 * @see SqlTypes#TIME_WITH_TIMEZONE
	 * @see SqlTypes#TIMESTAMP
	 * @see SqlTypes#TIMESTAMP_UTC
	 */
	static int getZonedTimestampSqlType(TimeZoneStorageStrategy storageStrategy) {
		switch ( storageStrategy ) {
			case NATIVE:
				return SqlTypes.TIMESTAMP_WITH_TIMEZONE;
			case COLUMN:
			case NORMALIZE:
				return SqlTypes.TIMESTAMP;
			case NORMALIZE_UTC:
				// sensitive to hibernate.type.preferred_instant_jdbc_type
				return SqlTypes.TIMESTAMP_UTC;
			default:
				throw new AssertionFailure( "unknown time zone storage strategy" );
		}
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
		switch ( temporalPrecision == null ? TemporalType.TIMESTAMP : temporalPrecision ) {
			case TIME:
				return getZonedTimeSqlType( getDefaultTimeZoneStorageStrategy() );
			case DATE:
				return Types.DATE;
			case TIMESTAMP:
				// sensitive to hibernate.timezone.default_storage
				return getZonedTimestampSqlType( getDefaultTimeZoneStorageStrategy() );
			default:
				throw new IllegalArgumentException( "Unexpected jakarta.persistence.TemporalType : " + temporalPrecision);
		}
	}

	Dialect getDialect();
}
