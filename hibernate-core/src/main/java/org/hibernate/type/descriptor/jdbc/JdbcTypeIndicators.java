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
import org.hibernate.TimeZoneStorageStrategy;
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
	 * When mapping a boolean type to the database what is the preferred SQL type code to use?
	 * <p>
	 * Returns a key into the {@link JdbcTypeRegistry}.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PREFERRED_BOOLEAN_JDBC_TYPE
	 * @see org.hibernate.dialect.Dialect#getPreferredSqlTypeCodeForBoolean()
	 */
	default int getPreferredSqlTypeCodeForBoolean() {
		return getCurrentBaseSqlTypeIndicators().getPreferredSqlTypeCodeForBoolean();
	}

	/**
	 * When mapping a duration type to the database what is the preferred SQL type code to use?
	 * <p>
	 * Returns a key into the {@link JdbcTypeRegistry}.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PREFERRED_DURATION_JDBC_TYPE
	 */
	default int getPreferredSqlTypeCodeForDuration() {
		return getCurrentBaseSqlTypeIndicators().getPreferredSqlTypeCodeForDuration();
	}

	/**
	 * When mapping an uuid type to the database what is the preferred SQL type code to use?
	 * <p>
	 * Returns a key into the {@link JdbcTypeRegistry}.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PREFERRED_UUID_JDBC_TYPE
	 */
	default int getPreferredSqlTypeCodeForUuid() {
		return getCurrentBaseSqlTypeIndicators().getPreferredSqlTypeCodeForUuid();
	}

	/**
	 * When mapping an instant type to the database what is the preferred SQL type code to use?
	 * <p>
	 * Returns a key into the {@link JdbcTypeRegistry}.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PREFERRED_INSTANT_JDBC_TYPE
	 */
	default int getPreferredSqlTypeCodeForInstant() {
		return getCurrentBaseSqlTypeIndicators().getPreferredSqlTypeCodeForInstant();
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
		return getCurrentBaseSqlTypeIndicators().getPreferredSqlTypeCodeForArray();
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
	 * Provides access to the {@link TypeConfiguration} for access to various type system related registries.
	 */
	TypeConfiguration getTypeConfiguration();

	private JdbcTypeIndicators getCurrentBaseSqlTypeIndicators() {
		return getTypeConfiguration().getCurrentBaseSqlTypeIndicators();
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
	 * @see SqlTypes#TIMESTAMP
	 * @see SqlTypes#TIMESTAMP_UTC
	 */
	default int getDefaultZonedTimestampSqlType() {
		final TemporalType temporalPrecision = getTemporalPrecision();
		switch ( temporalPrecision == null ? TemporalType.TIMESTAMP : temporalPrecision ) {
			case TIME:
				return Types.TIME;
			case DATE:
				return Types.DATE;
			case TIMESTAMP:
				// sensitive to hibernate.timezone.default_storage
				return getZonedTimestampSqlType( getDefaultTimeZoneStorageStrategy() );
			default:
				throw new IllegalArgumentException( "Unexpected jakarta.persistence.TemporalType : " + temporalPrecision);
		}
	}
}
