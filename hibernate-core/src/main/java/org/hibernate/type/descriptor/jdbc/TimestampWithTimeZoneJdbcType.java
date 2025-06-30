/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.Calendar;

/**
 * Descriptor for {@link Types#TIMESTAMP_WITH_TIMEZONE TIMESTAMP_WITH_TIMEZONE} handling.
 *
 * @author Gavin King
 */
public class TimestampWithTimeZoneJdbcType implements JdbcType {
	public static final TimestampWithTimeZoneJdbcType INSTANCE = new TimestampWithTimeZoneJdbcType();

	public TimestampWithTimeZoneJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.TIMESTAMP_WITH_TIMEZONE;
	}

	@Override
	public String getFriendlyName() {
		return "TIMESTAMP_WITH_TIMEZONE";
	}

	@Override
	public String toString() {
		return "TimestampWithTimeZoneDescriptor";
	}

	@Override
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().getDescriptor( OffsetDateTime.class );
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterTemporal<>( javaType, TemporalType.TIMESTAMP );
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return OffsetDateTime.class;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(
					PreparedStatement st,
					X value,
					int index,
					WrapperOptions options) throws SQLException {
				try {
					final OffsetDateTime dateTime = javaType.unwrap( value, OffsetDateTime.class, options );
					// supposed to be supported in JDBC 4.2
					st.setObject( index, dateTime, Types.TIMESTAMP_WITH_TIMEZONE );
				}
				catch (SQLException|AbstractMethodError e) {
					// fall back to treating it as a JDBC Timestamp
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
			}

			@Override
			protected void doBind(
					CallableStatement st,
					X value,
					String name,
					WrapperOptions options)
					throws SQLException {
				try {
					final OffsetDateTime dateTime = javaType.unwrap( value, OffsetDateTime.class, options );
					// supposed to be supported in JDBC 4.2
					st.setObject( name, dateTime, Types.TIMESTAMP_WITH_TIMEZONE );
				}
				catch (SQLException|AbstractMethodError e) {
					// fall back to treating it as a JDBC Timestamp
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
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int position, WrapperOptions options) throws SQLException {
				try {
					// supposed to be supported in JDBC 4.2
					return javaType.wrap( rs.getObject( position, OffsetDateTime.class ), options );
				}
				catch (SQLException|AbstractMethodError|ClassCastException e) {
					// fall back to treating it as a JDBC Timestamp
					return options.getJdbcTimeZone() != null ?
							javaType.wrap( rs.getTimestamp( position, Calendar.getInstance( options.getJdbcTimeZone() ) ), options ) :
							javaType.wrap( rs.getTimestamp( position ), options );
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, int position, WrapperOptions options) throws SQLException {
				try {
					// supposed to be supported in JDBC 4.2
					return javaType.wrap( statement.getObject( position, OffsetDateTime.class ), options );
				}
				catch (SQLException|AbstractMethodError|ClassCastException e) {
					// fall back to treating it as a JDBC Timestamp
					return options.getJdbcTimeZone() != null ?
							javaType.wrap( statement.getTimestamp( position, Calendar.getInstance( options.getJdbcTimeZone() ) ), options ) :
							javaType.wrap( statement.getTimestamp( position ), options );
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				try {
					// supposed to be supported in JDBC 4.2
					return javaType.wrap( statement.getObject( name, OffsetDateTime.class ), options );
				}
				catch (SQLException|AbstractMethodError|ClassCastException e) {
					// fall back to treating it as a JDBC Timestamp
					return options.getJdbcTimeZone() != null ?
							javaType.wrap( statement.getTimestamp( name, Calendar.getInstance( options.getJdbcTimeZone() ) ), options ) :
							javaType.wrap( statement.getTimestamp( name ), options );
				}
			}
		};
	}
}
