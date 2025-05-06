/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal;

import org.hibernate.internal.util.StringHelper;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Currency;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * @author Steve Ebersole
 */
public enum SimpleTypeInterpretation {
	BOOLEAN( Boolean.class ),
	BYTE( Byte.class ),
	SHORT( Short.class ),
	INTEGER( Integer.class ),
	LONG( Long.class ),
	DOUBLE( Double.class ),
	FLOAT( Float.class ),
	BIG_INTEGER( BigInteger.class ),
	BIG_DECIMAL( BigDecimal.class ),
	CHARACTER( Character.class ),
	STRING( String.class ),
	INSTANT( Instant.class ),
	DURATION( Duration.class ),
	YEAR( Year.class ),
	LOCAL_DATE_TIME( LocalDateTime.class ),
	LOCAL_DATE( LocalDate.class ),
	LOCAL_TIME( LocalTime.class ),
	OFFSET_DATE_TIME( OffsetDateTime.class ),
	OFFSET_TIME( OffsetTime.class ),
	ZONED_DATE_TIME( ZonedDateTime.class ),
	ZONE_ID( ZoneId.class ),
	ZONE_OFFSET( ZoneOffset.class ),
	UUID( UUID .class ),
	URL( java.net.URL.class ),
	INET_ADDRESS( InetAddress.class ),
	CURRENCY( Currency.class ),
	LOCALE( Locale.class ),
	CLASS( Class.class ),
	BLOB( Blob.class ),
	CLOB( Clob.class ),
	NCLOB( NClob.class ),
	JDBC_TIMESTAMP( Timestamp.class ),
	JDBC_DATE( Date.class ),
	JDBC_TIME( Time.class ),
	CALENDAR( Calendar.class ),
	TIME_ZONE( TimeZone.class ),
	SERIALIZABLE( Serializable.class ),

	PRIMITIVE_BOOLEAN( boolean.class, BOOLEAN ),
	PRIMITIVE_BYTE( byte.class, BYTE ),
	PRIMITIVE_SHORT( short.class, SHORT ),
	PRIMITIVE_INTEGER( int.class, INTEGER ),
	PRIMITIVE_LONG( long.class, LONG ),
	PRIMITIVE_DOUBLE( double.class, DOUBLE ),
	PRIMITIVE_FLOAT( float.class, FLOAT ),
	PRIMITIVE_CHARACTER( char.class, CHARACTER ),
	;


	private final Class<?> javaType;
	private final SimpleTypeInterpretation objectForm;

	SimpleTypeInterpretation(Class<?> javaType) {
		this.javaType = javaType;
		this.objectForm = this;
	}

	SimpleTypeInterpretation(Class<?> javaType, SimpleTypeInterpretation objectForm) {
		this.javaType = javaType;
		this.objectForm = objectForm;
	}

	public Class<?> getJavaType() {
		return javaType;
	}

	public SimpleTypeInterpretation getObjectForm() {
		return objectForm;
	}

