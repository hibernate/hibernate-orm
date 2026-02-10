/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Calendar;

import jakarta.persistence.TemporalType;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#TIMESTAMP TIMESTAMP} handling.
 *
 * @author Steve Ebersole
 */
public class TimestampJdbcType implements JdbcType {
	public static final TimestampJdbcType INSTANCE = new TimestampJdbcType();

	public TimestampJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.TIMESTAMP;
	}

	@Override
	public String getFriendlyName() {
		return "TIMESTAMP";
	}

	@Override
	public String toString() {
		return "TimestampTypeDescriptor";
	}

	@Override
	public JavaType<?> getRecommendedJavaType(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		final var javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
		return typeConfiguration.getCurrentBaseSqlTypeIndicators().preferJdbcDatetimeTypes()
				? javaTypeRegistry.resolveDescriptor( Timestamp.class )
				: javaTypeRegistry.resolveDescriptor( LocalDateTime.class );
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterTemporal<>( javaType, TemporalType.TIMESTAMP );
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return Timestamp.class;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final Timestamp timestamp = javaType.unwrap( value, Timestamp.class, options );
				if ( value instanceof Calendar calendar ) {
					st.setTimestamp( index, timestamp, calendar );
				}
				else if ( options.getJdbcTimeZone() != null ) {
					st.setTimestamp( index, timestamp, Calendar.getInstance( options.getJdbcTimeZone() ) );
				}
				else {
					st.setTimestamp( index, timestamp );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Timestamp timestamp = javaType.unwrap( value, Timestamp.class, options );
				if ( value instanceof Calendar calendar ) {
					st.setTimestamp( name, timestamp, calendar );
				}
				else if ( options.getJdbcTimeZone() != null ) {
					st.setTimestamp( name, timestamp, Calendar.getInstance( options.getJdbcTimeZone() ) );
				}
				else {
					st.setTimestamp( name, timestamp );
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
					javaType.wrap( rs.getTimestamp( paramIndex, Calendar.getInstance( options.getJdbcTimeZone() ) ), options ) :
					javaType.wrap( rs.getTimestamp( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return options.getJdbcTimeZone() != null ?
						javaType.wrap( statement.getTimestamp( index, Calendar.getInstance( options.getJdbcTimeZone() ) ), options ) :
						javaType.wrap( statement.getTimestamp( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return options.getJdbcTimeZone() != null ?
						javaType.wrap( statement.getTimestamp( name, Calendar.getInstance( options.getJdbcTimeZone() ) ), options ) :
						javaType.wrap( statement.getTimestamp( name ), options );
			}
		};
	}
}
