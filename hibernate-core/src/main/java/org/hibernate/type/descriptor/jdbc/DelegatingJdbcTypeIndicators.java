/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.Incubating;
import org.hibernate.TimeZoneStorageStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.EnumType;
import jakarta.persistence.TemporalType;

public class DelegatingJdbcTypeIndicators implements JdbcTypeIndicators {

	private final JdbcTypeIndicators delegate;

	public DelegatingJdbcTypeIndicators(JdbcTypeIndicators delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean isNationalized() {
		return delegate.isNationalized();
	}

	@Override
	public boolean isLob() {
		return delegate.isLob();
	}

	@Override
	public EnumType getEnumeratedType() {
		return delegate.getEnumeratedType();
	}

	@Override
	public TemporalType getTemporalPrecision() {
		return delegate.getTemporalPrecision();
	}

	@Override
	public boolean isPreferJavaTimeJdbcTypesEnabled() {
		return delegate.isPreferJavaTimeJdbcTypesEnabled();
	}

	@Override
	public boolean isPreferNativeEnumTypesEnabled() {
		return delegate.isPreferNativeEnumTypesEnabled();
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return delegate.getPreferredSqlTypeCodeForBoolean();
	}

	@Override
	public int getPreferredSqlTypeCodeForDuration() {
		return delegate.getPreferredSqlTypeCodeForDuration();
	}

	@Override
	public int getPreferredSqlTypeCodeForUuid() {
		return delegate.getPreferredSqlTypeCodeForUuid();
	}

	@Override
	public int getPreferredSqlTypeCodeForInstant() {
		return delegate.getPreferredSqlTypeCodeForInstant();
	}

	@Override
	public int getPreferredSqlTypeCodeForArray() {
		return delegate.getPreferredSqlTypeCodeForArray();
	}

	@Override
	public long getColumnLength() {
		return delegate.getColumnLength();
	}

	@Override
	public int getColumnPrecision() {
		return delegate.getColumnPrecision();
	}

	@Override
	public int getColumnScale() {
		return delegate.getColumnScale();
	}

	@Override
	@Incubating
	public Integer getExplicitJdbcTypeCode() {
		return delegate.getExplicitJdbcTypeCode();
	}

	@Override
	public TimeZoneStorageStrategy getDefaultTimeZoneStorageStrategy() {
		return delegate.getDefaultTimeZoneStorageStrategy();
	}

	@Override
	public JdbcType getJdbcType(int jdbcTypeCode) {
		return delegate.getJdbcType( jdbcTypeCode );
	}

	@Override
	public int resolveJdbcTypeCode(int jdbcTypeCode) {
		return delegate.resolveJdbcTypeCode( jdbcTypeCode );
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return delegate.getTypeConfiguration();
	}

	public static int getZonedTimeSqlType(TimeZoneStorageStrategy storageStrategy) {
		return JdbcTypeIndicators.getZonedTimeSqlType( storageStrategy );
	}

	public static int getZonedTimestampSqlType(TimeZoneStorageStrategy storageStrategy) {
		return JdbcTypeIndicators.getZonedTimestampSqlType( storageStrategy );
	}

	@Override
	public int getDefaultZonedTimeSqlType() {
		return delegate.getDefaultZonedTimeSqlType();
	}

	@Override
	public int getDefaultZonedTimestampSqlType() {
		return delegate.getDefaultZonedTimestampSqlType();
	}

	@Override
	public Dialect getDialect() {
		return delegate.getDialect();
	}
}