	public static SimpleTypeInterpretation interpret(String name) {
		assert StringHelper.isNotEmpty( name );

		if ( boolean.class.getName().equals( name ) ) {
			return PRIMITIVE_BOOLEAN;
		}

		if ( byte.class.getName().equals( name ) ) {
			return PRIMITIVE_BYTE;
		}

		if ( short.class.getName().equals( name ) ) {
			return PRIMITIVE_SHORT;
		}

		if ( int.class.getName().equals( name ) ) {
			return PRIMITIVE_INTEGER;
		}

		if ( long.class.getName().equals( name ) ) {
			return PRIMITIVE_LONG;
		}

		if ( double.class.getName().equals( name ) ) {
			return PRIMITIVE_DOUBLE;
		}

		if ( float.class.getName().equals( name ) ) {
			return PRIMITIVE_FLOAT;
		}

		if ( char.class.getName().equals( name ) ) {
			return PRIMITIVE_CHARACTER;
		}

		if ( Boolean.class.getName().equals( name )
				|| Boolean.class.getSimpleName().equals( name ) ) {
			return BOOLEAN;
		}

		if ( Byte.class.getName().equals( name )
				|| Byte.class.getSimpleName().equals( name ) ) {
			return BYTE;
		}

		if ( Short.class.getName().equals( name )
				|| Short.class.getSimpleName().equals( name ) ) {
			return SHORT;
		}

		if ( Integer.class.getName().equals( name )
				|| Integer.class.getSimpleName().equals( name ) ) {
			return INTEGER;
		}

		if ( Long.class.getName().equals( name )
				|| Long.class.getSimpleName().equals( name ) ) {
			return LONG;
		}

		if ( Double.class.getName().equals( name )
				|| Double.class.getSimpleName().equals( name ) ) {
			return DOUBLE;
		}

		if ( Float.class.getName().equals( name )
				|| Float.class.getSimpleName().equals( name ) ) {
			return FLOAT;
		}

		if ( BigInteger.class.getName().equals( name )
				|| BigInteger.class.getSimpleName().equals( name ) ) {
			return BIG_INTEGER;
		}

		if ( BigDecimal.class.getName().equals( name )
				|| BigDecimal.class.getSimpleName().equals( name ) ) {
			return BIG_DECIMAL;
		}

		if ( String.class.getName().equals( name )
				|| String.class.getSimpleName().equals( name ) ) {
			return STRING;
		}

		if ( Character.class.getName().equals( name )
				|| Character.class.getSimpleName().equals( name ) ) {
			return CHARACTER;
		}

		if ( UUID.class.getName().equals( name )
				|| UUID.class.getSimpleName().equals( name ) ) {
			return UUID;
		}

		if ( URL.class.getName().equals( name )
				|| URL.class.getSimpleName().equals( name ) ) {
			return URL;
		}

		if ( InetAddress.class.getName().equals( name )
				|| InetAddress.class.getSimpleName().equals( name ) ) {
			return INET_ADDRESS;
		}

		if ( Blob.class.getName().equals( name )
				|| Blob.class.getSimpleName().equals( name ) ) {
			return BLOB;
		}

		if ( Clob.class.getName().equals( name )
				|| Clob.class.getSimpleName().equals( name ) ) {
			return CLOB;
		}

		if ( NClob.class.getName().equals( name )
				|| NClob.class.getSimpleName().equals( name ) ) {
			return NCLOB;
		}

		if ( Instant.class.getName().equals( name )
				|| Instant.class.getSimpleName().equals( name ) ) {
			return INSTANT;
		}

		if ( LocalDate.class.getName().equals( name )
				|| LocalDate.class.getSimpleName().equals( name ) ) {
			return LOCAL_DATE;
		}

		if ( LocalTime.class.getName().equals( name )
				|| LocalTime.class.getSimpleName().equals( name ) ) {
			return LOCAL_TIME;
		}

		if ( LocalDateTime.class.getName().equals( name )
				|| LocalDateTime.class.getSimpleName().equals( name ) ) {
			return LOCAL_DATE_TIME;
		}

		if ( ZonedDateTime.class.getName().equals( name )
				|| ZonedDateTime.class.getSimpleName().equals( name ) ) {
			return ZONED_DATE_TIME;
		}

		if ( OffsetTime.class.getName().equals( name )
				|| OffsetTime.class.getSimpleName().equals( name ) ) {
			return OFFSET_TIME;
		}

		if ( OffsetDateTime.class.getName().equals( name )
				|| OffsetDateTime.class.getSimpleName().equals( name ) ) {
			return OFFSET_DATE_TIME;
		}

		if ( ZoneId.class.getName().equals( name )
				|| ZoneId.class.getSimpleName().equals( name ) ) {
			return ZONE_ID;
		}

		if ( ZoneOffset.class.getName().equals( name )
				|| ZoneOffset.class.getSimpleName().equals( name ) ) {
			return ZONE_OFFSET;
		}

		if ( Duration.class.getName().equals( name )
			|| Duration.class.getSimpleName().equals( name ) ) {
			return DURATION;
		}

		if ( Year.class.getName().equals( name )
			|| Year.class.getSimpleName().equals( name ) ) {
			return YEAR;
		}

		if ( Timestamp.class.getName().equals( name )
				|| Timestamp.class.getSimpleName().equals( name ) ) {
			return JDBC_TIMESTAMP;
		}

		if ( Date.class.getName().equals( name )
				|| Date.class.getSimpleName().equals( name ) ) {
			return JDBC_DATE;
		}

		if ( Time.class.getName().equals( name )
				|| Time.class.getSimpleName().equals( name ) ) {
			return JDBC_TIME;
		}

		if ( Calendar.class.getName().equals( name )
			|| Calendar.class.getSimpleName().equals( name )
			|| GregorianCalendar.class.getName().equals( name )
			|| GregorianCalendar.class.getSimpleName().equals( name ) ) {
			return CALENDAR;
		}

		if ( TimeZone.class.getName().equals( name )
				|| TimeZone.class.getSimpleName().equals( name ) ) {
			return TIME_ZONE;
		}

		if ( Currency.class.getName().equals( name )
			|| Currency.class.getSimpleName().equals( name ) ) {
			return CURRENCY;
		}

		if ( Locale.class.getName().equals( name )
			|| Locale.class.getSimpleName().equals( name ) ) {
			return LOCALE;
		}

		if ( Class.class.getName().equals( name )
			|| Class.class.getSimpleName().equals( name ) ) {
			return CLASS;
		}

		return null;
	}
}
