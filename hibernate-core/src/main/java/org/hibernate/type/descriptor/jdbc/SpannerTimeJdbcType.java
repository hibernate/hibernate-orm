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
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

public class SpannerTimeJdbcType extends TimeJdbcType {

	public static final TimeJdbcType INSTANCE = new SpannerTimeJdbcType();

	private static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone( ZoneOffset.UTC );

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return Timestamp.class;
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>(javaType, this) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final Timestamp timestamp;
				if (value instanceof Time time) {
					timestamp = new Timestamp( time.getTime() );
				}
				else {
					timestamp = javaType.unwrap( value, Timestamp.class, options );
				}
				st.setTimestamp( index, timestamp, Calendar.getInstance( DEFAULT_TIME_ZONE ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Timestamp timestamp;
				if (value instanceof Time time) {
					timestamp = new Timestamp( time.getTime() );
				}
				else {
					timestamp = javaType.unwrap( value, Timestamp.class, options );
				}
				st.setTimestamp( name, timestamp, Calendar.getInstance( DEFAULT_TIME_ZONE ) );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>(javaType, this) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaType.wrap( rs.getTimestamp( paramIndex, Calendar.getInstance( DEFAULT_TIME_ZONE ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaType.wrap( statement.getTimestamp( index, Calendar.getInstance( DEFAULT_TIME_ZONE ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaType.wrap( statement.getTimestamp( name, Calendar.getInstance( DEFAULT_TIME_ZONE ) ), options );
			}
		};
	}
}
