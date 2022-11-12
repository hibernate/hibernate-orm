/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.jdbc;

import jakarta.persistence.EnumType;
import jakarta.persistence.TemporalType;

import org.hibernate.TimeZoneStorageStrategy;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A parameter object that helps in determine the {@link java.sql.Types SQL/JDBC type}
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
	 * <p/>
	 * Returns a key into the {@link JdbcTypeRegistry}.
	 */
	default int getPreferredSqlTypeCodeForBoolean() {
		return getCurrentBaseSqlTypeIndicators().getPreferredSqlTypeCodeForBoolean();
	}

	/**
	 * When mapping a duration type to the database what is the preferred SQL type code to use?
	 * <p/>
	 * Returns a key into the {@link JdbcTypeRegistry}.
	 */
	default int getPreferredSqlTypeCodeForDuration() {
		return getCurrentBaseSqlTypeIndicators().getPreferredSqlTypeCodeForDuration();
	}

	/**
	 * When mapping an uuid type to the database what is the preferred SQL type code to use?
	 * <p/>
	 * Returns a key into the {@link JdbcTypeRegistry}.
	 */
	default int getPreferredSqlTypeCodeForUuid() {
		return getCurrentBaseSqlTypeIndicators().getPreferredSqlTypeCodeForUuid();
	}

	/**
	 * When mapping an instant type to the database what is the preferred SQL type code to use?
	 * <p/>
	 * Returns a key into the {@link JdbcTypeRegistry}.
	 */
	default int getPreferredSqlTypeCodeForInstant() {
		return getCurrentBaseSqlTypeIndicators().getPreferredSqlTypeCodeForInstant();
	}

	/**
	 * When mapping a basic array or collection type to the database what is the preferred SQL type code to use?
	 * <p/>
	 * Returns a key into the {@link JdbcTypeRegistry}.
	 *
	 * @since 6.1
	 */
	default int getPreferredSqlTypeCodeForArray() {
		return getCurrentBaseSqlTypeIndicators().getPreferredSqlTypeCodeForArray();
	}

	/**
	 * Useful for resolutions based on column length.
	 *
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
	 *
	 * E.g. for choosing between a {@code NUMERIC} and {@code INTERVAL SECOND}.
	 */
	default int getColumnScale() {
		return NO_COLUMN_SCALE;
	}

	/**
	 * The default {@link TimeZoneStorageStrategy}.
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
}
