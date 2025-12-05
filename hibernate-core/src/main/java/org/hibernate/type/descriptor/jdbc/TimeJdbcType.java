/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalTime;
import java.util.Calendar;

import jakarta.persistence.TemporalType;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#TIME TIME} handling.
 *
 * @author Steve Ebersole
 */
public class TimeJdbcType implements JdbcType {
	public static final TimeJdbcType INSTANCE = new TimeJdbcType();

	public TimeJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.TIME;
	}

	@Override
	public String getFriendlyName() {
		return "TIME";
	}

	@Override
	public String toString() {
		return "TimeTypeDescriptor";
	}

	@Override
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		final var javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
		return typeConfiguration.getCurrentBaseSqlTypeIndicators().preferJdbcDatetimeTypes()
				? javaTypeRegistry.getDescriptor( Time.class )
				: javaTypeRegistry.getDescriptor( LocalTime.class );
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterTemporal<>( javaType, TemporalType.TIME );
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return Time.class;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final Time time = javaType.unwrap( value, Time.class, options );
				if ( value instanceof Calendar calendar ) {
					st.setTime( index, time, calendar );
				}
				else if ( options.getJdbcTimeZone() != null ) {
					st.setTime( index, time, Calendar.getInstance( options.getJdbcTimeZone() ) );
				}
				else {
					st.setTime( index, time );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Time time = javaType.unwrap( value, Time.class, options );
				if ( value instanceof Calendar calendar ) {
					st.setTime( name, time, calendar );
				}
				else if ( options.getJdbcTimeZone() != null ) {
					st.setTime( name, time, Calendar.getInstance( options.getJdbcTimeZone() ) );
				}
				else {
					st.setTime( name, time );
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return options.getJdbcTimeZone() != null ?
						javaType.wrap( rs.getTime( paramIndex, Calendar.getInstance( options.getJdbcTimeZone() ) ), options ) :
						javaType.wrap( rs.getTime( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return options.getJdbcTimeZone() != null ?
						javaType.wrap( statement.getTime( index, Calendar.getInstance( options.getJdbcTimeZone() ) ), options ) :
						javaType.wrap( statement.getTime( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return options.getJdbcTimeZone() != null ?
						javaType.wrap( statement.getTime( name, Calendar.getInstance( options.getJdbcTimeZone() ) ), options ) :
						javaType.wrap( statement.getTime( name ), options );
			}
		};
	}
}
