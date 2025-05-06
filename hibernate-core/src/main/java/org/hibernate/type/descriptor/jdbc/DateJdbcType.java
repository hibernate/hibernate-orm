/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Calendar;

import jakarta.persistence.TemporalType;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#DATE DATE} handling.
 *
 * @author Steve Ebersole
 */
public class DateJdbcType implements JdbcType {
	public static final DateJdbcType INSTANCE = new DateJdbcType();

	public DateJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.DATE;
	}

	@Override
	public String getFriendlyName() {
		return "DATE";
	}

	@Override
	public String toString() {
		return "DateTypeDescriptor";
	}

	@Override
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getCurrentBaseSqlTypeIndicators().preferJdbcDatetimeTypes()
				? typeConfiguration.getJavaTypeRegistry().getDescriptor( Date.class )
				: typeConfiguration.getJavaTypeRegistry().getDescriptor( LocalDate.class );
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterTemporal<>( javaType, TemporalType.DATE );
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return Date.class;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final Date date = javaType.unwrap( value, Date.class, options );
				if ( value instanceof Calendar calendar ) {
					st.setDate( index, date, calendar );
				}
				else {
					st.setDate( index, date );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Date date = javaType.unwrap( value, Date.class, options );
				if ( value instanceof Calendar calendar ) {
					st.setDate( name, date, calendar );
				}
				else {
					st.setDate( name, date );
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaType.wrap( rs.getDate( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaType.wrap( statement.getDate( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaType.wrap( statement.getDate( name ), options );
			}
		};
	}
}
