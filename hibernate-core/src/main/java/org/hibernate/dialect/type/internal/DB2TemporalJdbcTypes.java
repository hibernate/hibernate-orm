/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;

import org.hibernate.Internal;
import org.hibernate.dialect.DB2GetObjectExtractor;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.InstantJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.OffsetDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.OffsetTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.ZonedDateTimeJdbcType;

/// Internal stock implementations exposed only through `DB2JdbcTypes`.
///
/// @author Steve Ebersole
/// @since 8.0
@Internal
public final class DB2TemporalJdbcTypes {
	private static final JdbcType INSTANT = new InstantJdbcType() {
		@Override
		public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
			return new DB2GetObjectExtractor<>( javaType, this, Instant.class );
		}
	};
	private static final JdbcType LOCAL_DATE = new LocalDateJdbcType() {
		@Override
		public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
			return new DB2GetObjectExtractor<>( javaType, this, LocalDate.class );
		}
	};
	private static final JdbcType LOCAL_TIME = new LocalTimeJdbcType() {
		@Override
		public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
			return new DB2GetObjectExtractor<>( javaType, this, LocalTime.class );
		}
	};
	private static final JdbcType LOCAL_DATE_TIME = new LocalDateTimeJdbcType() {
		@Override
		public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
			return new DB2GetObjectExtractor<>( javaType, this, LocalDateTime.class );
		}
	};
	private static final JdbcType OFFSET_TIME = new OffsetTimeJdbcType() {
		@Override
		public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
			return new DB2GetObjectExtractor<>( javaType, this, OffsetTime.class );
		}
	};
	private static final JdbcType OFFSET_DATE_TIME = new OffsetDateTimeJdbcType() {
		@Override
		public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
			return new DB2GetObjectExtractor<>( javaType, this, OffsetDateTime.class );
		}
	};
	private static final JdbcType ZONED_DATE_TIME = new ZonedDateTimeJdbcType() {
		@Override
		public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
			return new DB2GetObjectExtractor<>( javaType, this, ZonedDateTime.class );
		}
	};

	private DB2TemporalJdbcTypes() {
	}

	public static JdbcType instant() {
		return INSTANT;
	}

	public static JdbcType localDate() {
		return LOCAL_DATE;
	}

	public static JdbcType localTime() {
		return LOCAL_TIME;
	}

	public static JdbcType localDateTime() {
		return LOCAL_DATE_TIME;
	}

	public static JdbcType offsetTime() {
		return OFFSET_TIME;
	}

	public static JdbcType offsetDateTime() {
		return OFFSET_DATE_TIME;
	}

	public static JdbcType zonedDateTime() {
		return ZONED_DATE_TIME;
	}
}
