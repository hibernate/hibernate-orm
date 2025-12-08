/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

/**
 * Descriptor for {@link SqlTypes#TIMESTAMP_UTC TIMESTAMP_UTC} handling.
 *
 * @author Christian Beikov
 */
public class TimestampUtcAsOffsetDateTimeJdbcType implements JdbcType {

	public static final TimestampUtcAsOffsetDateTimeJdbcType INSTANCE = new TimestampUtcAsOffsetDateTimeJdbcType();

	public TimestampUtcAsOffsetDateTimeJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.TIMESTAMP_WITH_TIMEZONE;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.TIMESTAMP_UTC;
	}

	@Override
	public String getFriendlyName() {
		return "TIMESTAMP_UTC";
	}

	@Override
	public String toString() {
		return "TimestampUtcDescriptor";
	}

	@Override
	public JavaType<?> getRecommendedJavaType(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().resolveDescriptor( Instant.class );
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return OffsetDateTime.class;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterTemporal<>( javaType, TemporalType.TIMESTAMP );
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(
					PreparedStatement st,
					X value,
					int index,
					WrapperOptions wrapperOptions) throws SQLException {
				final OffsetDateTime dateTime = javaType.unwrap( value, OffsetDateTime.class, wrapperOptions );
				// supposed to be supported in JDBC 4.2
				st.setObject( index, dateTime.withOffsetSameInstant( ZoneOffset.UTC ), Types.TIMESTAMP_WITH_TIMEZONE );
			}

			@Override
			protected void doBind(
					CallableStatement st,
					X value,
					String name,
					WrapperOptions wrapperOptions)
					throws SQLException {
				final OffsetDateTime dateTime = javaType.unwrap( value, OffsetDateTime.class, wrapperOptions );
				// supposed to be supported in JDBC 4.2
				st.setObject( name, dateTime.withOffsetSameInstant( ZoneOffset.UTC ), Types.TIMESTAMP_WITH_TIMEZONE );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int position, WrapperOptions wrapperOptions) throws SQLException {
				return javaType.wrap( rs.getObject( position, OffsetDateTime.class ), wrapperOptions );
			}

			@Override
			protected X doExtract(CallableStatement statement, int position, WrapperOptions wrapperOptions) throws SQLException {
				return javaType.wrap( statement.getObject( position, OffsetDateTime.class ), wrapperOptions );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions wrapperOptions) throws SQLException {
				return javaType.wrap( statement.getObject( name, OffsetDateTime.class ), wrapperOptions );
			}
		};
	}
}
